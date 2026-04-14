// Draws a team (5 slots) as HTML. Used by both the draft and coach views.

function renderTeam(team, userRole, userChampion) {
    let html = '';
    for (let i = 0; i < ROLES.length; i++) {
        const role = ROLES[i];

        // The user's own slot - either empty placeholder or their chosen champion.
        if (userRole === role) {
            if (userChampion) {
                html += pickCard({
                    role: role,
                    championName: userChampion.name,
                    iconUrl: userChampion.iconUrl
                });
            } else {
                html += emptyCard(role);
            }
            continue;
        }

        // Everyone else.
        const pick = team.find(p => p.role === role);
        html += pick ? pickCard(pick) : emptyCard(role);
    }
    return html;
}

function pickCard(p) {
    const img = p.iconUrl
        ? `<img src="${p.iconUrl}" alt="${escapeHtml(p.championName)}"/>`
        : '';
    return `
        <div class="pick-card">
            <div class="role">${p.role}</div>
            <div class="icon-wrap">${img}</div>
            <div class="name">${escapeHtml(p.championName)}</div>
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
