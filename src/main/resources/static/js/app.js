/* 공용 헬퍼는 common.js에 있다. 이 파일은 실무자 화면 전용이다. */
const round1 = v => v === null || v === undefined ? null : Math.round(Number(v) * 10) / 10;
const kdate = v => v ? String(v).replaceAll('-', '.') : '-';

const form = $('diagnosis-form');
const recurrenceType = $('recurrence-type');
const areaCode = $('area-code');
const signguCode = $('signgu-code');
const notice = $('notice');
const state = {
    reportId: null, dashboard: null, report: null,
    map: null, overlays: [], infoWindow: null,
    pickerMap: null, pickerOverlays: [], pickerInfoWindow: null
};

const BADGE = {
    SURGING: { cls: 'b-crit', label: '급증', color: '#B23A32' },
    WARNING: { cls: 'b-warn', label: '주의', color: '#B9822A' },
    STABLE: { cls: 'b-ok', label: '안정', color: '#2F7D5B' },
    RELAXED: { cls: 'b-cool', label: '여유', color: '#3B6EA5' }
};
const badgeOf = level => BADGE[level] || { cls: 'b-mute', label: text(level), color: '#6A7078' };
const SEVERITY = { CRITICAL: { cls: 'b-crit', label: '긴급' }, WARNING: { cls: 'b-warn', label: '주의' }, INFO: { cls: 'b-cool', label: '참고' } };
const sevOf = s => SEVERITY[s] || { cls: 'b-mute', label: text(s) };
const STATUS_LABEL = { AVAILABLE: '정상', NO_DATA: '데이터 없음', OUT_OF_FORECAST_RANGE: '예측 범위 밖', FAILED: '호출 실패' };
const SOURCE_LABEL = {
    KOR_SERVICE: '재개최 축제 · API #8 자동 로드',
    USER_INPUT: '신규 축제 · 직접 입력 좌표',
    SIGNGU_CENTER: '미입력 · 시군구 중심 근사',
    UNAVAILABLE: '좌표 미확보'
};

/* ================= 초기화 ================= */
const today = new Date();
const startDefault = new Date(today); startDefault.setDate(today.getDate() + 7);
const endDefault = new Date(today); endDefault.setDate(today.getDate() + 10);
form.startDate.value = dateText(startDefault);
form.endDate.value = dateText(endDefault);

loadRegions().catch(error => fail(`지역 목록을 불러오지 못했습니다: ${error.message}`));
areaCode.addEventListener('change', () => loadDistricts(areaCode.value));

recurrenceType.addEventListener('change', () => {
    const isNew = recurrenceType.value === '신규';
    ['address-field', 'lat-field', 'lng-field'].forEach(id => $(id).classList.toggle('hide', !isNew));
    $('existing-content-field').classList.toggle('hide', isNew);
});

function fail(message) {
    notice.className = 'notice err';
    notice.textContent = message;
}

/* ================= 진단 실행 ================= */
form.addEventListener('submit', async event => {
    event.preventDefault();
    await runDiagnosis();
});
$('rerun-button').addEventListener('click', () => runDiagnosis());

async function runDiagnosis() {
    const values = new FormData(form);
    const isNew = values.get('recurrenceType') === '신규';
    const optional = name => {
        const value = values.get(name);
        return value === null || value === '' ? null : Number(value);
    };
    const payload = {
        festivalName: values.get('festivalName'),
        areaCode: values.get('areaCode'),
        signguCode: values.get('signguCode'),
        startDate: values.get('startDate'),
        endDate: values.get('endDate'),
        festivalType: values.get('festivalType'),
        scale: values.get('scale'),
        recurrenceType: values.get('recurrenceType'),
        existingFestivalContentId: isNew ? null : values.get('existingFestivalContentId'),
        festivalAddress: isNew ? values.get('festivalAddress') : null,
        latitude: isNew ? optional('latitude') : null,
        longitude: isNew ? optional('longitude') : null
    };

    $('submit-button').disabled = true;
    $('rerun-button').disabled = true;
    notice.className = 'notice busy';
    notice.textContent = '진단을 실행하는 중…';

    try {
        const created = await request('/api/v1/reports', { method: 'POST', body: JSON.stringify(payload) });
        state.reportId = created.reportId;
        state.dashboard = await request(`/api/v1/reports/${created.reportId}/dashboard`);
        state.report = await request(`/api/v1/reports/${created.reportId}/forecast-report`);

        notice.className = 'notice';
        notice.textContent = `진단 완료 · reportId ${created.reportId}`;
        $('results').classList.remove('hide');
        $('pdf-open').disabled = false;
        $('setup').open = false;
        renderAll();
        $('raw-json').textContent = JSON.stringify({ dashboard: state.dashboard, forecastReport: state.report }, null, 2);
        renderMap(state.dashboard).catch(error => { $('map-notice').textContent = error.message; });
    } catch (error) {
        fail(error.message);
    } finally {
        $('submit-button').disabled = false;
        $('rerun-button').disabled = false;
    }
}

/* ================= 파생값 ================= */
// 종합 판정은 새 지표가 아니라 리스크 심각도 집계다(종합 점수·등급을 만들지 않는다).
function verdictOf(risks) {
    const critical = risks.filter(r => r.severity === 'CRITICAL').length;
    const warning = risks.filter(r => r.severity === 'WARNING').length;
    if (critical) return { tone: 'crit', lights: 3, label: '주의 · 조정 필요', stamp: '주의', stampEn: 'CAUTION' };
    if (warning) return { tone: 'warn', lights: 2, label: '관찰 필요', stamp: '관찰', stampEn: 'WATCH' };
    return { tone: 'ok', lights: 1, label: '특이사항 없음', stamp: '양호', stampEn: 'CLEAR' };
}

function verdictSentence(dashboard) {
    const risks = dashboard.risks || [];
    if (!risks.length) return '매칭된 리스크가 없습니다. 데이터 출처의 수집 상태를 함께 확인하세요.';
    const top = [...risks].sort((a, b) => (a.priority ?? 99) - (b.priority ?? 99))[0];
    return top.description || top.title || '';
}

// 개최기간 중 가장 높은 집중률과 그 관광지·날짜
function peakOf(concentration) {
    let best = null;
    (concentration?.dailyConcentrations || []).forEach(day => (day.places || []).forEach(place => {
        if (place.concentrationRate === null || place.concentrationRate === undefined) return;
        if (!best || place.concentrationRate > best.rate) best = { rate: place.concentrationRate, date: day.date, placeName: place.placeName };
    }));
    return best;
}

function dailyMaxSeries(concentration) {
    return (concentration?.dailyConcentrations || []).map(day => {
        const rates = (day.places || []).map(p => p.concentrationRate).filter(v => v !== null && v !== undefined);
        return rates.length ? Math.max(...rates) : null;
    });
}

function countBy(items, key) {
    return items.reduce((acc, item) => { acc[item[key]] = (acc[item[key]] || 0) + 1; return acc; }, {});
}

/* ================= 렌더 ================= */
function renderAll() {
    const dashboard = state.dashboard;
    const report = state.report;
    renderHero(dashboard, report);
    renderKpis(dashboard);
    renderTodo(dashboard);
    renderRisks(dashboard.risks || []);
    renderProfile(dashboard.profile);
    renderDistribution(dashboard.distribution);
    renderVolatility(dashboard.volatility);
    renderFlow(dashboard.concentration);
    renderCompeting(dashboard.competing);
    renderVisitors(dashboard.regionalVisitors);
    renderSources(dashboard.dataStatuses || []);
    renderDoc(dashboard, report);
}

function renderHero(dashboard, report) {
    const diagnosis = dashboard.diagnosis || {};
    const hero = report?.hero || {};
    const name = hero.festivalName || diagnosis.festivalName || '축제';
    const period = `${kdate(hero.startDate || diagnosis.startDate)} – ${kdate(hero.endDate || diagnosis.endDate)}`;
    const location = dashboard.festivalLocation?.address || diagnosis.festivalAddress || '';

    $('hero-eyebrow').textContent = `진단 완료 · ${new Date().toLocaleString('ko-KR', { dateStyle: 'medium', timeStyle: 'short' })}`;
    $('hero-name').textContent = name;
    $('hero-sub').textContent = [period, location].filter(Boolean).join(' · ');
    $('top-now').replaceChildren(el('b', null, name), document.createTextNode(` · ${period}`));
    $('cond-line').textContent = `${name} · ${period} · ${text(diagnosis.festivalType)} · ${text(diagnosis.scale)} · ${text(diagnosis.recurrenceType)}`;

    const chips = clear('hero-chips');
    if (hero.daysUntilStart !== null && hero.daysUntilStart !== undefined) {
        chips.append(el('span', 'chip on', hero.daysUntilStart >= 0 ? `D-${hero.daysUntilStart}` : `개최 후 ${-hero.daysUntilStart}일`));
    }
    [diagnosis.festivalType, diagnosis.scale, diagnosis.recurrenceType].filter(Boolean)
        .forEach(v => chips.append(el('span', 'chip on', v)));

    const statuses = dashboard.dataStatuses || [];
    const ok = statuses.filter(s => s.status === 'AVAILABLE').length;
    chips.append(el('span', `chip ${ok === statuses.length ? '' : 'mid'}`.trim(), `데이터 ${ok}/${statuses.length} 정상`));

    const location$ = dashboard.festivalLocation;
    if (location$) {
        chips.append(el('span', `chip ${location$.precise ? '' : 'mid'}`.trim(), SOURCE_LABEL[location$.source] || location$.source));
    }

    const verdict = verdictOf(dashboard.risks || []);
    const lights = clear('verdict-lights');
    ['ok', 'warn', 'crit'].forEach((tone, index) => {
        lights.append(el('i', index < verdict.lights ? `on-${tone}` : ''));
    });
    const level = $('verdict-level');
    level.className = `lv t-${verdict.tone}`;
    level.textContent = verdict.label;
    $('verdict-desc').textContent = verdictSentence(dashboard);
}

function renderKpis(dashboard) {
    const root = clear('kpis');
    const peak = peakOf(dashboard.concentration);
    const volatilityPlaces = dashboard.volatility?.places || [];
    const mapPlaces = dashboard.map?.places || [];
    const near = mapPlaces.filter(p => p.withinNearestRadius);
    const nearHot = near.filter(p => p.badge === 'SURGING' || p.badge === 'WARNING');
    const festivals = dashboard.competing?.festivals || [];
    const nearest = festivals.map(f => Number(f.distanceKm)).filter(v => !Number.isNaN(v)).sort((a, b) => a - b)[0];
    const risks = dashboard.risks || [];
    const severityCount = countBy(risks, 'severity');
    const badgeCount = countBy(volatilityPlaces, 'level');
    const topIncrease = volatilityPlaces.map(p => p.increasePoint).filter(v => v !== null && v !== undefined).sort((a, b) => b - a)[0];

    root.append(kpiCard({
        label: '예상 최대 집중률',
        value: peak ? round1(peak.rate) : null,
        unit: '%',
        detail: peak ? `${peak.placeName} · ${kdate(peak.date)}` : '집중률 예측 데이터 없음',
        chart: sparkLine(dailyMaxSeries(dashboard.concentration))
    }));
    root.append(kpiCard({
        label: `반경 ${round1(dashboard.map?.nearestRadiusKm) ?? 1}km 급증 관광지`,
        value: dashboard.map?.site ? nearHot.length : null,
        unit: '곳',
        detail: dashboard.map?.site
            ? `반경 내 ${near.length}곳 · 전체 ${mapPlaces.length}곳`
            : '좌표 미확보 · 반경 판정 스킵',
        chart: stackBar([
            ['SURGING', badgeCount.SURGING || 0, '#B23A32'],
            ['WARNING', badgeCount.WARNING || 0, '#B9822A'],
            ['STABLE', badgeCount.STABLE || 0, '#2F7D5B'],
            ['RELAXED', badgeCount.RELAXED || 0, '#3B6EA5']
        ])
    }));
    root.append(kpiCard({
        label: '동기간 인근 축제',
        value: dashboard.competing?.totalCount ?? null,
        unit: '건',
        detail: nearest !== undefined ? `최근접 ${round1(nearest)}km` : '겹치는 축제 없음',
        chart: distanceDots(festivals)
    }));
    root.append(kpiCard({
        label: '처리할 리스크',
        value: risks.length,
        unit: '건',
        detail: `긴급 ${severityCount.CRITICAL || 0} · 주의 ${severityCount.WARNING || 0} · 참고 ${severityCount.INFO || 0}`,
        chart: stackBar([
            ['CRITICAL', severityCount.CRITICAL || 0, '#B23A32'],
            ['WARNING', severityCount.WARNING || 0, '#B9822A'],
            ['INFO', severityCount.INFO || 0, '#3B6EA5']
        ])
    }));

    if (topIncrease !== undefined) {
        root.firstChild.querySelector('.d').append(el('span', null, ` · 자기평균 +${round1(topIncrease)}%p`));
    }
}

function kpiCard({ label, value, unit, detail, chart }) {
    const card = el('div', 'kpi');
    card.append(el('div', 'k', label));
    const v = el('div', 'v');
    if (value === null || value === undefined) {
        v.append(el('span', null, '—'));
    } else {
        v.append(document.createTextNode(number(value)));
        if (unit) v.append(el('small', null, unit));
    }
    card.append(v);
    card.append(el('div', 'd', detail));
    if (chart) card.append(chart);
    return card;
}

function sparkLine(series) {
    const values = series.filter(v => v !== null);
    if (values.length < 2) return null;
    const max = Math.max(...values, 1);
    const stepX = 200 / (series.length - 1);
    const y = v => 32 - (v / max) * 28;
    const points = series.map((v, i) => v === null ? null : `${(stepX * i).toFixed(1)},${y(v).toFixed(1)}`).filter(Boolean);
    const svg = svgNode('svg', { viewBox: '0 0 200 34', role: 'img', 'aria-label': '일별 최고 집중률 추이', preserveAspectRatio: 'none' });
    svg.append(svgNode('polyline', { points: points.join(' '), fill: 'none', stroke: '#B23A32', 'stroke-width': 1.8, 'vector-effect': 'non-scaling-stroke' }));
    const peakIndex = series.indexOf(Math.max(...values));
    svg.append(svgNode('circle', { cx: (stepX * peakIndex).toFixed(1), cy: y(series[peakIndex]).toFixed(1), r: 3, fill: '#B23A32' }));
    return svg;
}

function stackBar(parts) {
    const total = parts.reduce((sum, [, count]) => sum + count, 0);
    if (!total) return null;
    const svg = svgNode('svg', { viewBox: '0 0 200 34', role: 'img', 'aria-label': '구성 비율', preserveAspectRatio: 'none' });
    let x = 0;
    parts.filter(([, count]) => count > 0).forEach(([name, count, color]) => {
        const width = (count / total) * 200;
        const rect = svgNode('rect', { x: x.toFixed(1), y: 12, width: Math.max(width - 2, 1).toFixed(1), height: 12, fill: color });
        rect.append(svgNode('title', {}, `${name} ${count}`));
        svg.append(rect);
        x += width;
    });
    return svg;
}

function distanceDots(festivals) {
    const distances = festivals.map(f => Number(f.distanceKm)).filter(v => !Number.isNaN(v));
    if (!distances.length) return null;
    const max = Math.max(30, ...distances);
    const svg = svgNode('svg', { viewBox: '0 0 200 34', role: 'img', 'aria-label': '인근 축제 거리 분포' });
    svg.append(svgNode('line', { x1: 4, y1: 18, x2: 196, y2: 18, stroke: '#E1E1DC', 'stroke-width': 1.5 }));
    svg.append(svgNode('circle', { cx: 4, cy: 18, r: 5, fill: '#17604A' }));
    distances.forEach((distance, index) => {
        const dot = svgNode('circle', { cx: (4 + (distance / max) * 192).toFixed(1), cy: 18, r: 4, fill: index === 0 ? '#B9822A' : '#6A7078' });
        dot.append(svgNode('title', {}, `${round1(distance)}km`));
        svg.append(dot);
    });
    svg.append(svgNode('text', { x: 196, y: 32, 'font-size': 8, fill: '#6A7078', 'text-anchor': 'end', 'font-family': 'monospace' }, `${round1(max)}km`));
    return svg;
}

function renderTodo(dashboard) {
    const items = dashboard.recommendations || [];
    $('todo-count').textContent = `운영 조정 제안 ${items.length}건`;
    const list = clear('todo-list');
    if (!items.length) return list.append(emptyRow('연계된 운영 제안이 없습니다.'));
    items.forEach(item => {
        const button = el('button', 'todo');
        button.type = 'button';
        button.setAttribute('aria-pressed', 'false');
        button.append(el('span', 'cb', '✓'));
        const tx = el('span', 'tx');
        tx.append(el('b', null, item.title || item.recommendationCode));
        tx.append(el('span', null, [
            item.recommendationCode,
            (item.relatedRiskCodes || []).length ? `${item.relatedRiskCodes.join(', ')} 대응` : null,
            item.category,
            item.difficulty ? `난이도 ${item.difficulty}` : null
        ].filter(Boolean).join(' · ')));
        if (item.defaultAction) tx.append(el('span', null, item.defaultAction));
        button.append(tx);
        button.append(el('span', 'lv', item.priority ? `우선순위 ${item.priority}` : ''));
        // 체크는 화면에서만 유지한다. 저장이 필요하면 별도 API가 있어야 한다.
        button.addEventListener('click', () => {
            button.setAttribute('aria-pressed', button.getAttribute('aria-pressed') === 'true' ? 'false' : 'true');
        });
        list.append(button);
    });
}

function renderRisks(risks) {
    $('risk-count').textContent = `${risks.length}건`;
    const list = clear('risk-list');
    if (!risks.length) return list.append(emptyRow('매칭된 리스크가 없습니다.'));
    risks.forEach(risk => {
        const box = el('div', `risk ${risk.severity}`);
        const head = el('div', 'hd');
        const severity = sevOf(risk.severity);
        head.append(el('span', `badge ${severity.cls}`, severity.label));
        head.append(el('b', null, risk.title || risk.riskCode));
        box.append(head);
        box.append(el('p', null, risk.description || ''));
        const evidence = el('div', 'ev');
        evidence.append(el('b', null, risk.riskCode));
        evidence.append(document.createTextNode(` · ${risk.metricKey} = ${text(risk.metricValue)}`));
        const pairs = Object.entries(risk.evidence || {}).map(([k, v]) => `${k}=${v}`).join(' ');
        if (pairs) evidence.append(document.createTextNode(` · ${pairs}`));
        if ((risk.recommendationCodes || []).length) evidence.append(document.createTextNode(` → ${risk.recommendationCodes.join(', ')}`));
        box.append(evidence);
        list.append(box);
    });
}

// 축마다 자연 척도가 달라 막대 길이는 각 축의 표시 범위 안에서만 계산한다.
function ratio(metric) {
    if (!metric || metric.value === null || metric.value === undefined) return 0;
    const minimum = Number(metric.minimum ?? 0);
    const maximum = Number(metric.maximum ?? Math.max(Number(metric.value), 1));
    const span = maximum - minimum || 1;
    const filled = (Number(metric.value) - minimum) / span;
    const oriented = metric.direction === 'LOWER' ? 1 - filled : filled;
    return Math.max(0, Math.min(1, oriented)) * 100;
}

const PROFILE_AXES = [
    ['시기적합도', 'timingFit'],
    ['여유 관광지', 'relaxedPlaces'],
    ['연계 풍부도', 'connectivity'],
    ['카테고리 다양성', 'categoryDiversity']
];

function metricText(metric) {
    if (!metric || metric.value === null || metric.value === undefined) return '데이터 없음';
    return `${metric.value}${metric.unit || ''}${metric.maximum ? ` / ${metric.maximum}` : ''}`;
}

function renderProfile(profile) {
    const box = clear('profile-box');
    if (!profile) return box.append(el('p', 'empty', '프로필 데이터가 없습니다.'));
    PROFILE_AXES.forEach(([name, key]) => {
        const metric = profile[key];
        const percent = ratio(metric);
        const row = el('div', 'meter');
        row.append(el('span', null, name));
        const track = el('span', 'tr');
        const fill = el('i', percent < 40 ? 'low' : '');
        fill.style.width = `${percent}%`;
        track.append(fill);
        row.append(track);
        row.append(el('span', 'vl', metricText(metric)));
        box.append(row);
    });
    const notes = el('ul', 'foot');
    (profile.notes || []).forEach(note => notes.append(el('li', null, note)));
    box.append(notes);
}

function renderVolatility(volatility) {
    const places = volatility?.places || [];
    $('vol-count').textContent = `전체 ${text(volatility?.totalCount)}곳`;
    const list = clear('vol-list');
    if (!places.length) return list.append(emptyRow('관광지 변동 데이터가 없습니다.'));
    places.forEach(place => {
        const badge = badgeOf(place.level);
        const row = el('div', 'row');
        row.append(el('span', `badge ${badge.cls}`, badge.label));
        const body = el('span');
        body.append(el('span', 'nm', place.placeName));
        body.append(el('br'));
        body.append(el('span', 'mt', `자기평균 ${text(round1(place.selfAverage))}% · 최고 ${text(round1(place.peakRate))}% · ${kdate(place.peakDate)}`));
        row.append(body);
        const value = el('span', 'num', place.increasePoint === null || place.increasePoint === undefined
            ? '-' : `${place.increasePoint > 0 ? '+' : ''}${round1(place.increasePoint)}%p`);
        value.style.color = badge.color;
        row.append(value);
        list.append(row);
    });
}

function renderDistribution(distribution) {
    const places = distribution?.places || [];
    $('dist-count').textContent = `전체 ${text(distribution?.totalCount)}곳`;
    const list = clear('dist-list');
    if (!places.length) return list.append(emptyRow('여유 관광지 데이터가 없습니다.'));
    places.forEach(place => {
        const row = el('div', 'row');
        row.append(el('span', 'badge b-cool', `${text(place.rank)}순위`));
        const body = el('span');
        body.append(el('span', 'nm', place.placeName));
        body.append(el('br'));
        body.append(el('span', 'mt', [
            place.category,
            place.relatedRank ? `연관 ${place.relatedRank}위` : null,
            place.recommendationReason
        ].filter(Boolean).join(' · ')));
        row.append(body);
        row.append(el('span', 'num', place.value === null || place.value === undefined ? '-' : `${round1(place.value)}%`));
        list.append(row);
    });
}

function renderCompeting(competing) {
    const festivals = competing?.festivals || [];
    $('comp-count').textContent = `전체 ${text(competing?.totalCount)}건`;
    const list = clear('comp-list');
    if (!festivals.length) return list.append(emptyRow('동기간 인근 축제가 없습니다.'));
    festivals.forEach(festival => {
        const row = el('div', 'row');
        row.append(el('span', 'badge b-warn', `${text(round1(festival.distanceKm))}km`));
        const body = el('span');
        body.append(el('span', 'nm', festival.festivalName));
        body.append(el('br'));
        body.append(el('span', 'mt', [
            festival.regionName,
            festival.lastYearVisitors ? `작년 ${number(festival.lastYearVisitors)}명` : null,
            festival.budgetMillionWon ? `예산 ${number(festival.budgetMillionWon)}백만원` : null,
            (festival.linkageTags || []).join(' · ') || null
        ].filter(Boolean).join(' · ')));
        row.append(body);
        const period = el('span', 'num');
        period.append(document.createTextNode(kdate(festival.startDate)), el('br'), document.createTextNode(kdate(festival.endDate)));
        row.append(period);
        list.append(row);
    });
}

function renderFlow(concentration) {
    const box = clear('flow-box');
    const days = concentration?.dailyConcentrations || [];
    if (!days.length) return box.append(el('p', 'empty', '집중률 예측 데이터가 없습니다.'));

    const series = new Map();
    days.forEach((day, index) => (day.places || []).forEach(place => {
        if (!series.has(place.placeName)) series.set(place.placeName, new Array(days.length).fill(null));
        series.get(place.placeName)[index] = place.concentrationRate;
    }));

    const width = 720, height = 230, padX = 34, padY = 14;
    const stepX = days.length > 1 ? (width - padX * 2) / (days.length - 1) : 0;
    const svg = svgNode('svg', { viewBox: `0 0 ${width} ${height}`, role: 'img', 'aria-label': '개최기간 일별 집중률 예측' });
    svg.style.width = '100%';
    svg.style.height = 'auto';

    [0, 50, 100].forEach(value => {
        const y = padY + (height - padY * 2 - 16) * (1 - value / 100);
        svg.append(svgNode('line', { x1: padX, y1: y, x2: width - padX, y2: y, stroke: '#E1E1DC' }));
        svg.append(svgNode('text', { x: 2, y: y + 4, 'font-size': 10, fill: '#6A7078', 'font-family': 'monospace' }, String(value)));
    });

    const palette = ['#B23A32', '#B9822A', '#2F7D5B', '#3B6EA5', '#17604A', '#7B4C8A', '#1F2328', '#A9601F', '#4B6572', '#8A6A16'];
    const legend = el('div', 'chips');
    legend.style.marginTop = '10px';
    [...series.entries()].forEach(([name, values], index) => {
        const color = palette[index % palette.length];
        const points = values
            .map((value, dayIndex) => value === null ? null
                : `${padX + stepX * dayIndex},${padY + (height - padY * 2 - 16) * (1 - value / 100)}`)
            .filter(Boolean).join(' ');
        svg.append(svgNode('polyline', { points, fill: 'none', stroke: color, 'stroke-width': 2 }));
        const chip = el('span', 'chip', name);
        chip.style.borderColor = color;
        chip.style.color = color;
        legend.append(chip);
    });

    days.forEach((day, index) => {
        if (days.length > 8 && index % 2 === 1) return;
        svg.append(svgNode('text', {
            x: padX + stepX * index, y: height - 2, 'font-size': 10, fill: '#6A7078',
            'text-anchor': 'middle', 'font-family': 'monospace'
        }, String(day.date).slice(5)));
    });

    box.append(svg, legend);
}

function renderVisitors(visitors) {
    const box = clear('visitor-box');
    const days = visitors?.dailyVisitors || [];
    $('visitor-ref').textContent = visitors?.referenceFestivalStartDate
        ? `참고 ${kdate(visitors.referenceFestivalStartDate)} – ${kdate(visitors.referenceFestivalEndDate)}` : '';
    if (!days.length) return box.append(el('p', 'empty', '방문자 데이터가 없습니다.'));

    ['개최기간 평균', '직전 평균', '직후 평균'].forEach((label, index) => {
        const value = [visitors.festivalPeriodAverage, visitors.beforePeriodAverage, visitors.afterPeriodAverage][index];
        const row = el('div', 'meter');
        row.append(el('span', null, label));
        const track = el('span', 'tr');
        const max = Math.max(Number(visitors.festivalPeriodAverage || 0), Number(visitors.beforePeriodAverage || 0), Number(visitors.afterPeriodAverage || 0), 1);
        const fill = el('i');
        fill.style.width = `${(Number(value || 0) / max) * 100}%`;
        track.append(fill);
        row.append(track);
        row.append(el('span', 'vl', `${number(value)}명`));
        box.append(row);
    });
    if (visitors.changeFromBeforePercent !== null && visitors.changeFromBeforePercent !== undefined) {
        box.append(el('p', 'foot', `직전 대비 ${round1(visitors.changeFromBeforePercent)}%`));
    }

    const wrap = el('div', 'tb-wrap');
    wrap.style.marginTop = '10px';
    wrap.append(table(['날짜', '현지인', '외지인', '외국인', '전체'],
        days.slice(0, 7).map(day => [kdate(day.date), number(day.localVisitors), number(day.outsideVisitors), number(day.foreignVisitors), number(day.totalVisitors)]),
        [false, true, true, true, true]));
    box.append(wrap);
}

function renderSources(statuses) {
    $('source-count').textContent = `${statuses.filter(s => s.status === 'AVAILABLE').length} / ${statuses.length} 정상`;
    const box = clear('source-box');
    if (!statuses.length) return box.append(el('p', 'empty', '수집 상태 정보가 없습니다.'));
    box.append(table(['데이터', '상태', '기준 시점', '사유'],
        statuses.map(item => {
            const badge = el('span', `badge ${item.status === 'AVAILABLE' ? 'b-ok' : item.status === 'FAILED' ? 'b-crit' : 'b-warn'}`,
                STATUS_LABEL[item.status] || item.status);
            return [item.source, badge, text(item.referencePeriod), text(item.reason)];
        })));
}

function table(headers, rows, numeric = []) {
    const node = el('table', 'tb');
    const head = el('tr');
    headers.forEach((header, index) => {
        const th = el('th', numeric[index] ? 'num' : '', header);
        if (numeric[index]) th.style.textAlign = 'right';
        head.append(th);
    });
    node.append(head);
    rows.forEach(row => {
        const line = el('tr');
        row.forEach((cell, index) => {
            const td = el('td', numeric[index] ? 'num' : '');
            if (cell instanceof Node) td.append(cell); else td.textContent = text(cell);
            line.append(td);
        });
        node.append(line);
    });
    return node;
}

function emptyRow(message) {
    const row = el('div', 'row');
    row.append(el('span', 'mt', message));
    return row;
}

/* ================= 지도 ================= */
async function renderMap(dashboard) {
    const info = $('map-notice');
    const legend = clear('map-legend');
    const location = dashboard.festivalLocation;
    const mapData = dashboard.map;

    info.textContent = location
        ? `${SOURCE_LABEL[location.source] || location.source}${location.notice ? ` · ${location.notice}` : ''}`
        : '위치 정보 없음';

    if (!mapData || !mapData.site) {
        legend.append(el('span', 'chip bad', '좌표 미확보 · 반경 판정 스킵'));
        return;
    }

    const withinCount = (mapData.places || []).filter(place => place.withinNearestRadius).length;
    legend.append(el('span', 'chip', `관광지 ${(mapData.places || []).length}곳`));
    legend.append(el('span', 'chip', `인근 축제 ${(mapData.nearbyFestivals || []).length}건`));
    legend.append(el('span', location.precise ? 'chip on' : 'chip mid',
        location.precise ? `반경 ${round1(mapData.nearestRadiusKm) ?? 1}km 이내 ${withinCount}곳` : '근사 좌표 · 반경 판정 스킵'));
    Object.entries(BADGE).forEach(([, badge]) => {
        const chip = el('span', 'chip', badge.label);
        chip.style.borderColor = badge.color;
        chip.style.color = badge.color;
        legend.append(chip);
    });

    const maps = await loadGoogleMaps();
    const center = { lat: Number(mapData.site.latitude), lng: Number(mapData.site.longitude) };
    if (!state.map) {
        state.map = new maps.Map($('map'), { center, zoom: 11, mapTypeControl: false, streetViewControl: false });
        state.infoWindow = new maps.InfoWindow();
    }
    state.map.setCenter(center);
    clearOverlays(state.overlays);

    state.overlays.push(new maps.Marker({
        map: state.map, position: center, title: '축제장', zIndex: 999, icon: circleSymbol(maps, '#17604A', 9)
    }));
    state.overlays.push(new maps.Circle({
        map: state.map, center, radius: (mapData.nearestRadiusKm || 1) * 1000,
        strokeColor: '#17604A', strokeWeight: 1, fillColor: '#17604A', fillOpacity: .06
    }));

    (mapData.places || []).forEach(place => {
        const marker = new maps.Marker({
            map: state.map,
            position: { lat: Number(place.latitude), lng: Number(place.longitude) },
            title: place.placeName,
            icon: circleSymbol(maps, badgeOf(place.badge).color, place.withinNearestRadius ? 8 : 5)
        });
        marker.addListener('click', () => {
            state.infoWindow.setContent(
                `<div class="info-window"><strong>${place.placeName}</strong>`
                + `${badgeOf(place.badge).label} · 상승폭 ${text(round1(place.peakIncreasePoint))}%p<br>`
                + `${text(round1(place.distanceKm))}km · hubRank ${text(place.hubRank)} · ${text(place.category)}</div>`
            );
            state.infoWindow.open({ map: state.map, anchor: marker });
        });
        state.overlays.push(marker);
    });

    (mapData.nearbyFestivals || []).forEach(festival => {
        if (festival.latitude === null || festival.longitude === null) return;
        const marker = new maps.Marker({
            map: state.map,
            position: { lat: Number(festival.latitude), lng: Number(festival.longitude) },
            title: festival.festivalName
        });
        marker.addListener('click', () => {
            state.infoWindow.setContent(
                `<div class="info-window"><strong>${festival.festivalName}</strong>`
                + `${kdate(festival.startDate)} ~ ${kdate(festival.endDate)}<br>${text(round1(festival.distanceKm))}km</div>`
            );
            state.infoWindow.open({ map: state.map, anchor: marker });
        });
        state.overlays.push(marker);
    });
}

/* ================= 개최지 선택 (API #8) ================= */
const picker = $('picker');

$('open-picker').addEventListener('click', () => {
    $('picker-from').value = form.startDate.value;
    const to = new Date(form.endDate.value);
    to.setDate(to.getDate() + 60);
    $('picker-to').value = dateText(to);
    picker.showModal();
    openPicker().catch(pickerFail);
});
$('close-picker').addEventListener('click', () => picker.close());
$('picker-reload').addEventListener('click', () => openPicker().catch(pickerFail));

function pickerFail(error) {
    $('picker-notice').className = 'notice err';
    $('picker-notice').textContent = error.message;
}

async function openPicker() {
    const pickerNotice = $('picker-notice');
    pickerNotice.className = 'notice busy';
    pickerNotice.textContent = '축제 개최지를 불러오는 중…';

    const maps = await loadGoogleMaps();
    if (!state.pickerMap) {
        state.pickerMap = new maps.Map($('picker-map'), {
            center: { lat: 36.5, lng: 127.8 }, zoom: 7, mapTypeControl: false, streetViewControl: false
        });
        state.pickerInfoWindow = new maps.InfoWindow();
    }

    // 데이터랩 시군구 코드 51130 = 법정동 시도 51 + 시군구 130
    const parameters = new URLSearchParams({
        startDate: tourDate($('picker-from').value),
        endDate: tourDate($('picker-to').value),
        legalDongRegionCode: areaCode.value,
        size: '100'
    });
    if ($('picker-signgu-only').checked) parameters.set('legalDongSignguCode', signguCode.value.slice(2));

    const response = await request(`/api/v1/festivals?${parameters}`);
    const festivals = (response.festivals || []).filter(f => f.latitude && f.longitude);

    clearOverlays(state.pickerOverlays);
    const bounds = new maps.LatLngBounds();
    festivals.forEach(festival => {
        const position = { lat: Number(festival.latitude), lng: Number(festival.longitude) };
        const marker = new maps.Marker({ map: state.pickerMap, position, title: festival.title });
        marker.addListener('click', () => showFestivalInfo(marker, festival));
        state.pickerOverlays.push(marker);
        bounds.extend(position);
    });
    if (festivals.length) state.pickerMap.fitBounds(bounds);

    pickerNotice.className = 'notice';
    pickerNotice.textContent = `총 ${response.totalCount}건 중 좌표가 있는 ${festivals.length}건을 표시했습니다. 핀을 눌러 선택하세요.`;
}

function showFestivalInfo(marker, festival) {
    const container = el('div', 'info-window');
    container.append(el('strong', null, festival.title));
    container.append(el('div', null, `${text(festival.eventStartDate)} ~ ${text(festival.eventEndDate)}`));
    container.append(el('div', null, text(festival.address)));
    container.append(el('div', null, `contentId ${festival.contentId}`));
    const select = el('button', null, '이 축제 선택');
    select.type = 'button';
    select.addEventListener('click', () => selectFestival(festival));
    container.append(select);
    state.pickerInfoWindow.setContent(container);
    state.pickerInfoWindow.open({ map: state.pickerMap, anchor: marker });
}

// 기획서 5.5.2 1단계 · 재개최 축제를 고르면 contentId와 지역이 자동으로 채워진다.
async function selectFestival(festival) {
    form.festivalName.value = festival.title;
    recurrenceType.value = '재개최';
    recurrenceType.dispatchEvent(new Event('change'));
    form.existingFestivalContentId.value = festival.contentId;

    if (festival.legalDongRegionCode) {
        areaCode.value = festival.legalDongRegionCode;
        await loadDistricts(areaCode.value, festival.legalDongRegionCode + (festival.legalDongSignguCode || ''));
    }
    if (festival.eventStartDate && festival.eventEndDate) {
        form.startDate.value = isoDate(festival.eventStartDate);
        form.endDate.value = isoDate(festival.eventEndDate);
    }

    const picked = $('picked-festival');
    picked.className = 'picked on';
    picked.textContent = `선택됨: ${festival.title} (contentId ${festival.contentId}) · 좌표는 진단 시 자동 로드`;
    state.pickerInfoWindow?.close();
    if (picker.open) picker.close();
}

/* ================= AI 리포트 ================= */
$('ai-button').addEventListener('click', async () => {
    if (!state.reportId) return fail('먼저 진단을 실행하세요.');
    const baseUrl = $('ai-base-url').value.replace(/\/+$/, '');
    $('ai-button').disabled = true;
    try {
        renderAi(await request(`${baseUrl}/api/v1/reports/${state.reportId}/ai-report`, { method: 'POST' }));
    } catch (error) {
        clear('ai-result').append(el('p', 'notice err', `ai-service 호출 실패: ${error.message}`));
    } finally {
        $('ai-button').disabled = false;
    }
});

function renderAi(ai) {
    const root = clear('ai-result');
    state.ai = ai;
    (ai.warnings || []).forEach(warning => root.append(el('p', 'notice err', `⚠ ${warning}`)));

    if (ai.briefing?.text) {
        const box = el('div', 'risk INFO');
        box.style.padding = '12px 14px';
        const head = el('div', 'hd');
        head.append(el('b', null, ai.briefing.source === 'AI' ? 'AI 브리핑' : '브리핑 (폴백)'));
        box.append(head);
        box.append(el('p', null, ai.briefing.text));
        if (ai.briefing.disclaimer) box.append(el('div', 'ev', ai.briefing.disclaimer));
        root.append(box);
        // 브리핑은 PDF §1에도 반영한다.
        if (state.dashboard) renderDoc(state.dashboard, state.report);
    }

    if ((ai.recommendations || []).length) {
        const wrap = el('div', 'tb-wrap');
        wrap.style.marginTop = '12px';
        wrap.append(table(['제안', '핵심 조치', '주차 분산', '인력 배치', '선행 조치'],
            ai.recommendations.map(item => [item.recommendationCode, item.coreAction, item.parkingDistribution, item.staffing, item.precedingAction])));
        root.append(wrap);
    }
    if ((ai.placeEstimates || []).length) {
        const wrap = el('div', 'tb-wrap');
        wrap.style.marginTop = '12px';
        wrap.append(table(['관광지', '추정', 'confidence', '근거'],
            ai.placeEstimates.map(item => [item.placeName, item.displayText, item.confidence, item.reasoning])));
        root.append(wrap);
    }
    if (ai.operatorPriorityNote) root.append(el('p', 'foot', ai.operatorPriorityNote));
}

/* =========================================================
   PDF 내보내기 · 문서형(방향 B)
   ========================================================= */
const pdf = $('pdf');
const doc = $('doc');

$('pdf-open').addEventListener('click', () => {
    if (!state.dashboard) return;
    renderDoc(state.dashboard, state.report);
    pdf.hidden = false;
    document.body.classList.add('pdf-open');
});
$('pdf-close').addEventListener('click', closePdf);
$('pdf-print').addEventListener('click', () => window.print());
document.addEventListener('keydown', event => { if (event.key === 'Escape' && !pdf.hidden) closePdf(); });

function closePdf() {
    pdf.hidden = true;
    document.body.classList.remove('pdf-open');
}

function setMode(mode) {
    doc.classList.toggle('summary', mode === 'summary');
    $('mode-summary').setAttribute('aria-pressed', String(mode === 'summary'));
    $('mode-full').setAttribute('aria-pressed', String(mode === 'full'));
    $('pdf-hint').textContent = mode === 'summary'
        ? 'A4 1쪽 · 결재·현장 첨부용 요약본'
        : '전체 6개 절 · 근거와 데이터 한계 포함';
}
$('mode-summary').addEventListener('click', () => setMode('summary'));
$('mode-full').addEventListener('click', () => setMode('full'));
setMode('summary');

function docSection(number$, title, full) {
    const section = el('section', 'sec');
    if (full) section.dataset.full = '';
    const heading = el('h2');
    heading.append(el('b', null, number$));
    heading.append(document.createTextNode(title));
    section.append(heading);
    return section;
}

function docTable(headers, rows, numeric = []) {
    const node = el('table', 'dtb');
    const head = el('tr');
    headers.forEach((header, index) => {
        const th = el('th', null, header);
        if (numeric[index]) th.style.textAlign = 'right';
        head.append(th);
    });
    node.append(head);
    rows.forEach(row => {
        const line = el('tr');
        row.forEach((cell, index) => {
            const td = el('td', numeric[index] ? 'num' : '');
            if (cell instanceof Node) td.append(cell); else td.textContent = text(cell);
            line.append(td);
        });
        node.append(line);
    });
    return node;
}

function renderDoc(dashboard, report) {
    const sheet = clear('sheet');
    const diagnosis = dashboard.diagnosis || {};
    const hero = report?.hero || {};
    const summary = report?.summarySheet || {};
    const evidence = report?.evidence || {};
    const risks = report?.risks || dashboard.risks || [];
    const proposals = report?.operationProposal?.items || dashboard.recommendations || [];
    const profile = report?.dataSummary?.profile || dashboard.profile;
    const verdict = verdictOf(dashboard.risks || []);
    const name = hero.festivalName || diagnosis.festivalName || '축제';

    const meta = el('div', 'docmeta');
    meta.append(el('span', null, dashboard.reportId || '-'));
    meta.append(el('span', null, `생성 ${new Date(evidence.generatedAt || Date.now()).toLocaleString('ko-KR', { dateStyle: 'medium', timeStyle: 'short' })}`));
    meta.append(el('span', null, '축제날씨 진단 리포트'));
    sheet.append(meta);

    sheet.append(el('h1', null, `${name} 개최 여건 진단`));
    sheet.append(el('p', 'lede', [
        `${kdate(hero.startDate || diagnosis.startDate)} ~ ${kdate(hero.endDate || diagnosis.endDate)}`,
        dashboard.festivalLocation?.address || diagnosis.festivalAddress,
        diagnosis.festivalType, diagnosis.scale, diagnosis.recurrenceType ? `${diagnosis.recurrenceType} 개최` : null
    ].filter(Boolean).join(' · ')));

    /* §1 개요와 판정 */
    const s1 = docSection('§1', '개요와 판정');
    const verdictBox = el('div', 'dverdict');
    const stamp = el('div', `stamp t-${verdict.tone}`);
    stamp.append(document.createTextNode(verdict.stamp));
    stamp.append(el('small', null, verdict.stampEn));
    verdictBox.append(stamp);
    verdictBox.append(el('p', null, hero.briefing || state.ai?.briefing?.text || verdictSentence(dashboard)));
    s1.append(verdictBox);

    const facts = el('ul', 'facts');
    (summary.keyFacts || []).forEach(fact => facts.append(el('li', null, fact)));
    if (hero.diagnosisTiming) facts.append(el('li', null, `진단 시점 ${hero.diagnosisTiming} · 예측 데이터 ${hero.forecastDataActive ? '활성' : '비활성'}`));
    if (dashboard.festivalLocation) {
        facts.append(el('li', null, `좌표 출처 ${SOURCE_LABEL[dashboard.festivalLocation.source] || dashboard.festivalLocation.source}`
            + `${dashboard.festivalLocation.precise ? ' · 반경 판정 유효' : ' · 근사 좌표로 일부 판정 제외'}`));
    }
    if (facts.children.length) s1.append(facts);
    sheet.append(s1);

    /* §2 핵심 사실 */
    const peak = peakOf(dashboard.concentration);
    const festivals = dashboard.competing?.festivals || [];
    const nearest = festivals.map(f => Number(f.distanceKm)).filter(v => !Number.isNaN(v)).sort((a, b) => a - b)[0];
    const topPlace = (dashboard.volatility?.places || [])
        .filter(p => p.increasePoint !== null && p.increasePoint !== undefined)
        .sort((a, b) => b.increasePoint - a.increasePoint)[0];
    const visitors = dashboard.regionalVisitors;

    const factRows = [];
    if (peak) factRows.push(['예상 최대 집중률', `${round1(peak.rate)}%`, `${peak.placeName} · ${kdate(peak.date)}`]);
    if (topPlace) factRows.push(['최대 상승폭', `+${round1(topPlace.increasePoint)}%p`, `${topPlace.placeName} · 자기평균 ${round1(topPlace.selfAverage)}%`]);
    factRows.push(['동기간 인근 축제', `${text(dashboard.competing?.totalCount)}건`, nearest !== undefined ? `최근접 ${round1(nearest)}km` : '겹치는 축제 없음']);
    factRows.push(['여유 관광지', `${text(dashboard.distribution?.totalCount)}곳`, `관광지 변동 대상 ${text(dashboard.volatility?.totalCount)}곳`]);
    if (visitors?.festivalPeriodAverage) {
        factRows.push(['시군구 방문자(참고)', `${number(visitors.festivalPeriodAverage)}명`,
            visitors.changeFromBeforePercent !== null && visitors.changeFromBeforePercent !== undefined
                ? `직전 대비 ${round1(visitors.changeFromBeforePercent)}%` : '-']);
    }
    const s2 = docSection('§2', '핵심 사실');
    s2.append(docTable(['항목', '값', '비교 기준'], factRows, [false, true, false]));
    sheet.append(s2);

    /* §3 리스크 (요약본에도 포함) */
    const sRisk = docSection('§3', `리스크 ${risks.length}건`);
    if (!risks.length) {
        sRisk.append(el('p', 'dnote', '매칭된 리스크가 없습니다.'));
    } else {
        sRisk.append(docTable(['심각도', '내용', '연계 제안'], risks.map(risk => {
            const severityCell = el('div');
            severityCell.append(el('span', `dsev ${risk.severity}`, risk.severity));
            severityCell.append(el('div', 'code', risk.riskCode));
            const bodyCell = el('div');
            bodyCell.append(el('b', null, risk.title || risk.riskCode));
            if (risk.description) bodyCell.append(el('div', 'sm', risk.description));
            return [severityCell, bodyCell, (risk.recommendationCodes || []).join(', ') || '-'];
        }), [false, false, true]));
    }
    sheet.append(sRisk);

    /* §4 운영 조정 제안 (요약본에도 포함) */
    const sProposal = docSection('§4', '운영 조정 제안');
    if (report?.operationProposal?.guidanceNote) sProposal.append(el('p', 'dnote', report.operationProposal.guidanceNote));
    if (!proposals.length) {
        sProposal.append(el('p', 'dnote', '연계된 제안이 없습니다.'));
    } else {
        sProposal.append(docTable(['코드', '조치', '난이도'], proposals.map(item => {
            const bodyCell = el('div');
            bodyCell.append(el('b', null, item.title || item.recommendationCode));
            if (item.defaultAction) bodyCell.append(el('div', 'sm', item.defaultAction));
            if ((item.relatedRiskCodes || []).length) bodyCell.append(el('div', 'code', `연계 ${item.relatedRiskCodes.join(', ')}`));
            return [item.recommendationCode, bodyCell, text(item.difficulty)];
        }), [true, false, true]));
    }
    sheet.append(sProposal);

    /* §5 데이터 요약 (전체 리포트 전용) */
    const sData = docSection('§5', '데이터 요약', true);
    if (profile) {
        PROFILE_AXES.forEach(([label, key]) => {
            const metric = profile[key];
            const row = el('div', 'dmeter');
            row.append(el('span', null, label));
            const track = el('span', 'tr');
            const fill = el('i');
            fill.style.width = `${ratio(metric)}%`;
            track.append(fill);
            row.append(track);
            row.append(el('span', 'vl', metricText(metric)));
            sData.append(row);
        });
        (profile.notes || []).forEach(note => sData.append(el('p', 'dnote', note)));
    }
    const volatilityPlaces = dashboard.volatility?.places || [];
    if (volatilityPlaces.length) {
        sData.append(el('p', 'dnote', '관광지 변동 상황'));
        sData.append(docTable(['관광지', '배지', '상승폭', '최고 집중률', '최대 발생일'], volatilityPlaces.map(place => [
            place.placeName,
            badgeOf(place.level).label,
            place.increasePoint === null || place.increasePoint === undefined ? '-' : `${round1(place.increasePoint)}%p`,
            place.peakRate === null || place.peakRate === undefined ? '-' : `${round1(place.peakRate)}%`,
            kdate(place.peakDate)
        ]), [false, false, true, true, true]));
    }
    if (festivals.length) {
        sData.append(el('p', 'dnote', '동기간 인근 축제'));
        sData.append(docTable(['축제명', '기간', '거리', '작년 방문'], festivals.map(festival => [
            festival.festivalName,
            `${kdate(festival.startDate)} ~ ${kdate(festival.endDate)}`,
            `${text(round1(festival.distanceKm))}km`,
            festival.lastYearVisitors ? `${number(festival.lastYearVisitors)}명` : '-'
        ]), [false, true, true, true]));
    }
    sheet.append(sData);

    /* §6 근거와 한계 (전체 리포트 전용) */
    const sEvidence = docSection('§6', '근거와 한계', true);
    const statuses = evidence.dataStatuses || dashboard.dataStatuses || [];
    if (statuses.length) {
        sEvidence.append(docTable(['데이터', '상태', '기준 시점', '사유'], statuses.map(item => [
            item.source,
            el('span', `dstatus ${item.status}`, STATUS_LABEL[item.status] || item.status),
            text(item.referencePeriod),
            text(item.reason)
        ]), [false, false, true, false]));
    }
    (evidence.referencePeriodNotes || []).forEach(note => sEvidence.append(el('p', 'dnote', note)));
    const limitation = evidence.limitationNote || summary.limitationNote;
    if (limitation) sEvidence.append(el('p', 'dnote', limitation));
    if (summary.dataSourceNote) sEvidence.append(el('p', 'dnote', summary.dataSourceNote));
    if (dashboard.festivalLocation?.notice) sEvidence.append(el('p', 'dnote', dashboard.festivalLocation.notice));
    sheet.append(sEvidence);
}
