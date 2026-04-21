import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'ws://localhost:8080';
const chatIdPrefix = __ENV.CHAT_ID_PREFIX || 'chat-multi';
const userIdPrefix = __ENV.USER_ID_PREFIX || 'multi-user';
const chatCount = Number(__ENV.CHAT_COUNT || 5);
const usersPerChat = Number(__ENV.USERS_PER_CHAT || __ENV.SENDERS_PER_CHAT || 5);
const duration = __ENV.DURATION || '30s';
const messagesPerConnection = Number(__ENV.MESSAGES_PER_CONNECTION || 5);
const sendIntervalMillis = Number(__ENV.SEND_INTERVAL_MS || 100);
const sendStartDelayMillis = Number(__ENV.SEND_START_DELAY_MS || 1000);
const socketLifetimeMillis = Number(__ENV.SOCKET_LIFETIME_MS || 35000);

const messagesSent = new Counter('ws_multi_chat_messages_sent_total');
const acceptedEvents = new Counter('ws_multi_chat_accepted_total');
const receivedEvents = new Counter('ws_multi_chat_received_total');
const crossChatLeaks = new Counter('ws_multi_chat_cross_chat_leaks_total');
const selfDeliveryLeaks = new Counter('ws_multi_chat_self_delivery_leaks_total');
const errorEvents = new Counter('ws_multi_chat_error_events_total');
const connectSuccess = new Counter('ws_multi_chat_connect_success_total');
const connectionErrors = new Counter('ws_multi_chat_connection_errors_total');
const responseLatency = new Trend('ws_multi_chat_response_latency_ms');

export const options = {
  scenarios: buildScenarios(),
  thresholds: {
    checks: ['rate>0.99'],
    ws_multi_chat_connect_success_total: [`count>=${chatCount * usersPerChat}`],
    ws_multi_chat_messages_sent_total: ['count>0'],
    ws_multi_chat_accepted_total: ['count>0'],
    ws_multi_chat_received_total: ['count>0'],
    ws_multi_chat_cross_chat_leaks_total: ['count==0'],
    ws_multi_chat_self_delivery_leaks_total: ['count==0'],
    ws_multi_chat_connection_errors_total: ['count==0'],
  },
};

export function participant() {
  const chatId = currentChatId();
  const userId = currentUserId(chatId);
  const role = currentRole();
  const url = connectUrl(userId, chatId, role);
  const sentAtByCorrelationId = {};
  let liveEventsStartedAtMillis = 0;

  const response = ws.connect(url, {}, function (socket) {
    socket.on('open', function () {
      liveEventsStartedAtMillis = Date.now();
      connectSuccess.add(1, { chatId, role });

      socket.setTimeout(function () {
        sendMessages(socket, chatId, userId, sentAtByCorrelationId);
      }, sendStartDelayMillis);
    });

    socket.on('message', function (rawMessage) {
      const envelope = parseJson(rawMessage);

      if (!envelope) {
        errorEvents.add(1, { chatId, reason: 'invalid_json' });
        return;
      }

      recordResponseLatency(envelope, sentAtByCorrelationId);

      if (envelope.eventType === 'chat.message.accepted') {
        acceptedEvents.add(1, { chatId });
        return;
      }

      if (envelope.eventType === 'chat.message.created') {
        recordCreatedDelivery(envelope, chatId, userId, liveEventsStartedAtMillis);
        return;
      }

      if (envelope.eventType === 'error') {
        errorEvents.add(1, { chatId, reason: 'error_event' });
      }
    });

    socket.on('error', function () {
      connectionErrors.add(1, { chatId, role });
    });

    socket.setTimeout(function () {
      socket.close();
    }, socketLifetimeMillis);
  });

  check(response, {
    'participant ws status is 101': (value) => value && value.status === 101,
  });

  sleep(1);
}

function buildScenarios() {
  const scenarios = {};

  for (let chatIndex = 0; chatIndex < chatCount; chatIndex += 1) {
    scenarios[`chat-${chatIndex}`] = {
      executor: 'constant-vus',
      exec: 'participant',
      vus: usersPerChat,
      duration,
      startTime: '0s',
      env: {
        CHAT_INDEX: String(chatIndex),
      },
    };
  }

  return scenarios;
}

function sendMessages(socket, chatId, userId, sentAtByCorrelationId) {
  if (sendIntervalMillis <= 0) {
    for (let i = 0; i < messagesPerConnection; i += 1) {
      sendMessage(socket, chatId, userId, i, sentAtByCorrelationId);
    }

    return;
  }

  let messageIndex = 0;

  const sendNext = function () {
    if (messageIndex >= messagesPerConnection) {
      return;
    }

    sendMessage(socket, chatId, userId, messageIndex, sentAtByCorrelationId);
    messageIndex += 1;
    socket.setTimeout(sendNext, sendIntervalMillis);
  };

  sendNext();
}

function sendMessage(socket, chatId, userId, messageIndex, sentAtByCorrelationId) {
  const now = new Date();
  const correlationId = `${userId}-${messageIndex}-${now.getTime()}`;

  sentAtByCorrelationId[correlationId] = Date.now();
  messagesSent.add(1, { chatId });

  socket.send(JSON.stringify({
    eventId: `multi-chat-event-${correlationId}`,
    eventType: 'chat.message.created',
    correlationId,
    chatId,
    senderId: userId,
    timestamp: now.toISOString(),
    payload: {
      type: 'TEXT',
      value: `multi-chat-message-${chatId}-${userId}-${messageIndex}`,
    },
  }));
}

function recordCreatedDelivery(envelope, expectedChatId, currentUserIdValue, liveEventsStartedAtMillis) {
  if (!isLiveEvent(envelope, liveEventsStartedAtMillis)) {
    return;
  }

  if (envelope.chatId !== expectedChatId) {
    crossChatLeaks.add(1, {
      expectedChatId,
      actualChatId: envelope.chatId || 'missing',
    });
    return;
  }

  if (envelope.senderId === currentUserIdValue) {
    selfDeliveryLeaks.add(1, { chatId: expectedChatId });
    return;
  }

  receivedEvents.add(1, { chatId: expectedChatId });
}

function isLiveEvent(envelope, liveEventsStartedAtMillis) {
  const eventTimeMillis = Date.parse(envelope.timestamp);

  if (Number.isNaN(eventTimeMillis)) {
    return false;
  }

  return eventTimeMillis >= liveEventsStartedAtMillis;
}

function recordResponseLatency(envelope, sentAtByCorrelationId) {
  const sentAt = sentAtByCorrelationId[envelope.correlationId];

  if (!sentAt) {
    return;
  }

  responseLatency.add(Date.now() - sentAt);
  delete sentAtByCorrelationId[envelope.correlationId];
}

function connectUrl(userId, chatId, role) {
  return `${baseUrl}/ws/chat?userId=${encodeURIComponent(userId)}&chatId=${encodeURIComponent(chatId)}&role=${role}`;
}

function currentChatId() {
  return `${chatIdPrefix}-${__ENV.CHAT_INDEX || '0'}`;
}

function currentUserId(chatId) {
  return `${userIdPrefix}-${chatId}-${__VU}`;
}

function currentRole() {
  return __VU % 2 === 0 ? 'operator' : 'client';
}

function parseJson(rawMessage) {
  try {
    return JSON.parse(rawMessage);
  } catch (_) {
    return null;
  }
}
