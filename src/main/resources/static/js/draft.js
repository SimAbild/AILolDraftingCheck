// Draft view: show the AI-generated teams, let the user pick their champion
// via autocomplete, then jump to the coach view.

function initDraft() {
    document.getElementById('back-home').addEventListener('click', function() {
        showView('home');
    });
    document.getElementById('ask-coach').addEventListener('click', askCoach);
    document.getElementById('champ-input').addEventListener('input', onSearchInput);
    document.getElementById('champ-suggestions').addEventListener('click', onSuggestionClick);
}

// Draw / redraw the entire draft view.
function renderDraftView(loading) {
    document.getElementById('user-role-label').textContent = state.userRole || '';
    document.getElementById('champ-input').value = '';
    document.getElementById('champ-suggestions').innerHTML = '';
    document.getElementById('ask-coach').disabled = true;

    document.getElementById('draft-spinner').style.display = loading ? 'block' : 'none';

    document.getElementById('ally-team').innerHTML =
        renderTeam(state.allyTeam, state.userRole, state.userChampion);
    document.getElementById('enemy-team').innerHTML =
        renderTeam(state.enemyTeam, null, null);
}

function onSearchInput(ev) {
    const query = ev.target.value.trim().toLowerCase();
    const ul = document.getElementById('champ-suggestions');
    if (!query) { ul.innerHTML = ''; return; }

    const matches = state.champions
        .filter(c => c.name.toLowerCase().indexOf(query) === 0)
        .slice(0, 8);

    let html = '';
    for (let i = 0; i < matches.length; i++) {
        const c = matches[i];
        html += `
            <li class="list-group-item" data-id="${c.id}" data-name="${escapeHtml(c.name)}" data-icon="${c.iconUrl}">
                <img src="${c.iconUrl}" alt=""/> ${escapeHtml(c.name)}
            </li>`;
    }
    ul.innerHTML = html;
}

function onSuggestionClick(ev) {
    const li = ev.target.closest('li');
    if (!li) return;
    state.userChampion = {
        id: li.dataset.id,
        name: li.dataset.name,
        iconUrl: li.dataset.icon
    };
    document.getElementById('champ-input').value = li.dataset.name;
    document.getElementById('champ-suggestions').innerHTML = '';
    document.getElementById('ask-coach').disabled = false;

    // Redraw so the user's chosen champion appears in their ally slot.
    document.getElementById('ally-team').innerHTML =
        renderTeam(state.allyTeam, state.userRole, state.userChampion);
}

async function askCoach() {
    if (!state.userChampion) return;
    showView('coach');
    renderCoachView(true, null); // loading = true

    try {
        const payload = {
            userRole: state.userRole,
            userChampion: state.userChampion.name,
            allyTeam: state.allyTeam,
            enemyTeam: state.enemyTeam
        };
        const result = await apiPostCoach(payload);
        renderCoachView(false, result);
    } catch (e) {
        showError('Coach failed: ' + e.message);
        renderCoachView(false, null);
    }
}
