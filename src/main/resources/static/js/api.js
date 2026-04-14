// Base URL for the backend. Same pattern as the chatgpt-jokes example.
const SERVER_URL = 'http://localhost:8080/api/v1/';

// Handles HTTP errors the same way as the example's main.js.
async function handleHttpErrors(res) {
    if (!res.ok) {
        const errorResponse = await res.json();
        const msg = errorResponse.message ? errorResponse.message : "No error details provided"
        throw new Error(msg)
    }
    return res.json()
}

// Thin API wrappers used by the other script files.
async function apiGetChampions() {
    return fetch(SERVER_URL + 'champions').then(handleHttpErrors);
}

async function apiPostDraft(role) {
    return fetch(SERVER_URL + 'draft', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ role })
    }).then(handleHttpErrors);
}

async function apiPostCoach(payload) {
    return fetch(SERVER_URL + 'coach', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    }).then(handleHttpErrors);
}
