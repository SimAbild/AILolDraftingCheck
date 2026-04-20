function buildChampionSlotHtml(pick) {
    const iconImage = pick.iconUrl
        ? `<img src="${pick.iconUrl}" alt="${pick.championName}"/>`
        : '';
    return `
        <div class="pick-card">
            <div class="pick-card__role">${pick.role}</div>
            <div class="pick-card__icon">${iconImage}</div>
            <div class="pick-card__name">${pick.championName}</div>
        </div>`;
}

function buildEmptyChampionSlotHtml(role) {
    return `
        <div class="pick-card pick-card--empty">
            <div class="pick-card__role">${role}</div>
            <div class="pick-card__icon">?</div>
            <div class="pick-card__name">Your pick</div>
        </div>`;
}
