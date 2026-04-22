function buildFeedbackListHtml(items) {
    let listHtml = '';
    for (const item of items) {
        listHtml += `<li class="feedback-list__item">${item}</li>`;
    }
    return listHtml;
}

function buildAlternativesHtml(alternatives) {
    let alternativesHtml = '';
    for (const alternative of alternatives) {
        const iconImage = alternative.iconUrl
            ? `<img src="${alternative.iconUrl}" alt=""/>`
            : '';
        alternativesHtml += `
            <div class="alt-card">
                ${iconImage}
                <div class="alt-card__body">
                    <div class="alt-card__name">${alternative.championName}</div>
                    <div class="alt-card__reason">${alternative.reason}</div>
                    ${buildStrengthBulletsHtml(alternative.strengths)}
                </div>
            </div>`;
    }
    return alternativesHtml;
}

function buildStrengthBulletsHtml(strengths) {
    let bulletsHtml = '<ul class="alt-card__strengths">';
    for (const strength of strengths) {
        bulletsHtml += `<li>${strength}</li>`;
    }
    bulletsHtml += '</ul>';
    return bulletsHtml;
}
