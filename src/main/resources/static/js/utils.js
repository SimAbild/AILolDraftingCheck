// Erstatter hele indholdet i .content med den givne HTML-streng.
// Dette er kernen i one-page designet — kun ét view er synligt ad gangen.
function showView(viewHtml) {
    document.querySelector('.content').innerHTML = viewHtml;
}

// Delt spinner-HTML der bruges i draft og coach viewet mens AI'en arbejder.
function buildSpinnerHtml() {
    return '<div class="spinner"></div>';
}
