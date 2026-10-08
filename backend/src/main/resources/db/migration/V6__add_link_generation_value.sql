-- Permanent committed allocation history survives Redis loss (FR-CRE-04).
ALTER TABLE links ADD COLUMN generation_value BIGINT;
ALTER TABLE links ADD CONSTRAINT links_generation_value_range
    CHECK (generation_value IS NULL OR generation_value BETWEEN 1 AND 218340105584895);
CREATE INDEX links_generation_value_idx ON links (generation_value DESC)
    WHERE generation_value IS NOT NULL;
