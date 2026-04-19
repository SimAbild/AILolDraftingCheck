document.addEventListener('DOMContentLoaded', async function() {
    initHome();
    initDraft();
    initCoach();
    showView('home');

    try {
        state.champions = await apiGetChampions();
    } catch (e) {
        showError('Could not load champion list: ' + e.message);
    }
});
