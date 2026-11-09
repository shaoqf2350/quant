-- QuantPF V1_0_0 baseline; Flyway supplies the schema.
CREATE TABLE quantpf_schema_marker (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    schema_version VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_quantpf_schema_marker_version (schema_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO quantpf_schema_marker (schema_version) VALUES ('V1_0_0');