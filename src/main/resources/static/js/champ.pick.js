const championPick = {
    buildPickedHtml(pick) {
        return `
        <div class="pick-card">
            <div class="pick-card__role">${pick.role}</div>
            <div class="pick-card__icon">${draftChampionPlaceholder.findChampionIconHtml(pick.name)}</div>
            <div class="pick-card__name">${pick.name}</div>
        </div>`;
    },

    buildEmptyHtml(role) {
        return `
        <div class="pick-card pick-card--empty">
            <div class="pick-card__role">${role}</div>
            <div class="pick-card__icon">?</div>
            <div class="pick-card__name">Your pick</div>
        </div>`;
    }
};