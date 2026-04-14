// Home view: click one of the role buttons on the rift map to trigger a draft.

function initHome() {
    const buttons = document.querySelectorAll('.role-btn');
    for (let i = 0; i < buttons.length; i++) {
        buttons[i].addEventListener('click', function() {
            onRolePicked(this.dataset.role);
        });
    }
}

async function onRolePicked(role) {
    state.userRole = role;
    state.userChampion = null;
    showView('draft');
    renderDraftView(true); // loading = true

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
