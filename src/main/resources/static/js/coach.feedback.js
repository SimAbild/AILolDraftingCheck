function buildFeedbackListHtml(items) {
    if (!items || items.length === 0) {
        return '<li class="list-group-item text-muted">(no items)</li>';
    }
    let listHtml = '';
    for (const item of items) {
        listHtml += `<li class="list-group-item">${item}</li>`;
    }
    return listHtml;
}

function buildAlternativesHtml(alternatives) {
    if (!alternatives || alternatives.length === 0) {
        return '<div class="text-muted">No alternatives suggested.</div>';
    }
    let alternativesHtml = '';
    for (const alternative of alternatives) {
        const iconImage = alternative.iconUrl
            ? `<img src="${alternative.iconUrl}" alt=""/>`
            : '';
        const strengthBullets = buildStrengthBulletsHtml(alternative.strengths);
        alternativesHtml += `
            <div class="alt-card">
                ${iconImage}
                <div class="alt-card__body">
                    <div class="alt-card__name">${alternative.championName}</div>
                    <div class="alt-card__reason">${alternative.reason}</div>
                    ${strengthBullets}
                </div>
            </div>`;
    }
    return alternativesHtml;
}

function buildStrengthBulletsHtml(strengths) {
    if (!strengths || strengths.length === 0) return '';
    let bulletsHtml = '<ul class="alt-card__strengths">';
    for (const strength of strengths) {
        bulletsHtml += `<li>${strength}</li>`;
    }
    bulletsHtml += '</ul>';
    return bulletsHtml;
}
