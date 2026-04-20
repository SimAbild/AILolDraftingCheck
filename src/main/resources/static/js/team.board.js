function buildTeamBoardHtml(team, userRole, userChampion) {
    let html = '';
    for (const role of VALID_ROLES) {
        if (userRole === role) {
            html += userChampion
                ? buildChampionSlotHtml({ role, championName: userChampion.name, iconUrl: userChampion.iconUrl })
                : buildEmptyChampionSlotHtml(role);
            continue;
        }
        const pick = team.find(teamPick => teamPick.role === role);
        html += pick ? buildChampionSlotHtml(pick) : buildEmptyChampionSlotHtml(role);
    }
    return html;
}
