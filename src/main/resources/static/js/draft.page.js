const draftPage = {
    show(callbacks) {
        viewRenderer.show(draftPage.buildHtml());
        draftPage.attachListeners(callbacks);
    },

    attachListeners(callbacks) {
        document.getElementById('new-role').addEventListener('click', callbacks.onNewRole);
        document.getElementById('ask-coach').addEventListener('click', callbacks.onAskCoach);
        document.getElementById('champ-input').addEventListener('input', championSearch.onInput);
        document.getElementById('champ-suggestions').addEventListener('click', (event) => {
            championSearch.onSuggestionPicked(event);
            draftPage.onChampionPicked();
        });
    },

    onChampionPicked() {
        document.getElementById('ask-coach').disabled = false;
        document.getElementById('ally-team').innerHTML =
            teamLineup.buildHtml(draftChampionPlaceholder.allyTeam, draftChampionPlaceholder.userRole, draftChampionPlaceholder.userChampion);
    },

    buildHtml() {
        return `
        <div class="section-header">
            <span class="section-header__step">Step 2</span>
            <h2 class="section-header__title">Your draft</h2>
        </div>
        <div class="section-card">
            <p class="card-text">The AI has drafted your enemy and ally team.
                Pick your champion, then ask the coach.</p>
            ${draftPage.buildBoardHtml()}
            ${draftPage.buildChampionPickerHtml()}
            <div class="section-actions">
                <button type="button" id="new-role" class="btn-secondary">← Pick a different role</button>
                <button type="button" id="ask-coach" class="btn-primary" disabled>Ask the coach →</button>
            </div>
        </div>`;
    },

    buildBoardHtml() {
        return `
        <div id="board-spinner">${viewRenderer.buildSpinnerHtml()}</div>
        ${teamLineup.buildBoardHtml()}`;
    },

    updateTeams() {
        document.getElementById('board-spinner').remove();
        document.getElementById('ally-team').innerHTML =
            teamLineup.buildHtml(draftChampionPlaceholder.allyTeam, draftChampionPlaceholder.userRole, draftChampionPlaceholder.userChampion);
        document.getElementById('enemy-team').innerHTML =
            teamLineup.buildHtml(draftChampionPlaceholder.enemyTeam, null, null);
    },

    buildChampionPickerHtml() {
        return `
        <div class="champion-picker">
            <label for="champ-input">Your champion (<span id="user-role-label">${draftChampionPlaceholder.userRole || ''}</span>)</label>
            <input type="text" id="champ-input" class="text-input" autocomplete="off"
                   placeholder='Type a champion name, e.g. "Aatrox"'/>
            <ul id="champ-suggestions" class="suggestions-list"></ul>
        </div>`;
    }
};