/* =========================================================
 *  AI LoL Drafting Coach - frontend
 *  Single-page state machine across 3 views: home -> draft -> coach
 * ========================================================= */

const API = (window.location.origin) + '/api/v1';

const state = {
    champions: [],          // full list from /champions
    userRole: null,         // chosen on home view
    enemyTeam: [],          // 5 picks
    allyTeam: [],           // 4 picks (no user slot)
    userChampion: null,     // {id, name, iconUrl}
};

// ---------- view switching ----------

const views = {
    home: document.getElementById('view-home'),
    draft: document.getElementById('view-draft'),
    coach: document.getElementById('view-coach'),
};

function show(name) {
    Object.values(views).forEach(v => v.classList.add('d-none'));
    views[name].classList.remove('d-none');
    document.getElementById('error-banner').classList.add('d-none');
    window.scrollTo(0, 0);
}

function showError(msg) {
    const banner = document.getElementById('error-banner');
    banner.textContent = msg;
    banner.classList.remove('d-none');
}

// ---------- HTTP helper ----------

async function request(url, opts = {}) {
    const res = await fetch(url, {
        headers: { 'Content-Type': 'application/json' },
        ...opts,
    });
    if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.message || res.statusText);
    }
    return res.json();
}

// ---------- bootstrap ----------

document.addEventListener('DOMContentLoaded', async () => {
    // Wire up role buttons
    document.querySelectorAll('.role-btn').forEach(btn => {
        btn.addEventListener('click', () => onRolePicked(btn.dataset.role));
    });
    document.getElementById('back-home').addEventListener('click', () => show('home'));
    document.getElementById('restart').addEventListener('click', () => show('home'));
    document.getElementById('ask-coach').addEventListener('click', onAskCoach);

    // Champion list (used by autocomplete)
    try {
        state.champions = await request(API + '/champions');
    } catch (e) {
        showError('Could not load champion list: ' + e.message);
    }
});

// ---------- view 1 -> view 2: generate draft ----------

async function onRolePicked(role) {
    state.userRole = role;
    show('draft');
    document.getElementById('user-role-label').textContent = role;
    document.getElementById('champ-input').value = '';
    document.getElementById('champ-suggestions').innerHTML = '';
    document.getElementById('ask-coach').disabled = true;
    state.userChampion = null;
    renderTeams('ally-team', 'enemy-team', /*includeUserSlot=*/true);

    document.getElementById('draft-spinner').classList.remove('d-none');
    try {
        const draft = await request(API + '/draft', {
            method: 'POST',
            body: JSON.stringify({ role }),
        });
        state.enemyTeam = draft.enemyTeam;
        state.allyTeam = draft.allyTeam;
        renderTeams('ally-team', 'enemy-team', /*includeUserSlot=*/true);
    } catch (e) {
        showError('Draft generation failed: ' + e.message);
    } finally {
        document.getElementById('draft-spinner').classList.add('d-none');
    }
}

// ---------- team rendering ----------

const ROLES = ['TOP', 'JGL', 'MID', 'ADC', 'SUPP'];

function renderTeams(allyId, enemyId, includeUserSlot) {
    document.getElementById(allyId).innerHTML = renderTeam(state.allyTeam, state.userRole, includeUserSlot, state.userChampion);
    document.getElementById(enemyId).innerHTML = renderTeam(state.enemyTeam, null, false, null);
}

function renderTeam(team, userRole, includeUserSlot, userChampion) {
    return ROLES.map(role => {
        if (userRole === role) {
            if (userChampion) {
                return pickCard({ role, championName: userChampion.name, iconUrl: userChampion.iconUrl });
            }
            if (includeUserSlot) return emptySlotCard(role);
            return '';
        }
        const pick = team.find(p => p.role === role);
        if (!pick) return emptySlotCard(role);
        return pickCard(pick);
    }).join('');
}

function pickCard(p) {
    const img = p.iconUrl
        ? `<img src="${p.iconUrl}" alt="${escapeHtml(p.championName)}"/>`
        : '';
    return `
        <div class="pick-card">
            <div class="role">${p.role}</div>
            <div class="icon-wrap">${img}</div>
            <div class="name">${escapeHtml(p.championName)}</div>
        </div>`;
}

function emptySlotCard(role) {
    return `
        <div class="pick-card empty">
            <div class="role">${role}</div>
            <div class="icon-wrap">?</div>
            <div class="name">Your pick</div>
        </div>`;
}

// ---------- champion autocomplete ----------

document.addEventListener('input', (ev) => {
    if (ev.target.id !== 'champ-input') return;
    const q = ev.target.value.trim().toLowerCase();
    const ul = document.getElementById('champ-suggestions');
    if (!q) { ul.innerHTML = ''; return; }
    const matches = state.champions
        .filter(c => c.name.toLowerCase().startsWith(q))
        .slice(0, 8);
    ul.innerHTML = matches.map(c => `
        <li class="list-group-item" data-id="${c.id}" data-name="${escapeHtml(c.name)}" data-icon="${c.iconUrl}">
            <img src="${c.iconUrl}" alt=""/> ${escapeHtml(c.name)}
        </li>
    `).join('');
});

document.addEventListener('click', (ev) => {
    const li = ev.target.closest('#champ-suggestions .list-group-item');
    if (!li) return;
    state.userChampion = {
        id: li.dataset.id,
        name: li.dataset.name,
        iconUrl: li.dataset.icon,
    };
    document.getElementById('champ-input').value = li.dataset.name;
    document.getElementById('champ-suggestions').innerHTML = '';
    document.getElementById('ask-coach').disabled = false;
    renderTeams('ally-team', 'enemy-team', /*includeUserSlot=*/true);
});

// ---------- view 2 -> view 3: ask coach ----------

async function onAskCoach() {
    if (!state.userChampion) return;
    document.getElementById('coach-spinner').classList.remove('d-none');
    show('coach');
    renderTeams('ally-team-final', 'enemy-team-final', /*includeUserSlot=*/true);

    try {
        const body = {
            userRole: state.userRole,
            userChampion: state.userChampion.name,
            allyTeam: state.allyTeam,
            enemyTeam: state.enemyTeam,
        };
        const r = await request(API + '/coach', {
            method: 'POST',
            body: JSON.stringify(body),
        });

        document.getElementById('coach-positives').innerHTML =
            (r.positives || []).map(s => `<li class="list-group-item">${escapeHtml(s)}</li>`).join('')
            || '<li class="list-group-item text-muted">(no items)</li>';
        document.getElementById('coach-negatives').innerHTML =
            (r.negatives || []).map(s => `<li class="list-group-item">${escapeHtml(s)}</li>`).join('')
            || '<li class="list-group-item text-muted">(no items)</li>';
        document.getElementById('coach-alternatives').innerHTML =
            (r.alternatives || []).map(a => `
                <div class="col-md-4">
                    <div class="alt-card">
                        ${a.iconUrl ? `<img src="${a.iconUrl}" alt=""/>` : ''}
                        <div class="alt-body">
                            <div class="alt-name">${escapeHtml(a.championName)}</div>
                            <div class="small">${escapeHtml(a.reason)}</div>
                        </div>
                    </div>
                </div>
            `).join('') || '<div class="text-muted">No alternatives suggested.</div>';
        document.getElementById('coach-datasource').textContent =
            r.dataSource === 'op.gg-scrape'
                ? 'op.gg counter scrape + ChatGPT reasoning'
                : 'ChatGPT reasoning only (op.gg data unavailable)';
    } catch (e) {
        showError('Coach failed: ' + e.message);
    } finally {
        document.getElementById('coach-spinner').classList.add('d-none');
    }
}

// ---------- helpers ----------

function escapeHtml(s) {
    return String(s ?? '').replace(/[&<>"']/g, c => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
}
