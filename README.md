# Ledger — expense tracker

[![CI](https://github.com/tadasdukavicius/expense-tracker/actions/workflows/ci.yml/badge.svg)](https://github.com/tadasdukavicius/expense-tracker/actions/workflows/ci.yml)

Import a bank statement CSV, let the app de-duplicate and auto-categorize the transactions with rules, and see where the money went.

Built as a portfolio project with a focus on the relational database design behind it. It was developed with AI assistance (GitHub Copilot); the design decisions below are mine to explain.

## Features

- **Statement import** with preview before saving. Parsers sit behind a `StatementParser` interface, chosen by the file's header line. Supported: a generic semicolon CSV and the Swedbank LT export.
- **Duplicate detection:** re-importing an overlapping statement never double-counts (see [Database design](#database-design)).
- **Rule-based categorization:** rules such as "description contains MAXIMA → Groceries", with priorities. The first matching rule wins and manual changes are never overwritten. A rule can be created from a transaction.
- **Own-account transfers** can be excluded from spending stats.
- **Overview dashboard:** spending by category for a chosen period and account.
- Transactions table with filters and paging, category management with emoji and colour.

## Stack

| Layer | Tech |
|---|---|
| Backend | Java 21, Spring Boot, Spring Data JPA, Bean Validation |
| Database | PostgreSQL 16, Flyway migrations |
| Frontend | Angular (standalone components), Angular Material, Chart.js |
| Tests | JUnit, Mockito, Testcontainers; Vitest for the frontend |
| CI | GitHub Actions: backend `verify`, frontend test and build |

## Database design

Schema lives in [`backend/src/main/resources/db/migration`](backend/src/main/resources/db/migration) (V1–V6).

- **Money is `NUMERIC(19,2)`** and `BigDecimal` in Java, never floating point.
- **Duplicate prevention in the database, not only the app.** `transactions.dedup_hash` is a SHA-256 of date, amount, counterparty, description and reference. A partial unique index enforces it per account:
  ```sql
  CREATE UNIQUE INDEX uq_transactions_account_dedup_hash
      ON transactions(account_id, dedup_hash)
      WHERE account_id IS NOT NULL AND dedup_hash IS NOT NULL;
  ```
- **Two-step import** with `import_batch` and `import_batch_row` staging tables: the upload is stored as a preview, and only confirming moves non-duplicate rows into `transactions`. `CHECK` constraints guard the status and counts, and rows cascade-delete with their batch.
- **Aggregation in SQL/JPQL:** spend by category uses a `LEFT JOIN` so uncategorized rows are counted, and skips categories flagged `excluded_from_stats`.
- **Indexes** on `booking_date` and `category_id` for the date-range and grouping queries.
- Migrations are append-only: applied files are never edited, only superseded.

## Run it locally

Requirements: Java 21, Node 22+, Docker.

```bash
docker compose up -d                    # PostgreSQL on :5432

cd backend && ./mvnw spring-boot:run    # API on :8080, Flyway runs on startup

cd frontend && npm ci && npx ng serve   # UI on http://localhost:4200
```

Run the tests:

```bash
cd backend && ./mvnw test               # needs Docker (Testcontainers)
cd frontend && npx ng test --watch=false
```

### Demo data

The repository contains only fabricated data. To try the app without a real statement:

- Import [`sample-swedbank-lt.csv`](backend/src/test/resources/sample-swedbank-lt.csv) through the Import page, or
- load six months of generated transactions into a separate demo database:

```bash
docker exec expense-tracker-db-1 psql -U expenses -d postgres -c "CREATE DATABASE expenses_demo"
cd backend && DB_URL=jdbc:postgresql://localhost:5432/expenses_demo ./mvnw spring-boot:run   # creates the schema, then stop it
docker exec -i expense-tracker-db-1 psql -U expenses -d expenses_demo < scripts/seed-demo.sql
cd backend && DB_URL=jdbc:postgresql://localhost:5432/expenses_demo ./mvnw spring-boot:run
```

Configuration is read from environment variables: `DB_URL`, `DB_USER`, `DB_PASSWORD`. `.env` files and all CSVs except the two fake samples are git-ignored.

## API

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/imports` | Upload a statement, returns a preview |
| POST | `/api/imports/{id}/confirm` | Save the previewed rows |
| GET | `/api/transactions` | Filter by date, category, text, account; paged |
| PATCH | `/api/transactions/{id}` | Change a transaction's category |
| POST | `/api/transactions/recategorize` | Re-run the rules |
| GET/POST/PUT/DELETE | `/api/categories`, `/api/category-rules` | Manage categories and rules |
| POST | `/api/category-rules/from-transaction/{id}` | Create a rule from a transaction |
| GET/POST | `/api/accounts` | Accounts |
| GET | `/api/stats/by-category?from=&to=&accountId=` | Spend per category |

## Roadmap

- Authentication (JWT) with per-user data isolation
- Monthly trend endpoint and chart
- Spending limits with alerts
- Docker images and deployment
