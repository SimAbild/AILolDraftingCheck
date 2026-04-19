const VALID_ROLES = ['TOP', 'JGL', 'MID', 'ADC', 'SUPP'];

const state = {
    champions: [],
    userRole: null,
    enemyTeam: [],
    allyTeam: [],
    userChampion: null
};

function showView(viewName) {
    document.getElementById('view-home').style.display  = viewName === 'home'  ? 'block' : 'none';
    document.getElementById('view-draft').style.display = viewName === 'draft' ? 'block' : 'none';
    document.getElementById('view-coach').style.display = viewName === 'coach' ? 'block' : 'none';
    hideError();
    window.scrollTo(0, 0);
}

function showError(errorMessage) {
    const errorBanner = document.getElementById('error-banner');
    errorBanner.textContent = errorMessage;
    errorBanner.style.display = 'block';
}

function hideError() {
    document.getElementById('error-banner').style.display = 'none';
}

function escapeHtml(unsafeText) {
    return String(unsafeText == null ? '' : unsafeText).replace(/[&<>"']/g, character => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[character]));
}
