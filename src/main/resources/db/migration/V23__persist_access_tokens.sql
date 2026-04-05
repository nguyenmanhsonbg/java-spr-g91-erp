CREATE TABLE access_tokens (
    token VARCHAR(128) NOT NULL,
    user_id CHAR(36) NOT NULL,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (token),
    CONSTRAINT fk_access_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_access_tokens_user
ON access_tokens(user_id);

CREATE INDEX idx_access_tokens_expires_at
ON access_tokens(expires_at);
