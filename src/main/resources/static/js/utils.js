// Erstatter hele indholdet i .content med den givne HTML-streng.
// Dette er kernen i one-page designet — kun ét view er synligt ad gangen.
function showView(viewHtml) {
    document.querySelector('.content').innerHTML = viewHtml;
}

function showError(message) {
    const errorBanner = document.getElementById('error-banner');
    errorBanner.textContent = message;
    errorBanner.style.display = 'block';
}

function hideError() {
    document.getElementById('error-banner').style.display = 'none';
}

// Delt spinner-HTML der bruges i draft og coach viewet mens AI'en arbejder.
function buildSpinnerHtml() {
    return '<div class="spinner"></div>';
}
