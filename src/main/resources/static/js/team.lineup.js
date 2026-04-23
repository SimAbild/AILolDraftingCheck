function buildTeamLineupHtml(team, userRole, userChampion) {
    let lineupHtml = '';
    for (const role of VALID_ROLES) {
        if (userRole === role) {
            lineupHtml += userChampion
                ? buildPickedChampionHtml({ role, name: userChampion.name })
                : buildEmptyPickHtml(role);
            continue;
        }
        const pick = team?.find(pick => pick.role === role);
        lineupHtml += pick ? buildPickedChampionHtml(pick) : buildEmptyPickHtml(role);
    }
    return lineupHtml;
}
