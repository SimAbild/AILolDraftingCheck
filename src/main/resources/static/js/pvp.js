document.addEventListener('DOMContentLoaded', async function () {
    // Load champion data for search and icons
    draftChampionPlaceholder.allChampions = await api.getChampions();

    // Connect WebSocket and set up event handlers
    pvpSocket.connect({
        onConnected: function () {
            pvpPage.showMatchmaking();
        },
        onStatus: function (data) {
            pvpPage.updateStatus(data.message);
        },
        onMatchFound: function (data) {
            pvpPage.showDraft(data);
        },
        onCountdown: function (data) {
            pvpPage.updateCountdown(data.seconds);
        },
        onChampionConfirmed: function (data) {
            pvpPage.onChampionConfirmed(data.champion);
        },
        onReveal: function (data) {
            pvpPage.onReveal(data);
        },
        onResults: function (data) {
            pvpPage.showResults(data);
        },
        onError: function (data) {
            pvpPage.showError(data.message);
        },
        onOpponentDisconnected: function (data) {
            pvpPage.showError(data.message);
        }
    });
});

const pvpPage = {
    currentRoomId: null,
    currentRole: null,
    myChampion: null,
    matchData: null,

    // ─── STATE 1: MATCHMAKING ───────────────────────────────────────────

    showMatchmaking() {
        viewRenderer.show(`
            <div class="section-header">
                <span class="section-header__step">PvP</span>
                <h2 class="section-header__title">Find en modstander</h2>
            </div>
            <div class="section-card">
                <p class="card-text">Indtast dit brugernavn og tryk Find Match for at konkurrere mod en anden spiller.</p>
                <div class="matchmaking-form">
                    <input type="text" id="pvp-username" class="text-input" placeholder="Dit brugernavn" maxlength="20" />
                    <button type="button" id="pvp-find-match" class="btn-primary">Find Match</button>
                </div>
                <p id="pvp-status" class="pvp-status" style="display:none;"></p>
            </div>
        `);

        document.getElementById('pvp-find-match').addEventListener('click', function () {
            const username = document.getElementById('pvp-username').value.trim();
            if (!username) return;

            pvpSocket.findMatch(username);
            document.getElementById('pvp-find-match').disabled = true;
            document.getElementById('pvp-username').disabled = true;
            pvpPage.updateStatus('Soeger efter modstander...');
        });

        // Allow Enter key
        document.getElementById('pvp-username').addEventListener('keydown', function (e) {
            if (e.key === 'Enter') document.getElementById('pvp-find-match').click();
        });
    },

    updateStatus(message) {
        const el = document.getElementById('pvp-status');
        if (el) {
            el.textContent = message;
            el.style.display = 'block';
        }
    },

    // ─── STATE 2: DRAFT + CHAMPION SELECT ───────────────────────────────

    showDraft(data) {
        pvpPage.currentRoomId = data.roomId;
        pvpPage.currentRole = data.role;
        pvpPage.matchData = data;
        pvpPage.myChampion = null;

        // Store draft data for icon lookups
        draftChampionPlaceholder.userRole = data.role;
        draftChampionPlaceholder.enemyTeam = data.enemyTeam;
        draftChampionPlaceholder.allyTeam = data.allyTeam;
        draftChampionPlaceholder.userChampion = null;

        viewRenderer.show(`
            <div class="pvp-countdown-bar">
                <span id="pvp-timer" class="countdown-timer">60</span>
            </div>

            <div class="pvp-drafts">
                <div class="pvp-draft-col">
                    <h3 class="pvp-player-name">${data.player1Username}'s Draft</h3>
                    ${pvpPage.buildDraftColumnHtml(data)}
                </div>
                <div class="pvp-draft-col">
                    <h3 class="pvp-player-name">${data.player2Username}'s Draft</h3>
                    ${pvpPage.buildDraftColumnHtml(data)}
                </div>
            </div>

            <div class="section-card pvp-pick-section">
                <p class="card-text">Du spiller <strong>${data.role}</strong> — vaelg din champion:</p>
                <div class="champion-picker">
                    <input type="text" id="champ-input" class="text-input" autocomplete="off"
                           placeholder='Skriv et champion-navn, f.eks. "Aatrox"'/>
                    <ul id="champ-suggestions" class="suggestions-list"></ul>
                </div>
                <p id="pvp-pick-confirm" class="pvp-pick-confirmed" style="display:none;"></p>
            </div>
        `);

        // Set up champion search
        document.getElementById('champ-input').addEventListener('input', championSearch.onInput);
        document.getElementById('champ-suggestions').addEventListener('click', function (event) {
            championSearch.onSuggestionPicked(event);
            pvpPage.onLocalChampionPicked();
        });
    },

    buildDraftColumnHtml(data) {
        let enemyHtml = '';
        for (const pick of data.enemyTeam) {
            enemyHtml += championPick.buildPickedHtml(pick);
        }

        let allyHtml = '';
        for (const role of VALID_ROLES) {
            if (role === data.role) {
                allyHtml += championPick.buildEmptyHtml(role);
                continue;
            }
            const pick = data.allyTeam.find(p => p.role === role);
            allyHtml += pick ? championPick.buildPickedHtml(pick) : championPick.buildEmptyHtml(role);
        }

        return `
            <div class="draft-board draft-board--pvp">
                <div class="team-col">${enemyHtml}</div>
                <div class="vs-label">VS</div>
                <div class="team-col">${allyHtml}</div>
            </div>
        `;
    },

    onLocalChampionPicked() {
        if (!draftChampionPlaceholder.userChampion) return;

        const champion = draftChampionPlaceholder.userChampion.name;
        pvpPage.myChampion = champion;

        // Send to server
        pvpSocket.selectChampion(pvpPage.currentRoomId, champion);

        // Update UI
        document.getElementById('champ-input').value = champion;
        document.getElementById('champ-input').disabled = true;
        document.getElementById('champ-suggestions').innerHTML = '';
    },

    onChampionConfirmed(champion) {
        const el = document.getElementById('pvp-pick-confirm');
        if (el) {
            el.textContent = 'Du har valgt: ' + champion + ' — venter paa countdown...';
            el.style.display = 'block';
        }
    },

    updateCountdown(seconds) {
        const el = document.getElementById('pvp-timer');
        if (el) {
            el.textContent = seconds;
            if (seconds <= 10) el.classList.add('countdown-timer--urgent');
        }
    },

    // ─── REVEAL ─────────────────────────────────────────────────────────

    onReveal(data) {
        // Brief reveal before full results come in
        const el = document.getElementById('pvp-pick-confirm');
        if (el) {
            const p1 = data.player1;
            const p2 = data.player2;
            el.textContent = p1.username + ' valgte ' + (p1.champion || 'ingen') +
                ' | ' + p2.username + ' valgte ' + (p2.champion || 'ingen');
            el.style.display = 'block';
        }
    },

    // ─── STATE 3: RESULTS ───────────────────────────────────────────────

    showResults(data) {
        const p1 = data.player1;
        const p2 = data.player2;

        let winnerText;
        if (p1.points > p2.points) {
            winnerText = p1.username + ' vandt med ' + p1.points + ' point!';
        } else if (p2.points > p1.points) {
            winnerText = p2.username + ' vandt med ' + p2.points + ' point!';
        } else {
            winnerText = 'Uafgjort! Begge scorede ' + p1.points + ' point.';
        }

        viewRenderer.show(`
            <div class="winner-banner">${winnerText}</div>

            <div class="pvp-results">
                <div class="pvp-result-col">
                    ${pvpPage.buildPlayerResultHtml(p1)}
                </div>
                <div class="pvp-result-col">
                    ${pvpPage.buildPlayerResultHtml(p2)}
                </div>
            </div>

            <div class="section-actions" style="justify-content:center; margin-top:1.5rem;">
                <button type="button" id="pvp-play-again" class="btn-primary">Spil igen</button>
            </div>
        `);

        document.getElementById('pvp-play-again').addEventListener('click', function () {
            pvpPage.showMatchmaking();
        });
    },

    buildPlayerResultHtml(player) {
        const pointsClass = pvpPage.getPointsColorClass(player.points);
        const championDisplay = player.champion
            ? `<div class="pvp-result-champion">
                   ${draftChampionPlaceholder.findChampionIconHtml(player.champion)}
                   <span>${player.champion}</span>
               </div>`
            : '<div class="pvp-result-champion"><span>Ingen champion valgt</span></div>';

        let feedbackHtml = '';
        if (player.feedback) {
            feedbackHtml = `
                <div class="feedback-grid">
                    <div>
                        <h6>What works</h6>
                        <ul class="feedback-list">${pvpPage.buildFeedbackList(player.feedback.positives)}</ul>
                    </div>
                    <div>
                        <h6>What doesn't work</h6>
                        <ul class="feedback-list">${pvpPage.buildFeedbackList(player.feedback.negatives)}</ul>
                    </div>
                </div>`;

            if (player.feedback.alternatives && player.feedback.alternatives.length > 0) {
                feedbackHtml += '<h6 class="alternatives-title">Alternatives</h6>';
                for (const alt of player.feedback.alternatives) {
                    const winrateText = alt.winrate && alt.winrate > 0
                        ? `<div class="alt-card__winrate">${alt.winrate}% winrate</div>`
                        : '';
                    feedbackHtml += `
                        <div class="alt-card">
                            ${draftChampionPlaceholder.findChampionIconHtml(alt.name)}
                            <div class="alt-card__body">
                                <div class="alt-card__name">${alt.name}</div>
                                ${winrateText}
                                <div class="alt-card__reason">${alt.reason}</div>
                            </div>
                        </div>`;
                }
            }
        }

        return `
            <div class="section-card pvp-result-card">
                <h3 class="pvp-player-name">${player.username}</h3>
                ${championDisplay}
                <div class="points-display ${pointsClass}">${player.points}pts</div>
                ${feedbackHtml}
            </div>`;
    },

    buildFeedbackList(items) {
        if (!items || items.length === 0) return '<li class="feedback-list__item feedback-list__item--muted">Ingen feedback</li>';
        let html = '';
        for (const item of items) {
            html += '<li class="feedback-list__item">' + item + '</li>';
        }
        return html;
    },

    getPointsColorClass(points) {
        if (points <= 40) return 'points--red';
        if (points <= 70) return 'points--yellow';
        return 'points--green';
    },

    showError(message) {
        const banner = document.getElementById('error-banner');
        if (banner) {
            banner.textContent = message;
            banner.style.display = 'block';
        }
    }
};
