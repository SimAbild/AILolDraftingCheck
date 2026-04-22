function buildFeedbackListHtml(feedbackItems) {
    let feedbackHtml = '';
    for (const feedbackItem of feedbackItems) {
        feedbackHtml += `<li class="feedback-list__item">${feedbackItem}</li>`;
    }
    return feedbackHtml;
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
    let strengthsHtml = '<ul class="alt-card__strengths">';
    for (const strength of strengths) {
        strengthsHtml += `<li>${strength}</li>`;
    }
    strengthsHtml += '</ul>';
    return strengthsHtml;
}
