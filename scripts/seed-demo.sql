-- Fabricated demo data for screenshots and demos. Run it on a demo database after the
-- backend has started once (so Flyway has created the schema and categories).
-- Re-running is a no-op if the demo account already exists.
-- Deterministic: amounts and days derive from md5(), not random().

DO $$
DECLARE
    acc_id BIGINT;
BEGIN
    IF EXISTS (SELECT 1 FROM account WHERE name = 'Demo current account') THEN
        RAISE NOTICE 'Demo data already present, skipping.';
        RETURN;
    END IF;

    INSERT INTO account (name, currency) VALUES ('Demo current account', 'EUR') RETURNING id INTO acc_id;

    CREATE TEMP TABLE demo_merchant (
        merchant TEXT, category TEXT, base NUMERIC, per_month INT
    ) ON COMMIT DROP;

    INSERT INTO demo_merchant VALUES
        ('Maxima Example',        'Groceries',      18.50, 8),
        ('Iki Example',           'Groceries',      24.00, 4),
        ('Spotify Example',       'Subscriptions',   8.99, 1),
        ('Streamflix Example',    'Subscriptions',  11.99, 1),
        ('Example Fuel',          'Gas',            42.00, 3),
        ('Example Energy',        'Home utilities', 58.00, 1),
        ('Example Water',         'Home utilities', 14.00, 1),
        ('Example Pharmacy',      'Health',         12.50, 2),
        ('Burger Corner Example', 'Takeaway',       11.80, 5),
        ('Pizza Place Example',   'Takeaway',       16.40, 2),
        ('Example Cinema',        'Entertainment',   9.50, 2),
        ('Example Taxi',          'Transport',       7.30, 4);

    -- Purchases: a deterministic day (0-26) and amount (80-129% of base) per merchant/month/index.
    INSERT INTO transactions (account_id, booking_date, amount, currency, description,
                              counterparty, category_id, reference)
    SELECT acc_id,
           p.booking_date,
           -round(m.base * (0.8 + (abs(('x' || substr(md5(n || m.merchant || mo), 1, 6))::bit(24)::int) % 50) / 100.0), 2),
           'EUR',
           'Card purchase ' || m.merchant,
           m.merchant,
           c.id,
           'DEMO-' || substr(md5(m.merchant || mo || n), 1, 12)
    FROM demo_merchant m
    CROSS JOIN generate_series(0, 5) AS mo
    CROSS JOIN LATERAL generate_series(1, m.per_month) AS n
    CROSS JOIN LATERAL (
        SELECT (date_trunc('month', current_date) - make_interval(months => mo))::date
               + (abs(('x' || substr(md5(m.merchant || mo || n), 1, 6))::bit(24)::int) % 27) AS booking_date
    ) p
    JOIN category c ON c.name = m.category
    WHERE p.booking_date <= current_date;

    INSERT INTO transactions (account_id, booking_date, amount, currency, description, counterparty, reference)
    SELECT acc_id, (date_trunc('month', current_date) - make_interval(months => mo))::date,
           2400.00, 'EUR', 'Salary', 'Example Employer', 'DEMO-SALARY-' || mo
    FROM generate_series(0, 5) AS mo;

    -- Own-account transfers carry the excluded category, so stats ignore them.
    INSERT INTO transactions (account_id, booking_date, amount, currency, description,
                              counterparty, category_id, reference)
    SELECT acc_id, (date_trunc('month', current_date) - make_interval(months => mo))::date + 1,
           -300.00, 'EUR', 'Transfer to savings account', 'Own account',
           (SELECT id FROM category WHERE name = 'Own-account transfer'), 'DEMO-SAVE-' || mo
    FROM generate_series(0, 5) AS mo;
END $$;
