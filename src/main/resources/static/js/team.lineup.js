const teamLineup = {
    buildHtml(team, userRole, userChampion) {
        let lineupHtml = '';
        for (const role of VALID_ROLES) {
            if (userRole === role) {
                lineupHtml += userChampion
                    ? championPick.buildPickedHtml({ role, name: userChampion.name })
                    : championPick.buildEmptyHtml(role);
                continue;
            }
            const pick = team?.find(pick => pick.role === role);
            lineupHtml += pick ? championPick.buildPickedHtml(pick) : championPick.buildEmptyHtml(role);
        }
        return lineupHtml;
    },

    buildBoardHtml() {
        const allyLineup = teamLineup.buildHtml(draftChampionPlaceholder.allyTeam, draftChampionPlaceholder.userRole, draftChampionPlaceholder.userChampion);
        const enemyLineup = teamLineup.buildHtml(draftChampionPlaceholder.enemyTeam, null, null);
        return `
        <div class="draft-board">
            <div id="ally-team" class="team-col">${allyLineup}</div>
            <div class="vs-label">VS</div>
            <div id="enemy-team" class="team-col">${enemyLineup}</div>
        </div>`;
    }
};