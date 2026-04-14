// Entry point. Loads the champion list and wires up the three views.

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
