function buildPickedChampionHtml(pick) {
    const champion = currentDraft.allChampions.find(c => c.name.toLowerCase() === pick.name?.toLowerCase());
    const iconImage = champion
        ? `<img src="${champion.iconUrl}" alt="${pick.name}"/>`
        : '';
    return `
        <div class="pick-card">
            <div class="pick-card__role">${pick.role}</div>
            <div class="pick-card__icon">${iconImage}</div>
            <div class="pick-card__name">${pick.name}</div>
        </div>`;
}

function buildEmptyPickHtml(role) {
    return `
        <div class="pick-card pick-card--empty">
            <div class="pick-card__role">${role}</div>
            <div class="pick-card__icon">?</div>
            <div class="pick-card__name">Your pick</div>
        </div>`;
}
