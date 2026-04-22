function showHomeView() {
    showView(buildHomeHtml());
    attachRoleButtonListeners();
}

function attachRoleButtonListeners() {
    const roleButtons = document.querySelectorAll('.role-btn');
    for (const roleButton of roleButtons) {
        roleButton.addEventListener('click', function() {
            onRolePicked(this.dataset.role);
        });
    }
}

function buildHomeHtml() {
    return `
        <div class="section-header">
            <span class="section-header__step">Step 1</span>
            <h2 class="section-header__title">Pick your role</h2>
        </div>
        <div class="section-card">
            <p class="card-text">Click the role you play on the map below.</p>
            <div id="rift-map">
                <button type="button" class="role-btn role-top"  data-role="TOP">TOP</button>
                <button type="button" class="role-btn role-jgl"  data-role="JGL">JGL</button>
                <button type="button" class="role-btn role-mid"  data-role="MID">MID</button>
                <button type="button" class="role-btn role-adc"  data-role="ADC">ADC</button>
                <button type="button" class="role-btn role-supp" data-role="SUPP">SUPP</button>
            </div>
        </div>`;
}

async function onRolePicked(role) {
    currentDraft.userRole = role;
    currentDraft.userChampion = null;

    showDraftView(true);

    const draft = await apiPostDraft(role);
    currentDraft.enemyTeam = draft.enemyTeam;
    currentDraft.allyTeam = draft.allyTeam;
    showDraftView(false);
}
