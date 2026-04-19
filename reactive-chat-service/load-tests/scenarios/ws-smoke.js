import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

export const options = {
  scenarios: {
    receiver: {
      executor: 'shared-iterations',
      exec: 'receiver',
      vus: 1,
      iterations: 1,
      startTime: '0s',
    },
    sender: {
      executor: 'shared-iterations',
      exec: 'sender',
      vus: 1,
      iterations: 1,
      startTime: '1s',
    },
  },
  thresholds: {
    checks: ['rate==1.0'],
    ws_smoke_sender_accepted_total: ['count>=1'],
    ws_smoke_receiver_created_total: ['count>=1'],
  },
};

const senderAccepted = new Counter('ws_smoke_sender_accepted_total');
const receiverCreated = new Counter('ws_smoke_receiver_created_total');
const receiverTyping = new Counter('ws_smoke_receiver_typing_total');
const errorEvents = new Counter('ws_smoke_error_events_total');

const baseUrl = __ENV.BASE_URL || 'ws://localhost:8080';
const chatId = __ENV.CHAT_ID || 'chat-smoke';
const senderId = __ENV.SENDER_ID || 'client-smoke-1';
const receiverId = __ENV.RECEIVER_ID || 'operator-smoke-1';

function connectUrl(userId, role) {
  return `${baseUrl}/ws/chat?userId=${encodeURIComponent(userId)}&chatId=${encodeURIComponent(chatId)}&role=${role}`;
}

function envelope(eventType, sender, payload, correlationId) {
  return JSON.stringify({
    eventId: `${correlationId}-event`,
    eventType,
    correlationId,
    chatId,
    senderId: sender,
    timestamp: new Date().toISOString(),
    payload,
  });
}

export function receiver() {
  const response = ws.connect(connectUrl(receiverId, 'operator'), {}, function (socket) {
    socket.on('message', function (rawMessage) {
      const message = JSON.parse(rawMessage);

      if (message.eventType === 'chat.message.created') {
        receiverCreated.add(1);
      }

      if (message.eventType === 'chat.typing.started' || message.eventType === 'chat.typing.stopped') {
        receiverTyping.add(1);
      }

      if (message.eventType === 'error') {
        errorEvents.add(1);
      }
    });

    socket.setTimeout(function () {
      socket.close();
    }, 5000);
  });

  check(response, {
    'receiver ws status is 101': (value) => value && value.status === 101,
  });

  sleep(1);
}

export function sender() {
  const response = ws.connect(connectUrl(senderId, 'client'), {}, function (socket) {
    socket.on('open', function () {
      socket.send(envelope(
        'chat.typing.started',
        senderId,
        { type: 'TEXT', value: 'started' },
        'smoke-typing-started',
      ));

      socket.send(envelope(
        'chat.message.created',
        senderId,
        { type: 'TEXT', value: 'smoke message 1' },
        'smoke-message-1',
      ));

      socket.send(envelope(
        'chat.message.created',
        senderId,
        { type: 'TEXT', value: 'smoke message 2' },
        'smoke-message-2',
      ));

      socket.send(envelope(
        'chat.typing.stopped',
        senderId,
        { type: 'TYPING', value: 'stopped' },
        'smoke-typing-stopped',
      ));
    });

    socket.on('message', function (rawMessage) {
      const message = JSON.parse(rawMessage);

      if (message.eventType === 'chat.message.accepted') {
        senderAccepted.add(1);
      }

      if (message.eventType === 'error') {
        errorEvents.add(1);
      }
    });

    socket.setTimeout(function () {
      socket.close();
    }, 4000);
  });

  check(response, {
    'sender ws status is 101': (value) => value && value.status === 101,
  });

  sleep(1);
}
