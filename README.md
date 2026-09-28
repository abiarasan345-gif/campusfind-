# CampusFind — Lost and Found Item Tracker

CampusFind is the refactored version of the uploaded Spring Boot + MySQL project. The old HealthRecords modules were removed and the existing Maven/Spring project structure was reused for the Campus Lost & Found requirements.

## Technology
- Java 17
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA / Hibernate
- MySQL
- HTML / CSS / Vanilla JavaScript
- Maven
- Jakarta Bean Validation
- BCrypt password hashing

## Architecture
`Frontend (HTML/CSS/JS) → REST Controller → Service → Repository → JPA/Hibernate → MySQL`

## Core features
- Student, Staff and Admin roles
- Database-backed registration and login
- Lost-item CRUD
- Found-item CRUD
- Category management
- Match suggestions using category, location and keywords
- Service-layer status rules
- Admin dashboard backed by database counts
- Search/filter endpoints
- Global exception handling and validation
- Responsive frontend with loading, empty, success and error states

## Demo accounts
Created automatically on first application start when the `users` table is empty:

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `admin123` |
| Staff | `staff1` | `staff123` |

New registrations are intentionally created as `STUDENT`; this prevents public registration from creating privileged Admin/Staff accounts.

## MySQL setup
1. Start MySQL.
2. Run `database/campusfind.sql` or simply create the database:

```sql
CREATE DATABASE IF NOT EXISTS campusfind;
```

3. Default connection is `root/root` on `localhost:3306`. Override with environment variables when needed:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

## Run
From the project root:

```powershell
mvn clean test
mvn spring-boot:run
```

Open:

`http://localhost:8080/`

The project also contains `mvnw.cmd` for Windows if Maven is installed through the wrapper environment.

## Main REST APIs
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `GET /api/categories`
- `POST /api/categories`
- `GET/POST/PUT/DELETE /api/lost-reports`
- `GET/POST/PUT/DELETE /api/found-items`
- `PATCH /api/found-items/{id}/status`
- `GET /api/matches`
- `POST /api/matches/confirm`
- `GET /api/admin/dashboard`

## Business rules enforced on the backend
1. `AVAILABLE → RETURNED` is rejected. A found item must go `AVAILABLE → CLAIMED → RETURNED`.
2. Only the reporting staff member or an Admin can change a found item's status or confirm its match.
3. Only authenticated users can create reports.
4. Required fields are validated with Jakarta Bean Validation.
5. Unknown IDs return HTTP 404.
6. Rule violations return HTTP 400/403; they are not silently written to MySQL.

## Testing
Automated unit tests are provided under `src/test/java` for matching logic and found-item status/authorization rules. Use:

```powershell
mvn clean test
```

The frontend is served by Spring Boot from `src/main/resources/static`, so browser API calls use the same-origin REST endpoints instead of fake JavaScript data.

See `docs/test-plan.md` for the endpoint and UI test matrix.
