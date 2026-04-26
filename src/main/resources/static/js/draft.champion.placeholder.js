const draftChampionPlaceholder = {
    allChampions: [],
    userRole: null,
    enemyTeam: [],
    allyTeam: [],
    userChampion: null,

    findChampionIconHtml(name) {
        const champion = draftChampionPlaceholder.allChampions.find(c => c.name.toLowerCase() === name?.toLowerCase());
        return champion ? `<img src="${champion.iconUrl}" alt="${name}"/>` : '';
    }
};
