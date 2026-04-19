const SERVER_BASE_URL = 'http://localhost:8080/api/v1/';

async function handleHttpErrors(response) {
    if (!response.ok) {
        const errorBody = await response.json();
        const errorMessage = errorBody.message ? errorBody.message : 'No error details provided';
        throw new Error(errorMessage);
    }
    return response.json();
}

async function apiGetChampions() {
    return fetch(SERVER_BASE_URL + 'champions').then(handleHttpErrors);
}

async function apiPostDraft(role) {
    return fetch(SERVER_BASE_URL + 'draft', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ role })
    }).then(handleHttpErrors);
}

async function apiPostCoach(coachRequest) {
    return fetch(SERVER_BASE_URL + 'coach', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(coachRequest)
    }).then(handleHttpErrors);
}
