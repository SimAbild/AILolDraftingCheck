function initHome() {
    const roleButtons = document.querySelectorAll('.role-btn');
    for (const roleButton of roleButtons) {
        roleButton.addEventListener('click', function() {
            onRolePicked(this.dataset.role);
        });
    }
}

async function onRolePicked(role) {
    state.userRole = role;
    state.userChampion = null;
    showView('draft');
    renderDraftView(true);

    try {
        const draft = await apiPostDraft(role);
        state.enemyTeam = draft.enemyTeam;
        state.allyTeam = draft.allyTeam;
        renderDraftView(false);
    } catch (e) {
        showError('Draft generation failed: ' + e.message);
        renderDraftView(false);
    }
}
