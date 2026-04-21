import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'ws://localhost:8080';
const chatId = __ENV.CHAT_ID || 'chat-burst';
const userIdPrefix = __ENV.USER_ID_PREFIX || 'client-burst';
const vus = Number(__ENV.VUS || 50);
const duration = __ENV.DURATION || '30s';
const messagesPerConnection = Number(__ENV.MESSAGES_PER_CONNECTION || 100);
const sendIntervalMillis = Number(__ENV.SEND_INTERVAL_MS || 0);
const socketLifetimeMillis = Number(__ENV.SOCKET_LIFETIME_MS || 10000);

const messagesSent = new Counter('ws_burst_messages_sent_total');
const acceptedEvents = new Counter('ws_burst_accepted_total');
const rejectedEvents = new Counter('ws_burst_rejected_total');
const errorEvents = new Counter('ws_burst_error_total');
const connectSuccess = new Counter('ws_burst_connect_success_total');
const connectionErrors = new Counter('ws_burst_connection_errors_total');
const responseLatency = new Trend('ws_burst_response_latency_ms');

export const options = {
  vus,
  duration,
  thresholds: {
    checks: ['rate>0.99'],
    ws_burst_connect_success_total: ['count>0'],
    ws_burst_messages_sent_total: ['count>0'],
    ws_burst_accepted_total: ['count>0'],
    ws_burst_connection_errors_total: ['count==0'],
  },
};

export default function () {
  const userId = `${userIdPrefix}-${__VU}-${__ITER}`;
  const url = `${baseUrl}/ws/chat?userId=${encodeURIComponent(userId)}&chatId=${encodeURIComponent(chatId)}&role=client`;
  const sentAtByCorrelationId = {};

  const response = ws.connect(url, {}, function (socket) {
    socket.on('open', function () {
      connectSuccess.add(1);
      sendMessages(socket, userId, sentAtByCorrelationId);
    });

    socket.on('message', function (rawMessage) {
      const envelope = parseJson(rawMessage);

      if (!envelope) {
        errorEvents.add(1);
        return;
      }

      recordResponseLatency(envelope, sentAtByCorrelationId);

      if (envelope.eventType === 'chat.message.accepted') {
        acceptedEvents.add(1);
        return;
      }

      if (envelope.eventType === 'chat.message.rejected') {
        rejectedEvents.add(1);
        return;
      }

      if (envelope.eventType === 'error') {
        errorEvents.add(1);
      }
    });

    socket.on('error', function () {
      connectionErrors.add(1);
    });

    socket.setTimeout(function () {
      socket.close();
    }, socketLifetimeMillis);
  });

  check(response, {
    'ws status is 101': (r) => r && r.status === 101,
  });

  sleep(1);
}

function sendMessages(socket, userId, sentAtByCorrelationId) {
  if (sendIntervalMillis <= 0) {
    for (let i = 0; i < messagesPerConnection; i += 1) {
      sendMessage(socket, userId, i, sentAtByCorrelationId);
    }

    return;
  }

  let messageIndex = 0;

  const sendNext = function () {
    if (messageIndex >= messagesPerConnection) {
      return;
    }

    sendMessage(socket, userId, messageIndex, sentAtByCorrelationId);
    messageIndex += 1;
    socket.setTimeout(sendNext, sendIntervalMillis);
  };

  sendNext();
}

function sendMessage(socket, userId, messageIndex, sentAtByCorrelationId) {
  const now = new Date();
  const correlationId = `${userId}-${__ITER}-${messageIndex}-${now.getTime()}`;

  sentAtByCorrelationId[correlationId] = Date.now();
  messagesSent.add(1);

  socket.send(JSON.stringify({
    eventId: `burst-event-${correlationId}`,
    eventType: 'chat.message.created',
    correlationId,
    chatId,
    senderId: userId,
    timestamp: now.toISOString(),
    payload: {
      type: 'TEXT',
      value: `burst-message-${messageIndex}`,
    },
  }));
}

function recordResponseLatency(envelope, sentAtByCorrelationId) {
  const sentAt = sentAtByCorrelationId[envelope.correlationId];

  if (!sentAt) {
    return;
  }

  responseLatency.add(Date.now() - sentAt);
  delete sentAtByCorrelationId[envelope.correlationId];
}

function parseJson(rawMessage) {
  try {
    return JSON.parse(rawMessage);
  } catch (_) {
    return null;
  }
}
