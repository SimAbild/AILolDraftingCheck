function showCoachView(isLoading, coachAnalysis) {
    showView(buildCoachHtml(isLoading, coachAnalysis));
    document.getElementById('start-new-draft').addEventListener('click', onStartNewDraftClicked);
}

function buildCoachHtml(isLoading, coachAnalysis) {
    return `
        <div class="section-header">
            <span class="section-header__step">Step 3</span>
            <h2 class="section-header__title">Coach feedback</h2>
        </div>
        <div class="section-card">
            ${isLoading ? buildSpinnerHtml() : ''}
            ${buildCoachDraftBoardHtml()}
            ${!isLoading ? buildCoachFeedbackHtml(coachAnalysis) : ''}
            <div class="section-actions">
                <button type="button" id="start-new-draft" class="btn-secondary">← Start a new draft</button>
            </div>
        </div>`;
}

function buildCoachDraftBoardHtml() {
    const allyLineup = buildTeamLineupHtml(currentDraft.allyTeam, currentDraft.userRole, currentDraft.userChampion);
    const enemyLineup = buildTeamLineupHtml(currentDraft.enemyTeam, null, null);
    return `
        <div class="draft-board">
            <div id="ally-team-final" class="team-col">${allyLineup}</div>
            <div class="vs-label">VS</div>
            <div id="enemy-team-final" class="team-col">${enemyLineup}</div>
        </div>`;
}

function buildCoachFeedbackHtml(coachAnalysis) {
    return `
        <div class="feedback-grid">
            <div>
                <h6>What works</h6>
                <ul class="feedback-list">${buildFeedbackListHtml(coachAnalysis.positives)}</ul>
            </div>
            <div>
                <h6>What doesn't work</h6>
                <ul class="feedback-list">${buildFeedbackListHtml(coachAnalysis.negatives)}</ul>
            </div>
        </div>
        <h6 class="alternatives-title">Stronger alternative champions</h6>
        <div>${buildAlternativesHtml(coachAnalysis.alternatives)}</div>`;
}

function onStartNewDraftClicked() {
    currentDraft.userRole = null;
    currentDraft.userChampion = null;
    currentDraft.allyTeam = [];
    currentDraft.enemyTeam = [];
    showHomeView();
}
