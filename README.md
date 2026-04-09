# CS4135 E-Library Project

Microservices for an e-library: service discovery (Eureka), centralised config (Spring Cloud Config), RabbitMQ, and bounded-context services (e.g. borrowing).

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (with Compose)
- For full stack: a [Supabase](https://supabase.com/) PostgreSQL project (or any PostgreSQL) and JDBC URLs per service

## Quick start — shared infrastructure only

From the **repository root**:

```bash
docker compose up eureka-server config-server rabbitmq --build
```

Sanity checks:

| Service        | URL                                      |
|----------------|------------------------------------------|
| Eureka         | http://localhost:8761                    |
| Config Server  | http://localhost:8888/actuator/health    |
| RabbitMQ UI    | http://localhost:15672 (guest / guest)   |

## Search & browse (profile `full`)

**search-service** calls **book-service** via Eureka to search and browse the catalogue (`GET /api/books` with optional filters; empty filters = browse first page).

1. Copy the environment template and add your database credentials:

   ```bash
   copy .env.example .env
   ```

   On macOS/Linux: `cp .env.example .env`

2. Edit `.env`: set `DB_USERNAME`, `DB_PASSWORD`, and JDBC URLs including `BOOK_DB_URL` for the catalogue. Use `?sslmode=require` if your host requires SSL.

3. Start infrastructure plus book catalogue and search:

   ```bash
   docker compose --profile full up eureka-server config-server rabbitmq borrowing-service book-service search-service --build
   ```

| Service        | Port | Role |
|----------------|------|------|
| book-service   | 8083 | Catalogue API |
| search-service | 8085 | `GET /api/search`, `GET /api/search/suggestions` → book-service via Eureka |

**Try browse:** `http://localhost:8085/api/search?page=0&size=10`

## Test UI (catalogue search)

A small **Vite** app under `frontend/` proxies `/api/search` to search-service.

1. Start the backend stack (at least **search-service**, **book-service**, **Eureka**, **Config**; use the `full` profile command above).
2. Install and run the UI:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

3. Open **http://localhost:5173** — submit **Catalogue search** with empty filters to browse, or set title/author/keyword to filter.

The dev server maps:

- `/api/search` → search-service (8085)
- `/api/borrows` → borrowing-service (8082)

Do **not** commit `.env`; it is listed in `.gitignore`.

## Environment variables (reference)

| Variable           | Used by                         |
|--------------------|---------------------------------|
| `DB_USERNAME`      | Services using PostgreSQL       |
| `DB_PASSWORD`      | Services using PostgreSQL       |
| `USER_DB_URL`      | user-service                    |
| `BORROWING_DB_URL` | borrowing-service               |
| `BOOK_DB_URL`      | book-service                    |
| `RABBITMQ_USER`    | Services using RabbitMQ         |
| `RABBITMQ_PASS`    | Services using RabbitMQ         |
