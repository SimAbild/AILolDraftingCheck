function initDraft() {
    document.getElementById('new-role').addEventListener('click', onNewRoleClicked);
    document.getElementById('ask-coach').addEventListener('click', onAskCoachClicked);
    document.getElementById('champ-input').addEventListener('input', onSearchInput);
    document.getElementById('champ-suggestions').addEventListener('click', onSuggestionClick);
}

function renderDraftSection(isLoading) {
    document.getElementById('user-role-label').textContent = currentDraft.userRole || '';
    document.getElementById('champ-input').value = '';
    document.getElementById('champ-suggestions').innerHTML = '';
    document.getElementById('ask-coach').disabled = true;
    document.getElementById('draft-spinner').style.display = isLoading ? 'block' : 'none';

    document.getElementById('ally-team').innerHTML =
        buildTeamLineupHtml(currentDraft.allyTeam, currentDraft.userRole, currentDraft.userChampion);
    document.getElementById('enemy-team').innerHTML =
        buildTeamLineupHtml(currentDraft.enemyTeam, null, null);
}

function onNewRoleClicked() {
    hideSection('section-draft');
    hideSection('section-coach');
    revealSection('section-home');
    currentDraft.userRole = null;
    currentDraft.userChampion = null;
    hideError();
}

async function onAskCoachClicked() {
    if (!currentDraft.userChampion) return;

    hideSection('section-draft');
    revealSection('section-coach');
    renderCoachSection(true, null);

    try {
        const coachRequest = {
            userRole: currentDraft.userRole,
            userChampion: currentDraft.userChampion.name,
            allyTeam: currentDraft.allyTeam,
            enemyTeam: currentDraft.enemyTeam
        };
        const coachAnalysis = await apiPostCoach(coachRequest);
        renderCoachSection(false, coachAnalysis);
    } catch (e) {
        showError('Coach failed: ' + e.message);
        renderCoachSection(false, null);
    }
}
