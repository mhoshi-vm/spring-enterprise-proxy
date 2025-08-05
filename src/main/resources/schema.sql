

CREATE TABLE IF NOT EXISTS artifact (
                           id VARCHAR(36) PRIMARY KEY, -- H2's native UUID type
                           path VARCHAR(512) NOT NULL,
                           content BLOB NOT NULL,
                           content_type VARCHAR(255) NOT NULL,
                           last_modified TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS event_publication
(
    id               UUID NOT NULL,
    listener_id      TEXT NOT NULL,
    event_type       TEXT NOT NULL,
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date  TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id)
    );