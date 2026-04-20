function initRolePicker() {
    const roleButtons = document.querySelectorAll('.role-btn');
    for (const roleButton of roleButtons) {
        roleButton.addEventListener('click', function() {
            onRolePicked(this.dataset.role);
        });
    }
}

async function onRolePicked(role) {
    currentDraft.userRole = role;
    currentDraft.userChampion = null;
    hideError();

    hideSection('section-home');
    revealSection('section-draft');
    renderDraftSection(true);

    try {
        const draft = await apiPostDraft(role);
        currentDraft.enemyTeam = draft.enemyTeam;
        currentDraft.allyTeam = draft.allyTeam;
        renderDraftSection(false);
    } catch (e) {
        showError('Draft generation failed: ' + e.message);
        renderDraftSection(false);
    }
}
