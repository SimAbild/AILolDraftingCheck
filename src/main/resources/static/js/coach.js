// Coach view: shows the full draft one more time plus the AI's
// positives / negatives / alternative champion suggestions.

function initCoach() {
    document.getElementById('restart').addEventListener('click', function() {
        showView('home');
    });
}

function renderCoachView(loading, result) {
    document.getElementById('coach-spinner').style.display = loading ? 'block' : 'none';

    document.getElementById('ally-team-final').innerHTML =
        renderTeam(state.allyTeam, state.userRole, state.userChampion);
    document.getElementById('enemy-team-final').innerHTML =
        renderTeam(state.enemyTeam, null, null);

    if (loading || !result) {
        document.getElementById('coach-positives').innerHTML = '';
        document.getElementById('coach-negatives').innerHTML = '';
        document.getElementById('coach-alternatives').innerHTML = '';
        return;
    }

    document.getElementById('coach-positives').innerHTML = asList(result.positives);
    document.getElementById('coach-negatives').innerHTML = asList(result.negatives);
    document.getElementById('coach-alternatives').innerHTML = asAlternatives(result.alternatives);
}

function asList(items) {
    if (!items || items.length === 0) return '<li class="list-group-item text-muted">(no items)</li>';
    let html = '';
    for (let i = 0; i < items.length; i++) {
        html += `<li class="list-group-item">${escapeHtml(items[i])}</li>`;
    }
    return html;
}

function asAlternatives(alts) {
    if (!alts || alts.length === 0) return '<div class="text-muted">No alternatives suggested.</div>';
    let html = '';
    for (let i = 0; i < alts.length; i++) {
        const a = alts[i];
        const img = a.iconUrl ? `<img src="${a.iconUrl}" alt=""/>` : '';
        html += `
            <div class="alt-card">
                ${img}
                <div class="alt-body">
                    <div class="alt-name">${escapeHtml(a.championName)}</div>
                    <div class="alt-reason">${escapeHtml(a.reason)}</div>
                </div>
            </div>`;
    }
    return html;
}
