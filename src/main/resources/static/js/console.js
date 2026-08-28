const form = document.getElementById('diagnosis-form');
const recurrenceType = document.getElementById('recurrence-type');
const areaCode = document.getElementById('area-code');
const signguCode = document.getElementById('signgu-code');
const newLocationFields = document.getElementById('new-location-fields');
const existingContentField = document.getElementById('existing-content-field');
const pickedFestival = document.getElementById('picked-festival');
const notice = document.getElementById('notice');
const result = document.getElementById('result');
const statusList = document.getElementById('status-list');
const submitButton = document.getElementById('submit-button');
const aiButton = document.getElementById('ai-button');
const picker = document.getElementById('picker');

const state = {
    reportId: null, dashboard: null, report: null,
    map: null, overlays: [], infoWindow: null,
    pickerMap: null, pickerOverlays: [], pickerInfoWindow: null
};

/* 공용 헬퍼(text·number·el·clear·request 등)는 common.js에 있다. */
const today = new Date();
const start = new Date(today); start.setDate(today.getDate() + 7);
const end = new Date(today); end.setDate(today.getDate() + 10);
form.startDate.value = dateText(start);
form.endDate.value = dateText(end);

loadRegions().catch(error => fail(`지역 목록을 불러오지 못했다: ${error.message}`));

areaCode.addEventListener('change', () => loadDistricts(areaCode.value));

recurrenceType.addEventListener('change', () => {
    const isNew = recurrenceType.value === '신규';
    newLocationFields.classList.toggle('hidden', !isNew);
    existingContentField.classList.toggle('hidden', isNew);
});

document.getElementById('tabs').addEventListener('click', event => {
    const button = event.target.closest('button[data-tab]');
    if (!button) return;
    document.querySelectorAll('#tabs button').forEach(item => item.classList.toggle('active', item === button));
    ['dashboard', 'map', 'report', 'ai', 'raw'].forEach(name => {
        document.getElementById(`tab-${name}`).classList.toggle('hidden', name !== button.dataset.tab);
    });
});

form.addEventListener('submit', async event => {
    event.preventDefault();
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

    submitButton.disabled = true;
    notice.className = 'notice';
    notice.textContent = '진단을 실행하는 중…';
    statusList.replaceChildren();
    try {
        const created = await request('/api/v1/reports', { method: 'POST', body: JSON.stringify(payload) });
        state.reportId = created.reportId;
        state.dashboard = await request(`/api/v1/reports/${created.reportId}/dashboard`);
        state.report = await request(`/api/v1/reports/${created.reportId}/forecast-report`);
        notice.textContent = `reportId: ${created.reportId} · 상태: ${state.dashboard.status}`;
        renderStatuses(state.dashboard.dataStatuses || []);
        renderDashboard(state.dashboard);
        renderReport(state.report);
        result.textContent = JSON.stringify({ dashboard: state.dashboard, forecastReport: state.report }, null, 2);
        renderMap(state.dashboard).catch(error => {
            document.getElementById('map-notice').textContent = error.message;
        });
    } catch (error) {
        fail(error.message);
        result.textContent = '요청 실패';
    } finally {
        submitButton.disabled = false;
    }
});

aiButton.addEventListener('click', async () => {
    if (!state.reportId) return fail('먼저 진단을 실행해야 한다.');
    const baseUrl = document.getElementById('ai-base-url').value.replace(/\/+$/, '');
    aiButton.disabled = true;
    try {
        renderAi(await request(`${baseUrl}/api/v1/reports/${state.reportId}/ai-report`, { method: 'POST' }));
    } catch (error) {
        clear('ai-result').append(el('p', 'notice error', `ai-service 호출 실패: ${error.message}`));
    } finally {
        aiButton.disabled = false;
    }
});

function fail(message) {
    notice.className = 'notice error';
    notice.textContent = message;
}

function renderStatuses(statuses) {
    const tone = { AVAILABLE: 'ok', NO_DATA: 'warn', OUT_OF_FORECAST_RANGE: 'warn', FAILED: 'bad' };
    statuses.forEach(item => {
        const chip = el('span', `chip ${tone[item.status] || 'mute'}`, `${item.source}: ${item.status}`);
        chip.title = item.reason || item.referencePeriod || '';
        statusList.append(chip);
    });
}

/* ---------------------------------------------------------------- 지도 (Part 5.5) */

async function renderMap(dashboard) {
    const info = document.getElementById('map-notice');
    const legend = clear('map-legend');
    const location = dashboard.festivalLocation;
    const mapData = dashboard.map;

    const sourceLabel = {
        KOR_SERVICE: '재개최 축제 · API #8 자동 로드',
        USER_INPUT: '신규 축제 · 사용자 입력 좌표',
        SIGNGU_CENTER: '미입력 · 시군구 중심 근사 (API #6 평균)',
        UNAVAILABLE: '좌표 미확보'
    };
    info.textContent = location
        ? `좌표 출처: ${sourceLabel[location.source] || location.source} · precise=${location.precise}`
            + (location.notice ? ` · ${location.notice}` : '')
        : '위치 정보 없음';

    if (!mapData || !mapData.site) {
        legend.append(el('span', 'chip bad', 'R-VOL-005 · O-INF-003 판정 스킵'));
        return;
    }

    const withinCount = (mapData.places || []).filter(place => place.withinNearestRadius).length;
    legend.append(el('span', 'chip', `관광지 마커 ${(mapData.places || []).length}개`));
    legend.append(el('span', 'chip', `인근 축제 ${(mapData.nearbyFestivals || []).length}건`));
    legend.append(el('span', location.precise ? 'chip ok' : 'chip warn',
        location.precise ? `1km 이내 ${withinCount}곳 · R-VOL-005 판정 가능` : 'R-VOL-005 · O-INF-003 판정 스킵'));

    const maps = await loadGoogleMaps();
    const center = { lat: Number(mapData.site.latitude), lng: Number(mapData.site.longitude) };
    if (!state.map) {
        state.map = new maps.Map(document.getElementById('map'), {
            center, zoom: 11, mapTypeControl: false, streetViewControl: false
        });
        state.infoWindow = new maps.InfoWindow();
    }
    state.map.setCenter(center);
    clearOverlays(state.overlays);

    state.overlays.push(new maps.Marker({
        map: state.map, position: center, title: '축제장', zIndex: 999,
        icon: circleSymbol(maps, '#2457d6', 9)
    }));
    state.overlays.push(new maps.Circle({
        map: state.map, center, radius: (mapData.nearestRadiusKm || 1) * 1000,
        strokeColor: '#2457d6', strokeWeight: 1, fillColor: '#2457d6', fillOpacity: .06
    }));

    const badgeColor = { SURGING: '#c0392b', WARNING: '#c99a2e', STABLE: '#5b8a6f', RELAXED: '#2457d6' };
    (mapData.places || []).forEach(place => {
        const marker = new maps.Marker({
            map: state.map,
            position: { lat: Number(place.latitude), lng: Number(place.longitude) },
            title: place.placeName,
            icon: circleSymbol(maps, badgeColor[place.badge] || '#98a2b3', place.withinNearestRadius ? 8 : 5)
        });
        marker.addListener('click', () => {
            state.infoWindow.setContent(
                `<div class="info-window"><strong>${place.placeName}</strong>`
                + `${text(place.badge)} · 상승폭 ${text(place.peakIncreasePoint)}%p<br>`
                + `${text(place.distanceKm)}km · hubRank ${text(place.hubRank)} · ${text(place.category)}</div>`
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
                + `${text(festival.startDate)} ~ ${text(festival.endDate)}<br>${text(festival.distanceKm)}km</div>`
            );
            state.infoWindow.open({ map: state.map, anchor: marker });
        });
        state.overlays.push(marker);
    });
}

/* ---------------------------------------------------------------- 개최지 선택 지도 (API #8) */

document.getElementById('open-picker').addEventListener('click', () => {
    document.getElementById('picker-from').value = form.startDate.value;
    const to = new Date(form.endDate.value);
    to.setDate(to.getDate() + 60);
    document.getElementById('picker-to').value = dateText(to);
    picker.showModal();
    openPicker().catch(error => {
        document.getElementById('picker-notice').className = 'notice error';
        document.getElementById('picker-notice').textContent = error.message;
    });
});

document.getElementById('close-picker').addEventListener('click', () => picker.close());
document.getElementById('picker-reload').addEventListener('click', () => openPicker().catch(error => {
    document.getElementById('picker-notice').className = 'notice error';
    document.getElementById('picker-notice').textContent = error.message;
}));

// API #8에서 해당 지역 축제를 받아 개최지를 핀으로 찍는다. 핀을 고르면 위저드 입력이 채워진다.
async function openPicker() {
    const pickerNotice = document.getElementById('picker-notice');
    pickerNotice.className = 'notice';
    pickerNotice.textContent = '축제 개최지를 불러오는 중…';

    const maps = await loadGoogleMaps();
    if (!state.pickerMap) {
        state.pickerMap = new maps.Map(document.getElementById('picker-map'), {
            center: { lat: 36.5, lng: 127.8 }, zoom: 7, mapTypeControl: false, streetViewControl: false
        });
        state.pickerInfoWindow = new maps.InfoWindow();
    }

    // 데이터랩 시군구 코드 51130 = 법정동 시도 51 + 시군구 130
    const signguOnly = document.getElementById('picker-signgu-only').checked;
    const parameters = new URLSearchParams({
        startDate: tourDate(document.getElementById('picker-from').value),
        endDate: tourDate(document.getElementById('picker-to').value),
        legalDongRegionCode: areaCode.value,
        size: '100'
    });
    if (signguOnly) {
        parameters.set('legalDongSignguCode', signguCode.value.slice(2));
    }

    const response = await request(`/api/v1/festivals?${parameters}`);
    const festivals = (response.festivals || []).filter(festival => festival.latitude && festival.longitude);

    clearOverlays(state.pickerOverlays);
    const bounds = new maps.LatLngBounds();
    festivals.forEach(festival => {
        const position = { lat: Number(festival.latitude), lng: Number(festival.longitude) };
        const marker = new maps.Marker({ map: state.pickerMap, position, title: festival.title });
        marker.addListener('click', () => showFestivalInfo(marker, festival));
        state.pickerOverlays.push(marker);
        bounds.extend(position);
    });

    if (festivals.length) {
        state.pickerMap.fitBounds(bounds);
    }
    pickerNotice.textContent = `총 ${response.totalCount}건 중 좌표가 있는 ${festivals.length}건을 표시했다. 핀을 눌러 선택한다.`;
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
        const combined = festival.legalDongRegionCode + (festival.legalDongSignguCode || '');
        await loadDistricts(areaCode.value, combined);
    }
    if (festival.eventStartDate && festival.eventEndDate) {
        form.startDate.value = isoDate(festival.eventStartDate);
        form.endDate.value = isoDate(festival.eventEndDate);
    }

    pickedFestival.className = 'notice';
    pickedFestival.textContent = `선택됨: ${festival.title} (contentId ${festival.contentId})`
        + ` · 좌표는 진단 시 API #8에서 자동 로드된다.`;
    state.pickerInfoWindow?.close();
    if (picker.open) picker.close();
}

/* ---------------------------------------------------------------- M2 대시보드 */

function renderDashboard(dashboard) {
    const root = clear('tab-dashboard');

    root.append(el('h3', null, '데이터 프로필 · 수평 막대 4축'));
    root.append(profileBars(dashboard.profile));

    root.append(el('h3', null, '뷰 01 · 개최기간 관광 흐름'));
    root.append(flowChart(dashboard.concentration));

    root.append(el('h3', null, `뷰 02 · 관광지 변동 상황 (전체 ${text(dashboard.volatility?.totalCount)}곳)`));
    root.append(table(
        ['관광지', '배지', '상승폭(%p)', '자기평균', '최고 집중률', '최대 발생일'],
        (dashboard.volatility?.places || []).map(place => [
            place.placeName, badgeChip(place.level), place.increasePoint, place.selfAverage, place.peakRate, place.peakDate
        ])
    ));

    root.append(el('h3', null, `뷰 03 · 여유 관광지 (전체 ${text(dashboard.distribution?.totalCount)}곳)`));
    root.append(cards((dashboard.distribution?.places || []).map(place => ({
        title: `${place.rank}. ${place.placeName}`,
        meta: [`유형: ${text(place.category)}`, `연관 순위: ${text(place.relatedRank)} · hubRank: ${text(place.hubRank)}`,
            `개최기간 평균 집중률: ${text(place.value)}`]
    }))));

    root.append(el('h3', null, `뷰 04 · 동기간 인근 축제 (전체 ${text(dashboard.competing?.totalCount)}건)`));
    root.append(cards((dashboard.competing?.festivals || []).map(festival => ({
        title: festival.festivalName,
        meta: [`${text(festival.startDate)} ~ ${text(festival.endDate)} · ${text(festival.distanceKm)}km`,
            `작년 방문 ${number(festival.lastYearVisitors)}명 · 예산 ${number(festival.budgetMillionWon)}백만원 (CSV #9)`,
            (festival.linkageTags || []).join(' · ')]
    }))));

    root.append(el('h3', null, '재개최 실적 (CSV #9)'));
    root.append(historyBlock(dashboard.festivalHistory));

    root.append(el('h3', null, '시군구 방문자 (API #7)'));
    root.append(visitorBlock(dashboard.regionalVisitors));

    root.append(el('h3', null, `리스크 (${(dashboard.risks || []).length}건)`));
    root.append(riskList(dashboard.risks));

    root.append(el('h3', null, `운영 조정 제안 (${(dashboard.recommendations || []).length}건)`));
    root.append(cards((dashboard.recommendations || []).map(item => ({
        title: `${item.recommendationCode} · ${item.title}`,
        meta: [item.defaultAction, `${text(item.category)} · 난이도 ${text(item.difficulty)} · 연계 ${(item.relatedRiskCodes || []).join(', ') || '-'}`]
    }))));
}

function profileBars(profile) {
    if (!profile) return empty('프로필 없음');
    const wrapper = el('div');
    const axes = [
        ['시기적합도', profile.timingFit],
        ['여유 관광지', profile.relaxedPlaces],
        ['연계 풍부도', profile.connectivity],
        ['카테고리 다양성', profile.categoryDiversity]
    ];
    axes.forEach(([name, metric]) => {
        const row = el('div', 'bar-row');
        row.append(el('span', null, name));
        const bar = el('div', 'bar');
        const fill = el('span');
        fill.style.width = `${ratio(metric)}%`;
        bar.append(fill);
        row.append(bar);
        const value = metric && metric.value !== null && metric.value !== undefined
            ? `${metric.value}${metric.unit || ''}${metric.maximum ? ` / ${metric.maximum}` : ''}`
            : '데이터 없음';
        row.append(el('span', 'bar-value', value));
        wrapper.append(row);
    });
    const notes = el('ul', 'notes');
    (profile.notes || []).forEach(note => notes.append(el('li', null, note)));
    wrapper.append(notes);
    return wrapper;
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

function flowChart(concentration) {
    const days = concentration?.dailyConcentrations || [];
    if (!days.length) return empty('집중률 예측 데이터 없음');

    const series = new Map();
    days.forEach((day, index) => (day.places || []).forEach(place => {
        if (!series.has(place.placeName)) series.set(place.placeName, new Array(days.length).fill(null));
        series.get(place.placeName)[index] = place.concentrationRate;
    }));

    const width = 720, height = 240, padX = 36, padY = 16;
    const stepX = days.length > 1 ? (width - padX * 2) / (days.length - 1) : 0;
    const svg = svgNode('svg', { viewBox: `0 0 ${width} ${height}` });
    svg.style.width = '100%';
    svg.style.height = 'auto';

    [0, 50, 100].forEach(value => {
        const y = padY + (height - padY * 2) * (1 - value / 100);
        svg.append(svgNode('line', { x1: padX, y1: y, x2: width - padX, y2: y, stroke: '#eef0f4' }));
        svg.append(svgNode('text', { x: 4, y: y + 4, 'font-size': 10, fill: '#98a2b3' }, String(value)));
    });

    const palette = ['#0b2545', '#c0392b', '#c99a2e', '#5b8a6f', '#2457d6', '#8e44ad', '#16a085', '#d35400', '#7f8c8d', '#2c3e50'];
    const legend = el('div', 'chips');
    [...series.entries()].forEach(([name, values], index) => {
        const points = values
            .map((value, dayIndex) => value === null ? null : `${padX + stepX * dayIndex},${padY + (height - padY * 2) * (1 - value / 100)}`)
            .filter(Boolean)
            .join(' ');
        svg.append(svgNode('polyline', { points, fill: 'none', stroke: palette[index % palette.length], 'stroke-width': 2 }));
        const chip = el('span', 'chip mute', name);
        chip.style.color = palette[index % palette.length];
        legend.append(chip);
    });

    days.forEach((day, index) => {
        if (days.length > 8 && index % 2 === 1) return;
        svg.append(svgNode('text', {
            x: padX + stepX * index, y: height - 2, 'font-size': 10, fill: '#98a2b3', 'text-anchor': 'middle'
        }, String(day.date).slice(5)));
    });

    const wrapper = el('div');
    wrapper.append(svg, legend);
    return wrapper;
}

function badgeChip(level) {
    const tone = { SURGING: 'bad', WARNING: 'warn', STABLE: 'ok', RELAXED: 'mute' };
    return el('span', `chip ${tone[level] || 'mute'}`, text(level));
}

function historyBlock(history) {
    if (!history) return empty('문체부 CSV에서 이 축제를 찾지 못했다 (CSV 미배치 시에도 동일).');
    return cards([{
        title: history.festivalName,
        meta: [`${text(history.regionName)} ${text(history.signguName)}`,
            `작년 방문 ${number(history.lastYearVisitors)}명 · 예산 ${number(history.budgetMillionWon)}백만원`,
            `최초 개최 ${text(history.firstHeldYear)}년 · ${text(history.roundCount)}회차`]
    }]);
}

function visitorBlock(visitors) {
    if (!visitors || !(visitors.dailyVisitors || []).length) return empty('방문자 데이터 없음');
    const wrapper = el('div');
    wrapper.append(cards([{
        title: `참고 기간 ${text(visitors.referenceFestivalStartDate)} ~ ${text(visitors.referenceFestivalEndDate)}`,
        meta: [`개최기간 평균 ${number(visitors.festivalPeriodAverage)}명`,
            `직전 ${number(visitors.beforePeriodAverage)}명 · 직후 ${number(visitors.afterPeriodAverage)}명`,
            `직전 대비 ${text(visitors.changeFromBeforePercent)}%`]
    }]));
    wrapper.append(table(
        ['날짜', '현지인', '외지인', '외국인', '전체'],
        visitors.dailyVisitors.slice(0, 10).map(day => [
            day.date, number(day.localVisitors), number(day.outsideVisitors), number(day.foreignVisitors), number(day.totalVisitors)
        ])
    ));
    return wrapper;
}

function riskList(risks) {
    if (!(risks || []).length) return empty('매칭된 리스크 없음');
    const wrapper = el('div');
    risks.forEach(risk => {
        const box = el('div', `risk ${risk.severity}`);
        box.append(el('strong', null, `[${risk.severity}] ${risk.riskCode} · ${risk.title}`));
        box.append(el('div', null, risk.description || ''));
        box.append(el('div', 'meta', `근거: ${risk.metricKey} = ${text(risk.metricValue)} · ${JSON.stringify(risk.evidence || {})} → 제안 ${(risk.recommendationCodes || []).join(', ') || '-'}`));
        wrapper.append(box);
    });
    return wrapper;
}

/* ---------------------------------------------------------------- M3 리포트 */

function renderReport(report) {
    const root = clear('tab-report');
    if (!report) return root.append(empty('리포트 없음'));

    const hero = report.hero || {};
    root.append(el('h3', null, '§1 히어로'));
    root.append(cards([{
        title: hero.festivalName,
        meta: [`${text(hero.startDate)} ~ ${text(hero.endDate)}`,
            `${text(hero.diagnosisTiming)} (예측 데이터 활성: ${hero.forecastDataActive})`,
            `활용 데이터: ${(hero.availableDataSources || []).join(', ')}`,
            `브리핑: ${hero.briefing || 'AI 탭에서 생성'}`]
    }]));

    const summary = report.summarySheet || {};
    root.append(el('h3', null, '§2 A4 요약본'));
    const facts = el('ul', 'notes');
    (summary.keyFacts || []).forEach(fact => facts.append(el('li', null, fact)));
    root.append(facts);
    root.append(cards((summary.topRisks || []).map(risk => ({
        title: `${risk.riskCode} · ${risk.severity}`, meta: [risk.title, risk.description]
    }))));
    root.append(cards((summary.topRecommendations || []).map(item => ({
        title: `${item.recommendationCode} · ${item.title}`, meta: [item.defaultAction]
    }))));
    root.append(el('p', 'notes', summary.dataSourceNote || ''));
    root.append(el('p', 'notes', summary.limitationNote || ''));

    root.append(el('h3', null, '§3 데이터 요약'));
    root.append(profileBars(report.dataSummary?.profile));

    root.append(el('h3', null, `§4 리스크 (${(report.risks || []).length}건)`));
    root.append(riskList(report.risks));

    root.append(el('h3', null, '§5 운영 조정 제안'));
    root.append(el('p', 'notes', report.operationProposal?.guidanceNote || ''));
    root.append(cards((report.operationProposal?.items || []).map(item => ({
        title: `${item.recommendationCode} · ${item.title}`,
        meta: [item.defaultAction, `${text(item.category)} · 난이도 ${text(item.difficulty)} · 연계 ${(item.relatedRiskCodes || []).join(', ') || '-'}`]
    }))));

    root.append(el('h3', null, '§6 근거'));
    const evidence = report.evidence || {};
    const notes = el('ul', 'notes');
    (evidence.referencePeriodNotes || []).forEach(note => notes.append(el('li', null, note)));
    root.append(notes);
    root.append(table(
        ['데이터', '상태', '기준 시점', '사유'],
        (evidence.dataStatuses || []).map(item => [item.source, item.status, item.referencePeriod, item.reason])
    ));
    root.append(el('p', 'notes', evidence.limitationNote || ''));
    if (evidence.festivalLocation?.notice) root.append(el('p', 'notes', evidence.festivalLocation.notice));
}

/* ---------------------------------------------------------------- AI (Part 6) */

function renderAi(ai) {
    const root = clear('ai-result');

    // 폴백이 났으면 왜 났는지 맨 위에 보여준다.
    (ai.warnings || []).forEach(warning => root.append(el('p', 'notice error', `⚠ ${warning}`)));

    root.append(el('h3', null, '방향 C · 리포트 브리핑'));
    root.append(cards([{
        title: ai.briefing?.source === 'AI' ? 'AI 브리핑' : '브리핑 미표시 (폴백)',
        meta: [ai.briefing?.text || '-', ai.briefing?.disclaimer]
    }]));

    root.append(el('h3', null, `방향 A · 리스크 심각도 판정 (${(ai.riskSeverities || []).length}건)`));
    root.append(table(
        ['리스크', '심각도', '판정 근거', '규칙 매칭 로그', 'source'],
        (ai.riskSeverities || []).map(item => [item.riskCode, item.severity, item.reason, item.ruleMatchLog, item.source])
    ));

    root.append(el('h3', null, `방향 B · 관광지 방문 인원 추정 (${(ai.placeEstimates || []).length}곳)`));
    root.append(table(
        ['관광지', '추정', 'confidence', '근거', 'source'],
        (ai.placeEstimates || []).map(item => [item.placeName, item.displayText, item.confidence, item.reasoning, item.source])
    ));

    root.append(el('h3', null, `방향 D · 운영 제안 확장 (${(ai.recommendations || []).length}건)`));
    root.append(cards((ai.recommendations || []).map(item => ({
        title: `${item.recommendationCode} · ${item.source}`,
        meta: [`핵심 조치: ${text(item.coreAction)}`, `주차 분산: ${text(item.parkingDistribution)}`,
            `인력 배치: ${text(item.staffing)}`, `선행 조치: ${text(item.precedingAction)}`, item.disclaimer]
    }))));

    root.append(el('p', 'notes', ai.operatorPriorityNote || ''));
}

/* ---------------------------------------------------------------- 공용 렌더 */

function table(headers, rows) {
    if (!rows.length) return empty('데이터 없음');
    const node = el('table');
    const head = el('tr');
    headers.forEach(header => head.append(el('th', null, header)));
    node.append(head);
    rows.forEach(row => {
        const line = el('tr');
        row.forEach(cell => {
            const td = el('td');
            if (cell instanceof Node) td.append(cell);
            else td.textContent = text(cell);
            line.append(td);
        });
        node.append(line);
    });
    return node;
}

function cards(items) {
    if (!items.length) return empty('데이터 없음');
    const wrapper = el('div', 'cards');
    items.forEach(item => {
        const card = el('div', 'card');
        card.append(el('strong', null, text(item.title)));
        const meta = el('div', 'meta');
        item.meta.filter(Boolean).forEach(line => meta.append(el('div', null, line)));
        card.append(meta);
        wrapper.append(card);
    });
    return wrapper;
}

function empty(message) {
    return el('p', 'empty', message);
}
