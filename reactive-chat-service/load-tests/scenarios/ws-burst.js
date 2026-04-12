import ws from 'k6/ws';
import { check, sleep } from 'k6';

export const options = {
  vus: 50,
  duration: '30s',
};

const baseUrl = __ENV.BASE_URL || 'ws://localhost:8080';
const chatId = __ENV.CHAT_ID || 'chat-1';
const senderId = __ENV.SENDER_ID || 'client-1';

export default function () {
  const url = `${baseUrl}/ws/chat`;

  const response = ws.connect(url, {}, function (socket) {
    socket.on('open', function () {
      for (let i = 0; i < 100; i++) {
        socket.send(JSON.stringify({
          eventType: 'chat.message.created',
          chatId,
          senderId,
          payload: {
            text: `message-${i}`,
          },
          timestamp: new Date().toISOString(),
        }));
      }
    });

    socket.on('error', function () {
      // Intentionally empty in scaffold version.
    });

    socket.on('close', function () {
      // Intentionally empty in scaffold version.
    });

    socket.setTimeout(function () {
      socket.close();
    }, 1000);
  });

  check(response, {
    'ws status is 101': (r) => r && r.status === 101,
  });

  sleep(1);
}
