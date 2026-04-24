async function handleHttpErrors(response) {
    if (!response.ok) {
        const errorBody = await response.json();
        const errorMessage = errorBody.message ? errorBody.message : 'No error details provided';
        throw new Error(errorMessage);
    }
    return response.json();
}

const api = {
    async getChampions() {
        return fetch(SERVER_BASE_URL + 'champions').then(handleHttpErrors);
    },
    async postDraft(role) {
        return fetch(SERVER_BASE_URL + 'draft', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ role })
        }).then(handleHttpErrors);
    },
    async postCoach(coachRequest) {
        return fetch(SERVER_BASE_URL + 'coach', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(coachRequest)
        }).then(handleHttpErrors);
    }
};