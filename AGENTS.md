# AGENTS.md — Campus Resource Exchange

Project root: `/home/pavan/Java_Project_Class`
Source specification: `project.md`
Implementation plan: `IMPLEMENTATION_PLAN.txt`

## Project

Jakarta EE WAR for Tomcat 10.1: student-to-student academic resource/book exchange.
Java 17 bytecode, MySQL 8, JDBC/DAO, JSP/JSTL, BCrypt, image upload, Java
serialization backup, JUnit 5 + Mockito.

## Environment

- Use JDK 21 for Maven and Tomcat: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`
- Maven: `~/tools/apache-maven-3.9.12/bin/mvn`
- Tomcat: `~/tools/apache-tomcat-10.1.55`
- Git repository initialized on 2026-10-20 (`main` branch, single initial commit).
- MySQL app database/user: created via `scripts/setup-db.sh` (the user ran it on
  2026-10-20). Tomcat's `bin/setenv.sh` holds the credentials.
- Tomcat is stopped as of 2026-10-20 (end of verification). Start it with
  `~/tools/apache-tomcat-10.1.55/bin/startup.sh` before smoke testing.
- Secrets must stay outside the repository. Configure Tomcat with `DB_URL`,
  `DB_USER`, `DB_PASSWORD`, `UPLOAD_DIR`, `BACKUP_DIR` (see
  `config/application.properties.example`).

## Commands

Build:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$HOME/tools/apache-maven-3.9.12/bin:$PATH"
mvn clean package
```

Tests:

```bash
mvn clean test
```

Deploy (Tomcat 10.1):

```bash
cp target/campus-resource-exchange.war ~/tools/apache-tomcat-10.1.55/webapps/
~/tools/apache-tomcat-10.1.55/bin/startup.sh
~/tools/apache-tomcat-10.1.55/bin/shutdown.sh
```

Smoke test (needs Tomcat + MySQL running):

```bash
BASE_URL=http://localhost:8080/campus-resource-exchange bash scripts/smoke.sh
```

Database initialization (already done; re-run to reset the demo data):

```bash
bash scripts/setup-db.sh
# or manually:
sudo mysql < database/setup.sql
mysql -u campus_app -p campus_exchange < database/schema.sql
mysql -u campus_app -p campus_exchange < database/sample_data.sql
```

## Layout

- `src/main/java/com/campus/exchange/model/` — domain classes, enums, interfaces.
- `src/main/java/com/campus/exchange/dao/` — JDBC DAOs.
- `src/main/java/com/campus/exchange/service/` — business rules and transactions.
- `src/main/java/com/campus/exchange/controller/` — servlets.
- `src/main/java/com/campus/exchange/filter/SecurityFilter.java` — auth, roles, CSRF.
- `src/main/java/com/campus/exchange/util/` — config, validation, uploads, BCrypt, CSRF.
- `src/main/java/com/campus/exchange/backup/` — serializable backup DTOs.
- `src/main/webapp/WEB-INF/views/` — JSPs; fragments in `fragments/`.
- `src/main/webapp/assets/` — one CSS file and one JS file.
- `database/` — `setup.sql`, `schema.sql`, `sample_data.sql`.

## Conventions

- No Spring, JPA, ORM, Lombok, JS framework, or JSP scriptlets.
- JSP output uses `c:out`/`fn:escapeXml`; services re-check ownership/roles.
- SQL uses PreparedStatement; services own transaction boundaries.
- Keep the ponytail skill: simplest viable implementation, no extra abstractions.
- Keep `project.md` untouched.

## Progress (2026-10-20) — project complete

Done:

- Phase 0: portable Maven/Tomcat install; JDK 21 pinned.
- Phase 1: `pom.xml`, `web.xml`, package tree, `AppConfig`, `DbConnection`,
  context listener, `database/schema.sql`. `mvn clean package` builds a WAR.
- Phase 2/3: domain model, enums, interfaces, exceptions, utilities, DAOs,
  services, serializable backup DTOs.
- Phase 4/5/6/7 UI: servlets, `SecurityFilter`, JSP pages, fragments, CSS, JS,
  image upload/serving, admin pages, backup pages, error pages.
- `database/sample_data.sql` with generated BCrypt hashes.
- `database/setup.sql` and `scripts/setup-db.sh` for database/user creation.
- Phase 8: 7 test classes / 50 tests (JUnit 5 + Mockito), `README.md`,
  `scripts/smoke.sh`, live deployment, and end-to-end verification.
- MySQL database/user created; Tomcat `setenv.sh` configured.
- `mvn clean test` → 50 tests, 0 failures. WAR builds. All 22 JSPs compile.
- `scripts/smoke.sh` → 23 steps pass (run repeatedly, also after a restart).
- Verified manually: every page, 404 page, profile update, listing persistence
  across restart, restore of a deleted listing, duplicate-review rejection,
  request messages visible to the owner (incoming table + dashboard).

Fixed during verification:

- `BackupService` ObjectInputFilter rejected primitive arrays, and `BigDecimal`
  serializes through a `byte[]`; restore always failed. Now allows primitive
  array components.

Remaining optional work:

- Automated browser/UI tests (out of scope for v1; the smoke script and manual
  checklist cover the flows).

## Known TODOs / risks

- `database/setup.sql` contains a password placeholder; `scripts/setup-db.sh`
  writes the real password only to Tomcat's `bin/setenv.sh`.
- `mvn clean test` uses Mockito's inline mock maker, which self-attaches; the JDK
  prints a warning. Harmless on JDK 21.
- Re-running `database/schema.sql` drops all tables (documented in README).

## Must use Skills

 - Use ponytail skill and superpowers.
