ALTER TABLE round ADD COLUMN starts_at TIMESTAMPTZ;
CREATE INDEX idx_round_season_starts_at ON round (season_id, starts_at);

UPDATE competition SET external_id = 'PD' WHERE code = 'LALIGA';
UPDATE season SET external_id = '2026'
WHERE name = '2026/27'
  AND competition_id = (SELECT id FROM competition WHERE code = 'LALIGA');