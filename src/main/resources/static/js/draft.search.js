function onSearchInput(ev) {
    const searchQuery = ev.target.value.trim().toLowerCase();
    const suggestionsList = document.getElementById('champ-suggestions');

    if (!searchQuery) {
        suggestionsList.innerHTML = '';
        return;
    }

    const matchingChampions = currentDraft.allChampions
        .filter(champion => champion.name.toLowerCase().startsWith(searchQuery))
        .slice(0, MAX_SUGGESTIONS);

    let suggestionsHtml = '';
    for (const champion of matchingChampions) {
        suggestionsHtml += `
            <li class="suggestion-item list-group-item"
                data-id="${champion.id}"
                data-name="${champion.name}"
                data-icon="${champion.iconUrl}">
                <img src="${champion.iconUrl}" alt=""/> ${champion.name}
            </li>`;
    }
    suggestionsList.innerHTML = suggestionsHtml;
}

function onSuggestionClick(ev) {
    const selectedSuggestion = ev.target.closest('li');
    if (!selectedSuggestion) return;

    currentDraft.userChampion = {
        id: selectedSuggestion.dataset.id,
        name: selectedSuggestion.dataset.name,
        iconUrl: selectedSuggestion.dataset.icon
    };

    document.getElementById('champ-input').value = selectedSuggestion.dataset.name;
    document.getElementById('champ-suggestions').innerHTML = '';
    document.getElementById('ask-coach').disabled = false;

    document.getElementById('ally-team').innerHTML =
        buildTeamLineupHtml(currentDraft.allyTeam, currentDraft.userRole, currentDraft.userChampion);
}
