function renderTeam(team, userRole, userChampion) {
    let html = '';
    for (const role of VALID_ROLES) {
        if (userRole === role) {
            html += userChampion
                ? pickCard({ role, championName: userChampion.name, iconUrl: userChampion.iconUrl })
                : emptyCard(role);
            continue;
        }

        const pick = team.find(teamPick => teamPick.role === role);
        html += pick ? pickCard(pick) : emptyCard(role);
    }
    return html;
}

function pickCard(pick) {
    const iconImage = pick.iconUrl
        ? `<img src="${pick.iconUrl}" alt="${escapeHtml(pick.championName)}"/>`
        : '';
    return `
        <div class="pick-card">
            <div class="role">${pick.role}</div>
            <div class="icon-wrap">${iconImage}</div>
            <div class="name">${escapeHtml(pick.championName)}</div>
        </div>`;
}

function emptyCard(role) {
    return `
        <div class="pick-card empty">
            <div class="role">${role}</div>
            <div class="icon-wrap">?</div>
            <div class="name">Your pick</div>
        </div>`;
}
