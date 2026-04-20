document.addEventListener('DOMContentLoaded', async function() {
    initRolePicker();
    initDraft();
    initCoach();

    try {
        currentDraft.allChampions = await apiGetChampions();
    } catch (e) {
        showError('Could not load champion list: ' + e.message);
    }
});
