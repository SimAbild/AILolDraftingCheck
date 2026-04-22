function onSearchInput(event) {
    const searchQuery = event.target.value.trim().toLowerCase();
    const matchingChampions = currentDraft.allChampions
        .filter(champion => champion.name.toLowerCase().startsWith(searchQuery))
        .slice(0, MAX_SUGGESTIONS);

    let suggestionsHtml = '';
    for (const champion of matchingChampions) {
        suggestionsHtml += `
            <li class="suggestion-item"
                data-id="${champion.id}"
                data-name="${champion.name}"
                data-icon="${champion.iconUrl}">
                <img src="${champion.iconUrl}" alt=""/> ${champion.name}
            </li>`;
    }
    document.getElementById('champ-suggestions').innerHTML = suggestionsHtml;
}

function onSuggestionClick(event) {
    const selectedSuggestion = event.target.closest('li');

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
