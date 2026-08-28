/* =========================================================
   두 화면(index.html · diagnosis-test.html)이 함께 쓰는 헬퍼.
   각 화면 스크립트보다 먼저 로드해야 한다.
   areaCode·signguCode 요소 참조는 각 화면 스크립트가 정의한다.
   ========================================================= */
const $ = id => document.getElementById(id);
const el = (tag, className, content) => {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (content !== undefined && content !== null) node.textContent = content;
    return node;
};
const clear = id => { const node = $(id); node.replaceChildren(); return node; };
const text = v => v === null || v === undefined || v === '' ? '-' : String(v);
const number = v => v === null || v === undefined ? '-' : Number(v).toLocaleString('ko-KR');
const dateText = d => d.toISOString().slice(0, 10);
const tourDate = v => v.replaceAll('-', '');
const isoDate = v => `${v.slice(0, 4)}-${v.slice(4, 6)}-${v.slice(6, 8)}`;

const svgNode = (tag, attributes, content) => {
    const node = document.createElementNS('http://www.w3.org/2000/svg', tag);
    Object.entries(attributes).forEach(([k, v]) => node.setAttribute(k, v));
    if (content !== undefined) node.textContent = content;
    return node;
};

async function request(url, options = {}) {
    const response = await fetch(url, { headers: { 'Content-Type': 'application/json' }, ...options });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(body.message || body.detail || `HTTP ${response.status}`);
    return body;
}

async function loadRegions() {
    const response = await request('/api/v1/diagnosis-regions');
    areaCode.replaceChildren();
    response.regions.forEach(region => areaCode.add(new Option(region.name, region.areaCode)));
    const gangwon = response.regions.find(region => region.areaCode === '51');
    if (gangwon) areaCode.value = gangwon.areaCode;
    await loadDistricts(areaCode.value);
}

async function loadDistricts(selectedAreaCode, preferredSignguCode) {
    const response = await request(`/api/v1/diagnosis-regions/${selectedAreaCode}/districts`);
    signguCode.replaceChildren();
    response.districts.forEach(district => signguCode.add(new Option(district.name, district.signguCode)));
    const preferred = response.districts.find(d => d.signguCode === (preferredSignguCode || '51130'));
    if (preferred) signguCode.value = preferred.signguCode;
}

let mapsPromise = null;

// Maps JS API는 한 번만 불러온다. 키는 서버 설정(/api/v1/client-config)에서 받는다.
function loadGoogleMaps() {
    if (mapsPromise) return mapsPromise;
    mapsPromise = request('/api/v1/client-config').then(config => {
        if (!config.googleMapsApiKey) {
            throw new Error('GOOGLE_MAPS_API_KEY가 비어 있습니다. .env에 키를 넣고 앱을 재시작하세요.');
        }
        return new Promise((resolve, reject) => {
            window.__mapsReady = () => resolve(window.google.maps);
            const script = document.createElement('script');
            script.async = true;
            script.src = 'https://maps.googleapis.com/maps/api/js'
                + `?key=${encodeURIComponent(config.googleMapsApiKey)}`
                + '&language=ko&region=KR&callback=__mapsReady';
            script.onerror = () => reject(new Error('Google Maps 스크립트를 불러오지 못했습니다. 키 제한·결제 설정을 확인하세요.'));
            document.head.append(script);
        });
    });
    mapsPromise.catch(() => { mapsPromise = null; });
    return mapsPromise;
}

// 배지별 색이 다른 원형 마커. Advanced Marker는 Map ID가 필요해 기본 심볼을 쓴다.
function circleSymbol(maps, color, scale) {
    return { path: maps.SymbolPath.CIRCLE, scale, fillColor: color, fillOpacity: .9, strokeColor: '#ffffff', strokeWeight: 1.5 };
}

function clearOverlays(overlays) {
    overlays.forEach(overlay => overlay.setMap(null));
    overlays.length = 0;
}
