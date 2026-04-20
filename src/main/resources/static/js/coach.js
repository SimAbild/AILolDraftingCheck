function initCoach() {
    document.getElementById('start-new-draft').addEventListener('click', onStartNewDraftClicked);
}

function renderCoachSection(isLoading, coachAnalysis) {
    document.getElementById('coach-spinner').style.display = isLoading ? 'block' : 'none';

    document.getElementById('ally-team-final').innerHTML =
        buildTeamLineupHtml(currentDraft.allyTeam, currentDraft.userRole, currentDraft.userChampion);
    document.getElementById('enemy-team-final').innerHTML =
        buildTeamLineupHtml(currentDraft.enemyTeam, null, null);

    if (isLoading || !coachAnalysis) {
        document.getElementById('coach-positives').innerHTML = '';
        document.getElementById('coach-negatives').innerHTML = '';
        document.getElementById('coach-alternatives').innerHTML = '';
        return;
    }

    document.getElementById('coach-positives').innerHTML =
        buildFeedbackListHtml(coachAnalysis.positives);
    document.getElementById('coach-negatives').innerHTML =
        buildFeedbackListHtml(coachAnalysis.negatives);
    document.getElementById('coach-alternatives').innerHTML =
        buildAlternativesHtml(coachAnalysis.alternatives);
}

function onStartNewDraftClicked() {
    hideSection('section-coach');
    revealSection('section-home');
    currentDraft.userRole = null;
    currentDraft.userChampion = null;
    hideError();
}
