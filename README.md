# CS4135 Library Project

E-Library microservices system built with Spring Boot + React.

## Services

- `backend/eureka-server` (service discovery)
- `backend/config-server` (centralized configuration)
- `backend/user-service`
- `backend/borrowing-service`
- `backend/book-service`
- `backend/search-service`
- `backend/notification-service`
- `frontend`

## Run End-to-End (Docker + Frontend)

1. Start backend stack:
   - `docker compose up -d --build`
2. Start frontend:
   - `cd frontend`
   - `npm install`
   - `npm start`
3. Key URLs:
   - Frontend: `http://localhost:3000`
   - Eureka: `http://localhost:8761`
   - Config Server: `http://localhost:8888`

## Run Automated Tests

From each service folder:

- `mvn test`

Or run all backend tests from the backend folder:

- `mvn test`

Or run them using the IntelliJ run feature.

Current automated tests cover:

- Borrowing service (domain, application, web tests)
- Book service (domain service tests)
- User, Search, Notification, Config, and Eureka services (application context smoke tests)

## Circuit Breaker Failure Demo Checklist (Resilience)

Goal: reproducibly demonstrate fallback behavior when a critical dependency is unavailable.

Scenario: `borrowing-service` calling `user-service` via `UserServiceClient` with circuit breaker + fallback.

1. Start the full stack:
   - `docker compose up -d --build`
2. Verify both services are up:
   - `docker compose ps`
3. Stop user-service only:
   - `docker compose stop user-service`
4. Trigger borrowing flow that needs user validation:
   - `POST http://localhost:8082/api/borrows`
   - Body: `{"userId":1,"bookId":1}`
5. Expected behavior (fallback / fail-closed):
   - Borrow request is rejected (no successful borrow created).
   - `borrowing-service` stays responsive (no crash/hang).
6. Optional evidence capture:
   - `docker compose logs borrowing-service --since=2m`
   - Confirm fallback/circuit-breaker warning/error messages.
7. Recover dependency:
   - `docker compose start user-service`
   - Retry the borrow call and confirm normal behavior resumes.

## Traceable E2E Scenario Checklist (Given/When/Then)

Goal: trace one complete borrow lifecycle through API calls with verifiable state transitions.

Given:
- Stack is running (`docker compose up -d`).
- A valid active user exists (example: `userId=1`).
- A borrowable book exists with available copies (example: `bookId=1`).

When:
1. Borrow a book:
   - `POST http://localhost:8082/api/borrows`
   - Body: `{"userId":1,"bookId":1}`
2. Retrieve user borrows:
   - `GET http://localhost:8082/api/borrows/user/1`
3. Return the borrow record from step 1:
   - `PUT http://localhost:8082/api/borrows/{recordId}/return`
4. Re-check user borrows:
   - `GET http://localhost:8082/api/borrows/user/1`
5. Check book availability in book-service:
   - `GET http://localhost:8083/api/books/1/availability`

Then:
- Step 1 returns a created borrow with status `ACTIVE`.
- Step 2 includes that `recordId`.
- Step 3 returns status `RETURNED`.
- Step 4 shows the same record now in returned state (and fine data if applicable).
- Step 5 confirms available copies were decremented on borrow and restored on return.

Traceability notes:
- This scenario exercises inter-service communication between borrowing and book contexts.
- Related contract tests exist for client payload mapping in borrowing/book services.
