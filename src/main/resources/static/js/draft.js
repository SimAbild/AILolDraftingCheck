const MAX_SUGGESTIONS = 8;

function initDraft() {
    document.getElementById('back-home').addEventListener('click', function() {
        showView('home');
    });
    document.getElementById('ask-coach').addEventListener('click', askCoach);
    document.getElementById('champ-input').addEventListener('input', onSearchInput);
    document.getElementById('champ-suggestions').addEventListener('click', onSuggestionClick);
}

function renderDraftView(isLoading) {
    document.getElementById('user-role-label').textContent = state.userRole || '';
    document.getElementById('champ-input').value = '';
    document.getElementById('champ-suggestions').innerHTML = '';
    document.getElementById('ask-coach').disabled = true;

    document.getElementById('draft-spinner').style.display = isLoading ? 'block' : 'none';

    document.getElementById('ally-team').innerHTML =
        renderTeam(state.allyTeam, state.userRole, state.userChampion);
    document.getElementById('enemy-team').innerHTML =
        renderTeam(state.enemyTeam, null, null);
}

function onSearchInput(ev) {
    const searchQuery = ev.target.value.trim().toLowerCase();
    const suggestionsList = document.getElementById('champ-suggestions');

    if (!searchQuery) {
        suggestionsList.innerHTML = '';
        return;
    }

    const matchingChampions = state.champions
        .filter(champion => champion.name.toLowerCase().startsWith(searchQuery))
        .slice(0, MAX_SUGGESTIONS);

    let html = '';
    for (const champion of matchingChampions) {
        html += `
            <li class="list-group-item" data-id="${champion.id}" data-name="${escapeHtml(champion.name)}" data-icon="${champion.iconUrl}">
                <img src="${champion.iconUrl}" alt=""/> ${escapeHtml(champion.name)}
            </li>`;
    }
    suggestionsList.innerHTML = html;
}

function onSuggestionClick(ev) {
    const selectedSuggestion = ev.target.closest('li');
    if (!selectedSuggestion) return;

    state.userChampion = {
        id: selectedSuggestion.dataset.id,
        name: selectedSuggestion.dataset.name,
        iconUrl: selectedSuggestion.dataset.icon
    };

    document.getElementById('champ-input').value = selectedSuggestion.dataset.name;
    document.getElementById('champ-suggestions').innerHTML = '';
    document.getElementById('ask-coach').disabled = false;

    document.getElementById('ally-team').innerHTML =
        renderTeam(state.allyTeam, state.userRole, state.userChampion);
}

async function askCoach() {
    if (!state.userChampion) return;
    showView('coach');
    renderCoachView(true, null);

    try {
        const coachRequest = {
            userRole: state.userRole,
            userChampion: state.userChampion.name,
            allyTeam: state.allyTeam,
            enemyTeam: state.enemyTeam
        };
        const coachAnalysis = await apiPostCoach(coachRequest);
        renderCoachView(false, coachAnalysis);
    } catch (e) {
        showError('Coach failed: ' + e.message);
        renderCoachView(false, null);
    }
}
