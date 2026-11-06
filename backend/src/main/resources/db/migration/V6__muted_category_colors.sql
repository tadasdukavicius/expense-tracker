-- Muted palette for the seeded categories; only rows still on their original colour are touched.
UPDATE category AS c
SET color = v.new_color
FROM (VALUES
    ('Groceries',      '#4caf50', '#315c45'),
    ('Subscriptions',  '#9c27b0', '#8d8f5e'),
    ('Gas',            '#ff9800', '#d9a26b'),
    ('Home utilities', '#607d8b', '#c9b88d'),
    ('Health',         '#e91e63', '#a8574f'),
    ('Takeaway',       '#ff5722', '#c0714f'),
    ('Entertainment',  '#3f51b5', '#5b7f8c'),
    ('Transport',      '#00bcd4', '#7fa088'),
    ('Own-account transfer', '#9e9e9e', '#a9a79d')
) AS v(name, old_color, new_color)
WHERE c.name = v.name AND lower(c.color) = v.old_color;
