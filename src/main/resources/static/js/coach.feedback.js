const coachFeedback = {
    buildHtml(coachAnalysis) {
        const wr = coachAnalysis.matchupWinrate;
        const winrateIsPositive = wr && wr >= 50;
        const winrateIsNegative = wr && wr < 50;

        const scoreHtml = coachAnalysis.score != null
            ? `<div class="coach-score ${coachFeedback.getScoreClass(coachAnalysis.score)}">
                   <span class="coach-score__number">${coachAnalysis.score}</span>
                   <span class="coach-score__label">/ 100</span>
               </div>`
            : '';

        return `
        ${scoreHtml}
        <div class="feedback-grid">
            <div>
                <h6>What works</h6>
                <ul class="feedback-list">${coachFeedback.buildListWithWinrate(coachAnalysis.positives, winrateIsPositive ? wr : null)}</ul>
            </div>
            <div>
                <h6>What doesn't work</h6>
                <ul class="feedback-list">${coachFeedback.buildListWithWinrate(coachAnalysis.negatives, winrateIsNegative ? wr : null)}</ul>
            </div>
        </div>
        <h6 class="alternatives-title">Stronger alternative champions</h6>
        <div>${coachFeedback.buildAlternativesHtml(coachAnalysis.alternatives)}</div>`;
    },

    // Bygger feedback-liste med winrate-badge i den første boks (hvis winrate er givet).
    buildListWithWinrate(items, winrate) {
        let html = '';
        for (let i = 0; i < items.length; i++) {
            if (i === 0 && winrate) {
                html += `<li class="feedback-list__item">
                    <span class="matchup-winrate-badge ${coachFeedback.getWinrateClass(winrate)}">${winrate}%</span>
                    ${items[i]}
                </li>`;
            } else {
                html += `<li class="feedback-list__item">${items[i]}</li>`;
            }
        }
        return html;
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

    getScoreClass(score) {
        if (score <= 40) return 'coach-score--low';
        if (score <= 70) return 'coach-score--mid';
        return 'coach-score--high';
    },

    getWinrateClass(winrate) {
        if (winrate < 48) return 'winrate--low';
        if (winrate < 52) return 'winrate--mid';
        return 'winrate--high';
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
