const pvpSocket = {
    stompClient: null,
    connected: false,

    connect(callbacks) {
        const socket = new SockJS('/ws');
        pvpSocket.stompClient = Stomp.over(socket);
        // Disable noisy STOMP debug logging
        pvpSocket.stompClient.debug = null;

        pvpSocket.stompClient.connect({}, function () {
            pvpSocket.connected = true;

            // Subscribe to personal queues
            pvpSocket.stompClient.subscribe('/user/queue/pvp/status', function (msg) {
                if (callbacks.onStatus) callbacks.onStatus(JSON.parse(msg.body));
            });

            pvpSocket.stompClient.subscribe('/user/queue/pvp/match-found', function (msg) {
                if (callbacks.onMatchFound) callbacks.onMatchFound(JSON.parse(msg.body));
            });

            pvpSocket.stompClient.subscribe('/user/queue/pvp/countdown', function (msg) {
                if (callbacks.onCountdown) callbacks.onCountdown(JSON.parse(msg.body));
            });

            pvpSocket.stompClient.subscribe('/user/queue/pvp/champion-confirmed', function (msg) {
                if (callbacks.onChampionConfirmed) callbacks.onChampionConfirmed(JSON.parse(msg.body));
            });

            pvpSocket.stompClient.subscribe('/user/queue/pvp/reveal', function (msg) {
                if (callbacks.onReveal) callbacks.onReveal(JSON.parse(msg.body));
            });

            pvpSocket.stompClient.subscribe('/user/queue/pvp/results', function (msg) {
                if (callbacks.onResults) callbacks.onResults(JSON.parse(msg.body));
            });

            pvpSocket.stompClient.subscribe('/user/queue/pvp/error', function (msg) {
                if (callbacks.onError) callbacks.onError(JSON.parse(msg.body));
            });

            pvpSocket.stompClient.subscribe('/user/queue/pvp/opponent-disconnected', function (msg) {
                if (callbacks.onOpponentDisconnected) callbacks.onOpponentDisconnected(JSON.parse(msg.body));
            });

            if (callbacks.onConnected) callbacks.onConnected();
        }, function (error) {
            pvpSocket.connected = false;
            if (callbacks.onError) callbacks.onError({ message: 'WebSocket forbindelse fejlede.' });
        });
    },

    findMatch(username) {
        if (!pvpSocket.stompClient || !pvpSocket.connected) return;
        pvpSocket.stompClient.send('/app/pvp/find-match', {}, JSON.stringify({ username }));
    },

    selectChampion(roomId, champion) {
        if (!pvpSocket.stompClient || !pvpSocket.connected) return;
        pvpSocket.stompClient.send('/app/pvp/champion-selected', {}, JSON.stringify({ roomId, champion }));
    }
};
