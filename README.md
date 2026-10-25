# Campus Resource Exchange

A student-to-student exchange for academic resources — books, lab equipment, and
study material. Students list what they no longer need, browse and search the
catalogue, request an item, and complete a sale, donation, or loan. An
administrator verifies students, moderates listings, and can back up and restore
the catalogue.

Built with Jakarta EE (Servlet 6.0 / JSP 3.1 / JSTL 3.0), plain JDBC, and MySQL 8.
No Spring, no JPA/Hibernate, no ORM, no Lombok, and no JavaScript framework.

---

## 1. Features

| Area | What works |
|---|---|
| Accounts | Register, log in/out, BCrypt password hashing, session auth, profile editing |
| Verification | New students start `PENDING`; an admin verifies or rejects them; only verified, active students can list or request |
| Listings | Create/edit/deactivate, three resource types, three exchange types (`SELL`, `LEND`, `DONATE`), image upload |
| Discovery | Keyword/category/course/type/condition/price search, listing detail with owner reviews |
| Requests | Send, cancel, accept, reject; duplicate and self-requests blocked |
| Exchanges | Transaction created on acceptance; SELL/DONATE handover completion; LEND loan start and return |
| Reviews | Requester-only, after completion, 1–5 rating, one review per transaction |
| Admin | Dashboard stats, user verification/suspension, listing moderation, activity views |
| Backup | Java-serialization catalogue backup with allowlist filter, validation, additive restore |
| Security | Session auth, role checks in a filter *and* in services, CSRF on every POST, ownership checks, secure uploads, escaped output |

---

## 2. Technology

- Java 17 bytecode, built and run with JDK 21
- Jakarta Servlet 6.0, JSP 3.1, JSTL 3.0
- Apache Tomcat 10.1
- MySQL 8 (`mysql-connector-j` 9.1.0)
- JUnit 5 + Mockito for unit tests
- Maven WAR packaging

---

## 3. Project layout

```
pom.xml
config/application.properties.example     environment variable reference
database/setup.sql                        database + app user creation
database/schema.sql                       tables, indexes, constraints
database/sample_data.sql                  demo accounts and listings
scripts/smoke.sh                          curl end-to-end check
src/main/java/com/campus/exchange/
  model/        domain classes, enums, interfaces
  dao/          JDBC queries (PreparedStatement only)
  service/      business rules and transaction boundaries
  controller/   servlets
  filter/       SecurityFilter (auth, roles, CSRF)
  util/         config, DB, BCrypt, uploads, validation, flash, CSRF
  backup/       serializable backup DTOs
  exception/    AppException hierarchy
src/main/webapp/
  WEB-INF/views/  JSPs and fragments
  assets/         styles.css, app.js
src/test/java/    50 JUnit/Mockito tests
```

---

## 4. Prerequisites

- JDK 21 (`/usr/lib/jvm/java-21-openjdk-amd64`)
- Maven 3.9+
- Tomcat 10.1
- MySQL 8 running locally

The commands below assume this workspace layout:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$HOME/tools/apache-maven-3.9.12/bin:$PATH"
export CATALINA_HOME="$HOME/tools/apache-tomcat-10.1.55"
```

---

## 5. Database setup

The quickest path is the setup script. It prompts for a password, creates the
database and application user, loads the schema and demo data, and writes Tomcat's
`bin/setenv.sh` with the matching settings. Run it in an interactive terminal:

```bash
bash scripts/setup-db.sh
```

Or do it by hand. Pick a local password and keep it in your shell — never commit it.

```bash
DB_PASSWORD='choose-a-strong-local-password'

sudo mysql -e "CREATE DATABASE IF NOT EXISTS campus_exchange
                 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
               CREATE USER IF NOT EXISTS 'campus_app'@'localhost'
                 IDENTIFIED BY '$DB_PASSWORD';
               GRANT ALL PRIVILEGES ON campus_exchange.* TO 'campus_app'@'localhost';
               FLUSH PRIVILEGES;"

mysql -u campus_app -p"$DB_PASSWORD" campus_exchange < database/schema.sql
mysql -u campus_app -p"$DB_PASSWORD" campus_exchange < database/sample_data.sql
```

`database/setup.sql` contains the same statements with a `CHANGE_ME_STRONG_PASSWORD`
placeholder if you prefer to run a file; replace the placeholder first and use the
same value in `setenv.sh`. The application user only receives privileges on
`campus_exchange.*`.

Reset the demo data at any time:

```bash
mysql -u campus_app -p"$DB_PASSWORD" campus_exchange < database/schema.sql
mysql -u campus_app -p"$DB_PASSWORD" campus_exchange < database/sample_data.sql
```

> `schema.sql` drops and recreates its tables, so **all data is lost** on reset.

Upgrading an existing database (no reset) — apply the numbered scripts in
`database/migrations/` in order, for example:

```bash
mysql -u campus_app -p"$DB_PASSWORD" campus_exchange \
  < database/migrations/001_add_daily_essential.sql
```

---

## 6. Configuration

All configuration comes from environment variables read by
`com.campus.exchange.util.AppConfig`. Nothing secret is bundled in the WAR.

| Variable | Required | Purpose | Default |
|---|---|---|---|
| `DB_URL` | no | JDBC URL | `jdbc:mysql://localhost:3306/campus_exchange?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true` |
| `DB_USER` | no | database user | `campus_app` |
| `DB_PASSWORD` | **yes** | database password | — (startup fails without it) |
| `UPLOAD_DIR` | no | listing images | `~/campus-exchange/uploads` |
| `BACKUP_DIR` | no | `.ser` backup files | `~/campus-exchange/backups` |
| `DB_POOL_SIZE` | no | max pooled database connections (HikariCP) | `10` |

Put them in Tomcat's `bin/setenv.sh` (outside the repository):

```bash
cat > "$CATALINA_HOME/bin/setenv.sh" <<'EOF'
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export DB_URL='jdbc:mysql://localhost:3306/campus_exchange?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
export DB_USER='campus_app'
export DB_PASSWORD='THE_PASSWORD_YOU_CHOSE'
export UPLOAD_DIR="$HOME/campus-exchange/uploads"
export BACKUP_DIR="$HOME/campus-exchange/backups"
EOF
chmod +x "$CATALINA_HOME/bin/setenv.sh"
mkdir -p "$HOME/campus-exchange/uploads" "$HOME/campus-exchange/backups"
```

---

## 7. Build, test, deploy

One command does everything (build if stale, deploy, start, wait for a 200):

```bash
./run.sh            # or ./run.sh --skip-build / ./run.sh stop / ./run.sh smoke
```

By hand:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$HOME/tools/apache-maven-3.9.12/bin:$PATH"

mvn clean test        # unit tests, no MySQL required
mvn package           # target/campus-resource-exchange.war

cp target/campus-resource-exchange.war "$CATALINA_HOME/webapps/"
"$CATALINA_HOME/bin/startup.sh"
tail -f "$CATALINA_HOME/logs/catalina.out"    # wait for deployment to finish

# open http://localhost:8080/campus-resource-exchange/
```

Stop Tomcat with `"$CATALINA_HOME/bin/shutdown.sh"`. If `webapps/` already contains
an exploded directory from an earlier deploy, remove it first so the new WAR is
unpacked.

---

## 8. Demo accounts

Loaded by `database/sample_data.sql`. Development only — never reuse these
passwords anywhere real.

| Account | Password | Role | State |
|---|---|---|---|
| `admin@campus.test` | `Admin@12345` | Administrator | Active |
| `alice@campus.test` | `Student@123` | Student | Verified, active |
| `bob@campus.test` | `Student@123` | Student | Verified, active |
| `carol@campus.test` | `Student@123` | Student | **Pending** (use to demo verification) |

Sample listings and one pending request (Bob → Alice) are included.

To generate a BCrypt hash for a different password:

```bash
java -cp target/classes \
  com.campus.exchange.util.PasswordTool 'new-password'
```

---

## 9. End-to-end smoke test

With Tomcat running and the sample data loaded:

```bash
BASE_URL=http://localhost:8080/campus-resource-exchange bash scripts/smoke.sh
```

The script drives the real HTTP interface with `curl` and checks status codes and
page markers at every step. It creates unique students and listings per run, so it
is safe to repeat. `./run.sh smoke` runs it and then removes that data again on a
local database; to clean up by hand:

```bash
bash scripts/clean-smoke.sh
```

It covers:

1. Home page + database connectivity
2. Admin login
3. Student registration and admin verification
4. Listing creation with a PNG upload, plus image serving
5. Search, request creation, owner acceptance, transaction creation
6. SELL handover completion → resource `UNAVAILABLE`
7. Requester review + duplicate review rejection
8. LEND loan start → return → resource `AVAILABLE`
9. Role and CSRF enforcement (403/302)
10. Backup creation and restore

Any unexpected status or missing marker fails the run with a non-zero exit code.

---

## 10. Acceptance checklist

| # | Requirement | How it is demonstrated |
|---|---|---|
| 1 | Application starts with documented instructions | Sections 5–7; `catalina.out` shows deployment |
| 2 | New student can register | Smoke steps 3 and 5 |
| 3 | Student can log in and log out | Smoke step 4; logout link in header |
| 4 | Administrator can verify a student | Smoke steps 3 and 5 |
| 5 | Verified student can create a listing | Smoke steps 4 and 8 |
| 6 | Listing persists across a restart | Verified manually: create, restart Tomcat, reload |
| 7 | Student can upload a resource image | Smoke step 4 (upload + `GET /images/<uuid>.png`) |
| 8 | Another student can search and find it | Smoke step 5 |
| 9 | Second student can create a request | Smoke step 6 |
| 10 | Owner can accept or reject | Smoke step 6 (accept); reject uses the same page |
| 11 | Acceptance creates a transaction consistently | Smoke step 6; single transaction insert verified in `RequestServiceTest` |
| 12 | Sold/donated resource becomes unavailable | Smoke step 6 (`status-unavailable`) |
| 13 | Lent resource can be returned and made available | Smoke step 8 (`status-lent` → `status-available`) |
| 14 | Requester can review after completion | Smoke step 7 |
| 15 | Duplicate reviews and invalid transitions rejected | Smoke step 7; `ReviewServiceTest`, `TransactionServiceTest` |
| 16 | Unauthorized users cannot perform protected operations | Smoke step 9; services re-check ownership and roles |
| 17 | Backup and restore by an admin | Smoke step 10; `BackupServiceTest` |
| 18 | Required OOP concepts in working code | Section 11 |
| 19 | Project builds with Maven | `mvn clean package` |
| 20 | README explains setup, config, DB init, deployment, demo | This document |

---

## 11. OOP mapping

| Concept | Where |
|---|---|
| **Abstraction** | `User` (abstract) and `Resource` (abstract) define the shared contract; services depend on those types |
| **Inheritance** | `Student`/`Admin` extend `User`; `Book`/`LabEquipment`/`StudyMaterial` extend `Resource` |
| **Polymorphism** | `getDashboardPath()` and `getRoleLabel()` dispatch per user type; `displayDetails()` renders per resource type; `ResourceService.newResource(type)` builds the right subclass |
| **Interfaces** | `Sellable` (price rules), `Lendable` (loan terms/period), `Searchable` (`matches()` for catalogue search) |
| **Encapsulation** | Private fields with getters/setters; service rules normalize invalid combinations (e.g. `DONATE` clears price); DAOs never expose SQL |
| **Collections** | `List<Resource>`, `List<ResourceRequest>`, `List<Transaction>`, `List<Review>` across DAOs, services, and JSPs |
| **Exception handling** | `AppException` hierarchy (`Validation`, `NotFound`, `AccessDenied`, `Conflict`) with user-safe messages; servlets translate to flashes or error pages |
| **Serialization** | `ResourceCatalogBackup` / `ResourceBackupItem` with `ObjectInputFilter` allowlist and validation before restore |

---

## 12. Team contributions

**Shllok — Core application**
- Domain model, enums, interfaces (`model/`)
- OOP concepts: inheritance, polymorphism, abstraction, encapsulation
- Exception hierarchy (`exception/`)
- Business rules in services and their unit tests
- Collections usage and `displayDetails()`

**Pavan — Data management**
- `database/schema.sql`, `sample_data.sql`, `setup.sql`
- `util/AppConfig`, `util/DbConnection`
- JDBC DAOs, `PreparedStatement` usage, transaction handling
- File handling: `UploadUtil`, `ImageServlet`
- Serialization backup: `backup/`, `BackupService`

**Shelly — UI and web application**
- Servlets (`controller/`)
- `SecurityFilter`, sessions, CSRF
- JSPs, fragments, CSS, JavaScript
- Navigation, forms, dashboards, error pages
- Frontend/backend integration

Shared contracts: service signatures consumed by servlets, DAO signatures
consumed by services, and the enum/status transitions.

---

## 13. Design notes

**Transactions and concurrency.** `RequestService.acceptRequest` and
`TransactionService` open one JDBC connection with `autoCommit=false`, lock the
request and resource rows with `SELECT ... FOR UPDATE`, and use conditional
`UPDATE ... WHERE status = ?` statements. A second acceptance of the same listing
affects zero rows and is rolled back with a user-facing conflict message.

**CSRF.** `SecurityFilter` validates `_csrf` on every non-multipart POST before
authentication and role checks. Multipart listing forms are validated inside
`ResourceServlet` after parsing. Tokens are 32 random bytes, URL-safe Base64,
bound to the session, and rotated on login.

**Uploads.** Only JPEG/PNG/GIF up to 5 MB. The stored name is a random UUID with
the extension derived from the verified content type, never the submitted
filename. `ImageServlet` only serves names matching that pattern.

**Backup security.** Admin-only; files must match
`catalog-YYYYMMDD-HHMMSS-SSS.ser` and exist in `BACKUP_DIR`. Deserialization uses
an `ObjectInputFilter` allowlist (backup DTOs, `String`, `BigDecimal`, `ArrayList`,
primitive arrays) with depth/reference/length limits. Every item is validated
before the first insert, and the restore is additive: duplicates by owner + title
are skipped.

**Escaped output.** JSPs use `c:out`/`fn:escapeXml`; no scriptlets.

**Request messages.** The message on a request is optional. The owner sees it in
the incoming requests table and in the dashboard's "Received" list; an empty
message is shown explicitly as "No message provided." instead of a blank cell.

---

## 14. Deliberate limitations (v1)

- No online payments, chat, notifications, delivery, or AI recommendations.
- `OVERDUE` status and reminders are not implemented.
- A transaction cannot be cancelled after acceptance.
- No "Other" resource type; no listing reactivation flow.
- No email verification, password reset, or email notifications.
- Backups are not downloadable through the browser.
- Catalogue lists are paginated (12 per page); admin lists still are not.
- One image per listing; no thumbnails or cropping.
- Login is rate limited in memory (5 failures per email per 15 minutes), not
  across restarts or multiple Tomcat instances.
- No automated browser/UI tests; `scripts/smoke.sh` + the manual checklist cover
  the flows. GitHub Actions runs the unit tests and the smoke test (`ci.yml`).

---

## 15. Verification results (2026-10-20)

Environment: JDK 21, Maven 3.9.12, Tomcat 10.1.55, MySQL 8.4.11.

Build and unit tests:

- `mvn clean test` — **50 tests, 0 failures, 0 errors** (7 test classes).
- `mvn package` — WAR produced at `target/campus-resource-exchange.war`.
- All **22 JSPs compiled** with Tomcat's Jasper `JspC` (0 errors).
- Deployment log: `Database connection verified at startup.`

Live end-to-end (`scripts/smoke.sh`) — **23 steps passed**, run twice, including
once after a Tomcat restart:

- registration, admin verification, login/logout, role and CSRF enforcement
- SELL listing created with a PNG upload; image served as `image/png`
- catalogue search, request creation, owner acceptance → transaction created;
  the owner's incoming page and dashboard both show the requester's message
- handover completion → resource `status-unavailable`
- requester review saved; duplicate review rejected with an error flash
- LEND loan start → `status-lent`, return → `status-available`
- catalogue backup created; restore reported success

Additional manual checks:

- Every remaining page rendered HTTP 200: dashboard, my listings, edit form,
  outgoing/incoming requests, transactions, profile, admin dashboard, filtered
  user list, listing moderation, activity, backup.
- Unknown subpath returned the 404 page.
- Profile update showed its flash message.
- A listing created before a Tomcat restart was still present afterwards.
- A listing was deleted with SQL and restored from a backup:
  `Backup restored. 1 listing(s) added; duplicates were skipped.`

Bug found and fixed during this verification:

- `BackupService`'s `ObjectInputFilter` rejected primitive arrays. `BigDecimal`
  serializes its value through a `byte[]`, so **restore always failed** for a
  catalogue containing a price. The filter now allows primitive array
  components; the `BackupServiceTest` round-trip test covers it.

Refinement after the first run:

- Owner-side request messages are now unambiguous: the dashboard's "Received"
  list shows the message, the incoming table labels empty ones "No message
  provided.", and `scripts/smoke.sh` asserts the owner sees the message text.
  Re-verified with a full 23/23 smoke pass and 50/50 unit tests.

---

## 16. Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `DB_PASSWORD is not configured` in logs | `setenv.sh` is missing or not executable; restart Tomcat after fixing |
| 500 on every page | MySQL down, wrong credentials, or schema not loaded — check `catalina.out` |
| 404 for the app root | WAR not deployed yet or context is still starting; check `webapps/` and logs |
| Login says "Invalid email or password" for demo accounts | Sample data not loaded, or a previous `schema.sql` reset removed it |
| Upload rejected | Image over 5 MB, not JPEG/PNG/GIF, or corrupt |
| `The form expired` 403 | CSRF token missing/stale — reload the page and resubmit |
| Port 8080 in use | Change the connector port in `conf/server.xml` and update `BASE_URL` |
