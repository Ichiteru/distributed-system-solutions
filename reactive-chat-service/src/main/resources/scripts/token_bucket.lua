local key = KEYS[1]

local capacity = tonumber(ARGV[1])
local refill_tokens = tonumber(ARGV[2])
local refill_period_millis = tonumber(ARGV[3])
local requested_tokens = tonumber(ARGV[4])
local now_millis = tonumber(ARGV[5])
local ttl_seconds = tonumber(ARGV[6])

local bucket = redis.call('HMGET', key, 'tokens', 'last_refill_millis')

local tokens = tonumber(bucket[1])
local last_refill_millis = tonumber(bucket[2])

if tokens == nil or last_refill_millis == nil then
  tokens = capacity
  last_refill_millis = now_millis
end

local elapsed = now_millis - last_refill_millis
if elapsed < 0 then
  elapsed = 0
end

local refill_periods = math.floor(elapsed / refill_period_millis)
if refill_periods > 0 then
  tokens = math.min(capacity, tokens + (refill_periods * refill_tokens))
  last_refill_millis = last_refill_millis + (refill_periods * refill_period_millis)
end

local allowed = 0
local retry_after_millis = 0

if tokens >= requested_tokens then
  tokens = tokens - requested_tokens
  allowed = 1
else
  local missing_tokens = requested_tokens - tokens
  local periods_needed = math.ceil(missing_tokens / refill_tokens)
  retry_after_millis = (periods_needed * refill_period_millis) - (now_millis - last_refill_millis)

  if retry_after_millis < 0 then
    retry_after_millis = 0
  end
end

redis.call('HSET', key, 'tokens', tokens, 'last_refill_millis', last_refill_millis)
redis.call('EXPIRE', key, ttl_seconds)

return allowed .. ':' .. tokens .. ':' .. retry_after_millis
