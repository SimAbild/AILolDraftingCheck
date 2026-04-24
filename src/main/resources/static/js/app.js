document.addEventListener('DOMContentLoaded', async function() {
    draftChampionPlaceholder.allChampions = await api.getChampions();
    goToRolePickerStep();
});

function goToRolePickerStep() {
    draftChampionPlaceholder.userRole = null;
    draftChampionPlaceholder.userChampion = null;
    draftChampionPlaceholder.allyTeam = [];
    draftChampionPlaceholder.enemyTeam = [];
    rolePicker.show({ onRolePicked: goToDraftStep });
}

async function goToDraftStep(role) {
    draftChampionPlaceholder.userRole = role;
    draftChampionPlaceholder.userChampion = null;

    draftPage.show({ onNewRole: goToRolePickerStep, onAskCoach: goToCoachStep });

    const draft = await api.postDraft(role);
    draftChampionPlaceholder.enemyTeam = draft.enemy;
    draftChampionPlaceholder.allyTeam = draft.ally;

    draftPage.updateTeams();
}

async function goToCoachStep() {
    coachPage.show({ onNewDraft: goToRolePickerStep });

    const coachRequest = {
        userRole: draftChampionPlaceholder.userRole,
        userChampion: draftChampionPlaceholder.userChampion.name,
        allyTeam: draftChampionPlaceholder.allyTeam,
        enemyTeam: draftChampionPlaceholder.enemyTeam
    };
    const coachAnalysis = await api.postCoach(coachRequest);

    coachPage.updateFeedback(coachAnalysis);
}