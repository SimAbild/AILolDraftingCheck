// Global state shared between the three views. Plain object, no framework.
const ROLES = ['TOP', 'JGL', 'MID', 'ADC', 'SUPP'];

const state = {
    champions: [],      // full list from /api/v1/champions
    userRole: null,     // "TOP" | "JGL" | "MID" | "ADC" | "SUPP"
    enemyTeam: [],      // 5 DraftPicks
    allyTeam: [],       // 4 DraftPicks (does not include the user's slot)
    userChampion: null  // {id, name, iconUrl}
};

// Switch which <section> is visible.
function showView(name) {
    document.getElementById('view-home').style.display  = name === 'home'  ? 'block' : 'none';
    document.getElementById('view-draft').style.display = name === 'draft' ? 'block' : 'none';
    document.getElementById('view-coach').style.display = name === 'coach' ? 'block' : 'none';
    hideError();
    window.scrollTo(0, 0);
}

function showError(message) {
    const banner = document.getElementById('error-banner');
    banner.textContent = message;
    banner.style.display = 'block';
}

function hideError() {
    document.getElementById('error-banner').style.display = 'none';
}

// Tiny HTML-escape helper used wherever we inject untrusted strings.
function escapeHtml(s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, c => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
}
