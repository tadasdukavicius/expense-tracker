ALTER TABLE category ADD COLUMN emoji VARCHAR(16);

INSERT INTO category (name, color, emoji) VALUES
    ('Groceries',      '#4caf50', '🛒'),
    ('Subscriptions',  '#9c27b0', '🔁'),
    ('Gas',            '#ff9800', '⛽'),
    ('Home utilities', '#607d8b', '💡'),
    ('Health',         '#e91e63', '💊'),
    ('Takeaway',       '#ff5722', '🍔'),
    ('Entertainment',  '#3f51b5', '🎬'),
    ('Transport',      '#00bcd4', '🚕')
ON CONFLICT (name) DO UPDATE
    SET emoji = COALESCE(category.emoji, EXCLUDED.emoji);

-- Lower priority number wins. Specific rules (e.g. "bolt food") must beat generic ones ("bolt").
INSERT INTO category_rule (match_field, pattern, category_id, priority)
SELECT 'COUNTERPARTY', v.keyword, c.id, v.priority
FROM (VALUES
    ('Groceries', 'maxima', 100), ('Groceries', 'rimi', 100), ('Groceries', 'lidl', 100),
    ('Groceries', 'iki', 100), ('Groceries', 'barbora', 100), ('Groceries', 'norfa', 100),
    ('Subscriptions', 'netflix', 100), ('Subscriptions', 'spotify', 100),
    ('Subscriptions', 'youtube', 100), ('Subscriptions', 'disney', 100),
    ('Subscriptions', 'icloud', 100), ('Subscriptions', 'google one', 100),
    ('Gas', 'circle k', 100), ('Gas', 'neste', 100), ('Gas', 'viada', 100),
    ('Gas', 'orlen', 100), ('Gas', 'lukoil', 100),
    ('Home utilities', 'ignitis', 100), ('Home utilities', 'elektrum', 100),
    ('Home utilities', 'latvenergo', 100), ('Home utilities', 'telia', 100),
    ('Home utilities', 'tele2', 100), ('Home utilities', 'bite', 100),
    ('Health', 'pharmacy', 100), ('Health', 'apotheka', 100), ('Health', 'benu', 100),
    ('Health', 'eurovaistine', 100), ('Health', 'clinic', 100), ('Health', 'dental', 100),
    ('Takeaway', 'bolt food', 50), ('Takeaway', 'wolt', 100), ('Takeaway', 'glovo', 100),
    ('Takeaway', 'foodora', 100), ('Takeaway', 'mcdonald', 100), ('Takeaway', 'kfc', 100),
    ('Takeaway', 'burger king', 100), ('Takeaway', 'pizza', 100),
    ('Entertainment', 'cinema', 100), ('Entertainment', 'forum cinemas', 100),
    ('Entertainment', 'steam', 100), ('Entertainment', 'playstation', 100),
    ('Entertainment', 'ticketmaster', 100),
    ('Transport', 'bolt', 100), ('Transport', 'uber', 100), ('Transport', 'taxi', 100),
    ('Transport', 'flixbus', 100), ('Transport', 'lux express', 100), ('Transport', 'ltg', 100)
) AS v(category_name, keyword, priority)
JOIN category c ON c.name = v.category_name;

-- Description fallback, for rows without a usable merchant name
INSERT INTO category_rule (match_field, pattern, category_id, priority)
SELECT 'DESCRIPTION', v.keyword, c.id, 200
FROM (VALUES
    ('Transport', 'bus ticket'), ('Transport', 'train ticket'), ('Transport', 'taxi'),
    ('Transport', 'parking'), ('Gas', 'fuel'), ('Health', 'pharmacy'),
    ('Home utilities', 'electricity'), ('Home utilities', 'internet'),
    ('Home utilities', 'water bill'), ('Groceries', 'groceries'),
    ('Subscriptions', 'subscription')
) AS v(category_name, keyword)
JOIN category c ON c.name = v.category_name;