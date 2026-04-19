function initCoach() {
    document.getElementById('restart').addEventListener('click', function() {
        showView('home');
    });
}

function renderCoachView(isLoading, coachAnalysis) {
    document.getElementById('coach-spinner').style.display = isLoading ? 'block' : 'none';

    document.getElementById('ally-team-final').innerHTML =
        renderTeam(state.allyTeam, state.userRole, state.userChampion);
    document.getElementById('enemy-team-final').innerHTML =
        renderTeam(state.enemyTeam, null, null);

    if (isLoading || !coachAnalysis) {
        document.getElementById('coach-positives').innerHTML = '';
        document.getElementById('coach-negatives').innerHTML = '';
        document.getElementById('coach-alternatives').innerHTML = '';
        return;
    }

    document.getElementById('coach-positives').innerHTML = buildListHtml(coachAnalysis.positives);
    document.getElementById('coach-negatives').innerHTML = buildListHtml(coachAnalysis.negatives);
    document.getElementById('coach-alternatives').innerHTML = buildAlternativesHtml(coachAnalysis.alternatives);
}

function buildListHtml(items) {
    if (!items || items.length === 0) return '<li class="list-group-item text-muted">(no items)</li>';
    let html = '';
    for (const item of items) {
        html += `<li class="list-group-item">${escapeHtml(item)}</li>`;
    }
    return html;
}

function buildAlternativesHtml(alternatives) {
    if (!alternatives || alternatives.length === 0) return '<div class="text-muted">No alternatives suggested.</div>';
    let html = '';
    for (const alternative of alternatives) {
        const iconImage = alternative.iconUrl ? `<img src="${alternative.iconUrl}" alt=""/>` : '';
        html += `
            <div class="alt-card">
                ${iconImage}
                <div class="alt-body">
                    <div class="alt-name">${escapeHtml(alternative.championName)}</div>
                    <div class="alt-reason">${escapeHtml(alternative.reason)}</div>
                </div>
            </div>`;
    }
    return html;
}
