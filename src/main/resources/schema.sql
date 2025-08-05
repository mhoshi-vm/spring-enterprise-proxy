

CREATE TABLE IF NOT EXISTS artifact (
                           id VARCHAR(36) NOT NULL PRIMARY KEY,
                           path VARCHAR(512) NOT NULL,
                           content BLOB NOT NULL,
                           content_type VARCHAR(255) NOT NULL,
                           last_modified TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS event_publication
(
    id               VARCHAR(36) NOT NULL PRIMARY KEY ,
    listener_id      TEXT NOT NULL,
    event_type       TEXT NOT NULL,
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completion_date  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );