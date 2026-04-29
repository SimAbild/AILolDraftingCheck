const coachFeedback = {
    buildHtml(coachAnalysis) {
        return `
        <div class="feedback-grid">
            <div>
                <h6>What works</h6>
                <ul class="feedback-list">${coachFeedback.buildFeedbackListHtml(coachAnalysis.positives)}</ul>
            </div>
            <div>
                <h6>What doesn't work</h6>
                <ul class="feedback-list">${coachFeedback.buildFeedbackListHtml(coachAnalysis.negatives)}</ul>
            </div>
        </div>
        <h6 class="alternatives-title">Stronger alternative champions</h6>
        <div>${coachFeedback.buildAlternativesHtml(coachAnalysis.alternatives)}</div>`;
    },

    buildFeedbackListHtml(feedbackItems) {
        let feedbackHtml = '';
        for (const feedbackItem of feedbackItems) {
            feedbackHtml += `<li class="feedback-list__item">${feedbackItem}</li>`;
        }
        return feedbackHtml;
    },

    buildAlternativesHtml(alternatives) {
        let alternativesHtml = '';
        for (const alternative of alternatives) {
            const winrateHtml = alternative.winrate && alternative.winrate > 0
                ? `<div class="alt-card__winrate">${alternative.winrate}% winrate</div>`
                : '';
            alternativesHtml += `
            <div class="alt-card">
                ${draftChampionPlaceholder.findChampionIconHtml(alternative.name)}
                <div class="alt-card__body">
                    <div class="alt-card__name">${alternative.name}</div>
                    ${winrateHtml}
                    <div class="alt-card__reason">${alternative.reason}</div>
                    ${coachFeedback.buildStrengthBulletsHtml(alternative.strengths)}
                </div>
            </div>`;
        }
        return alternativesHtml;
    },

    buildStrengthBulletsHtml(strengths) {
        let strengthsHtml = '<ul class="alt-card__strengths">';
        for (const strength of strengths) {
            strengthsHtml += `<li>${strength}</li>`;
        }
        strengthsHtml += '</ul>';
        return strengthsHtml;
    }
};