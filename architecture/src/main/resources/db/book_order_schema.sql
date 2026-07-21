CREATE TABLE book_order_recovery_task (
    task_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    unique_key VARCHAR(256) NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    child_order_no VARCHAR(64) NOT NULL DEFAULT '',
    pnr VARCHAR(32) NOT NULL DEFAULT '',
    task_type VARCHAR(32) NOT NULL,
    task_status VARCHAR(32) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NULL,
    last_error VARCHAR(1024),
    UNIQUE KEY uk_recovery_business (unique_key),
    UNIQUE KEY uk_cancel_pnr (order_no, child_order_no, pnr, task_type)
);

CREATE TABLE book_order_state_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(128) NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    from_state VARCHAR(32) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    to_state VARCHAR(32) NOT NULL,
    operator VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_order_event (event_id)
);

CREATE TABLE book_order_outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(128) NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    published TINYINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_outbox_event (event_id)
);
