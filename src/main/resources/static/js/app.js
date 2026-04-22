document.addEventListener('DOMContentLoaded', async function() {
    showHomeView();
    currentDraft.allChampions = await apiGetChampions();
});
