const viewRenderer = {
    show(viewHtml) {
        document.querySelector('.content').innerHTML = viewHtml;
    },
    buildSpinnerHtml() {
        return '<div class="spinner"></div>';
    }
};