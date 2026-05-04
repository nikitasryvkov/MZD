UPDATE dashboard.streaming_outbox
SET topic = '/topic/v1.trains',
    next_attempt_at = now(),
    last_error = NULL
WHERE topic = '/topic/v1/trains'
  AND published_at IS NULL;

UPDATE dashboard.streaming_outbox
SET topic = '/topic/v1.events',
    next_attempt_at = now(),
    last_error = NULL
WHERE topic = '/topic/v1/events'
  AND published_at IS NULL;
