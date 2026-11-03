INSERT INTO competition (code, name, country)
VALUES ('LALIGA', 'LaLiga', 'España');

INSERT INTO season (competition_id, name, start_date, end_date, status)
SELECT id, '2026/27', DATE '2026-08-15', DATE '2027-05-30', 'IN_PROGRESS'
FROM competition
WHERE code = 'LALIGA';

INSERT INTO round (season_id, sequence, name)
SELECT s.id, n, 'Jornada ' || n
FROM season s
JOIN competition c ON c.id = s.competition_id
CROSS JOIN generate_series(1, 38) AS n
WHERE c.code = 'LALIGA' AND s.name = '2026/27';