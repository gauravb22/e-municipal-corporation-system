document.querySelectorAll('.star').forEach(star => {
    star.addEventListener('click', function(){
        const workId = this.getAttribute('data-work');
        const value = this.getAttribute('data-value');
        const already = document.querySelector('[data-rated="' + workId + '"]');
        document.getElementById('rating-' + workId).value = value;
        const form = document.getElementById('rateForm-' + workId);
        const stars = document.querySelectorAll('.star[data-work="' + workId + '"]');
        stars.forEach(s => {
            if (s.getAttribute('data-value') <= value) {
                s.classList.add('active');
            } else {
                s.classList.remove('active');
            }
        });
        if (form) form.submit();
    });
});

const wardSelect = document.getElementById('filterWard');
const zoneSelect = document.getElementById('filterZone');
const allSelect = document.getElementById('filterAll');
const posts = Array.from(document.querySelectorAll('.post'));

function buildFilters() {
    const wards = new Set();
    const zones = new Set();
    posts.forEach(p => {
        const w = p.getAttribute('data-ward');
        const z = p.getAttribute('data-zone');
        if (w) wards.add(w);
        if (z) zones.add(z);
    });
    Array.from(wards).sort((a,b) => Number(a) - Number(b)).forEach(w => {
        const opt = document.createElement('option');
        opt.value = w;
        opt.textContent = w;
        wardSelect.appendChild(opt);
    });
    Array.from(zones).sort().forEach(z => {
        const opt = document.createElement('option');
        opt.value = z;
        opt.textContent = z;
        zoneSelect.appendChild(opt);
    });
}

function applyFilters() {
    const ward = wardSelect.value;
    const zone = zoneSelect.value;
    posts.forEach(p => {
        const matchWard = ward === 'all' || p.getAttribute('data-ward') === ward;
        const matchZone = zone === 'all' || p.getAttribute('data-zone') === zone;
        p.style.display = (matchWard && matchZone) ? '' : 'none';
    });
}

if (wardSelect && zoneSelect && allSelect) {
    buildFilters();
    wardSelect.addEventListener('change', applyFilters);
    zoneSelect.addEventListener('change', applyFilters);
    allSelect.addEventListener('change', () => {
        wardSelect.value = 'all';
        zoneSelect.value = 'all';
        applyFilters();
    });
}
