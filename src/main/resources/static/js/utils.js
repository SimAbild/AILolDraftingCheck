function revealSection(sectionId) {
    const section = document.getElementById(sectionId);
    section.classList.remove('section--hidden');
    section.classList.add('section--revealed');
}

function hideSection(sectionId) {
    const section = document.getElementById(sectionId);
    section.classList.add('section--hidden');
    section.classList.remove('section--revealed');
}

function showError(errorMessage) {
    const errorBanner = document.getElementById('error-banner');
    errorBanner.textContent = errorMessage;
    errorBanner.style.display = 'block';
}

function hideError() {
    document.getElementById('error-banner').style.display = 'none';
}
