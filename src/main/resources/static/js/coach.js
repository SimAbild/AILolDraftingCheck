const coachPage = {
    show(callbacks) {
        viewRenderer.show(coachPage.buildHtml());
        document.getElementById('start-new-draft').addEventListener('click', callbacks.onNewDraft);
    },

    buildHtml() {
        return `
        <div class="section-header">
            <span class="section-header__step">Step 3</span>
            <h2 class="section-header__title">Coach feedback</h2>
        </div>
        <div class="section-card">
            ${teamLineup.buildBoardHtml()}
            <div id="feedback-spinner">${viewRenderer.buildSpinnerHtml()}</div>
            <div id="feedback-content"></div>
            <div class="section-actions">
                <button type="button" id="start-new-draft" class="btn-secondary">← Start a new draft</button>
            </div>
        </div>`;
    },

    updateFeedback(coachAnalysis) {
        document.getElementById('feedback-spinner').remove();
        document.getElementById('feedback-content').innerHTML = coachFeedback.buildHtml(coachAnalysis);
    },

};