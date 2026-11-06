ALTER TABLE category ADD COLUMN excluded_from_stats BOOLEAN NOT NULL DEFAULT FALSE;

INSERT INTO category (name, color, emoji, excluded_from_stats)
VALUES ('Own-account transfer', '#9e9e9e', '🔄', TRUE)
ON CONFLICT (name) DO UPDATE SET excluded_from_stats = TRUE;

-- Priority 10 so these win over merchant keyword rules. Stems avoid declension differences.
INSERT INTO category_rule (match_field, pattern, category_id, priority)
SELECT 'DESCRIPTION', v.keyword, c.id, 10
FROM (VALUES
    ('pervedimas tarp savo'),
    ('pervedimas į taupykl')
) AS v(keyword)
JOIN category c ON c.name = 'Own-account transfer';
