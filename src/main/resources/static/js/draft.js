function showDraftView(isLoading) {
    showView(buildDraftHtml(isLoading));
    attachDraftListeners();
}

function attachDraftListeners() {
    document.getElementById('new-role').addEventListener('click', onNewRoleClicked);
    document.getElementById('ask-coach').addEventListener('click', onAskCoachClicked);
    document.getElementById('champ-input').addEventListener('input', onSearchInput);
    document.getElementById('champ-suggestions').addEventListener('click', onSuggestionClick);
}

function buildDraftHtml(isLoading) {
    return `
        <div class="section-header">
            <span class="section-header__step">Step 2</span>
            <h2 class="section-header__title">Your draft</h2>
        </div>
        <div class="section-card">
            <p class="card-text">The AI has drafted your enemy and ally team.
                Pick your champion, then ask the coach.</p>
            ${isLoading ? buildSpinnerHtml() : ''}
            ${buildDraftBoardHtml()}
            ${buildChampionPickerHtml()}
            <div class="section-actions">
                <button type="button" id="new-role" class="btn-secondary">← Pick a different role</button>
                <button type="button" id="ask-coach" class="btn-primary" disabled>Ask the coach →</button>
            </div>
        </div>`;
}

function buildDraftBoardHtml() {
    const allyLineup = buildTeamLineupHtml(currentDraft.allyTeam, currentDraft.userRole, currentDraft.userChampion);
    const enemyLineup = buildTeamLineupHtml(currentDraft.enemyTeam, null, null);
    return `
        <div class="draft-board">
            <div id="ally-team" class="team-col">${allyLineup}</div>
            <div class="vs-label">VS</div>
            <div id="enemy-team" class="team-col">${enemyLineup}</div>
        </div>`;
}

function buildChampionPickerHtml() {
    return `
        <div class="champion-picker">
            <label for="champ-input">Your champion (<span id="user-role-label">${currentDraft.userRole || ''}</span>)</label>
            <input type="text" id="champ-input" class="text-input" autocomplete="off"
                   placeholder='Type a champion name, e.g. "Aatrox"'/>
            <ul id="champ-suggestions" class="suggestions-list"></ul>
        </div>`;
}

function onNewRoleClicked() {
    currentDraft.userRole = null;
    currentDraft.userChampion = null;
    currentDraft.allyTeam = [];
    currentDraft.enemyTeam = [];
    showHomeView();
}

async function onAskCoachClicked() {
    showCoachView(true, null);

    const coachRequest = {
        userRole: currentDraft.userRole,
        userChampion: currentDraft.userChampion.name,
        allyTeam: currentDraft.allyTeam,
        enemyTeam: currentDraft.enemyTeam
    };
    const coachAnalysis = await apiPostCoach(coachRequest);
    showCoachView(false, coachAnalysis);
}
