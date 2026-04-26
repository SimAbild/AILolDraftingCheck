const championSearch = {
    onInput(event) {
        const searchQuery = event.target.value.trim().toLowerCase();
        const matchingChampions = draftChampionPlaceholder.allChampions
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
    },

    onSuggestionPicked(event) {
        const selectedSuggestion = event.target.closest('li');

        draftChampionPlaceholder.userChampion = {
            id: selectedSuggestion.dataset.id,
            name: selectedSuggestion.dataset.name,
            iconUrl: selectedSuggestion.dataset.icon
        };

        document.getElementById('champ-input').value = selectedSuggestion.dataset.name;
        document.getElementById('champ-suggestions').innerHTML = '';
    }
};