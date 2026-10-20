PROJECT: Peer-to-Peer Academic Resource & Book Exchange

YOUR ROLE

Act as a senior Java full-stack developer, software architect, database
designer, and testing engineer.

Your task is to design, implement, test, and document a complete working
Java full-stack web application.

Do not stop at generating a plan, diagrams, pseudocode, or sample snippets.
You must create the actual project files, implement the application, run
the available build and tests, fix errors, and document how to run it.

Inspect the existing repository and development environment before making
changes. Preserve useful existing work. If the directory is empty, create
the project structure from scratch.

Make sensible decisions without asking questions for every minor detail.
If an essential requirement cannot be implemented because a dependency,
configuration, or service is unavailable, explain the blocker accurately
and continue with all work that can be completed.


========================================================
1. PROJECT DEFINITION
========================================================

Project name:
Peer-to-Peer Academic Resource & Book Exchange

Short name:
Campus Resource Exchange

Project type:
Java Full-Stack Web Application

Problem being solved:

Students often own textbooks, laboratory equipment, calculators,
electronic components, and other academic resources that they no longer
need after completing a course. Other students may need these same
resources but find them expensive or difficult to obtain.

There is no centralized campus platform that connects students who have
academic resources with students who need them, while supporting
requests, verification, exchange tracking, and reviews.

Proposed solution:

Develop a campus-focused web application where students can list, search,
buy, sell, lend, donate, and request academic resources.

The application will allow students to discover resources by keyword,
category, condition, price, course code, exchange type, and pickup
location.

Students can request listed resources, owners can accept or reject the
requests, accepted requests can become transactions, and completed
exchanges can receive reviews.

An administrator verifies student accounts and moderates the platform.

Primary objective:

Create a functional, database-backed campus resource exchange
application that demonstrates the Java concepts and technologies
required by an undergraduate Java full-stack assignment.


========================================================
2. SCOPE AND LIMITATIONS
========================================================

Implement these features:

- Student registration and login.
- Session-based authentication and logout.
- Student and administrator roles.
- Student profile management.
- Administrator verification of students.
- Academic resource listings.
- Resource image uploads.
- Search, filtering, and browsing.
- Resource requests.
- Request acceptance and rejection.
- Transaction tracking.
- Lending and resource-return tracking.
- Reviews and ratings.
- MySQL persistence through JDBC.
- File handling.
- Java serialization and deserialization.
- Java OOP, inheritance, polymorphism, abstraction, interfaces,
  exception handling, and collections.
- Responsive JSP-based user interface.
- Documentation and tests.

Explicitly exclude these features from version 1:

- Online payments or payment gateway integration.
- Real-time chat.
- External university identity verification APIs.
- Real-time notifications.
- AI recommendations.
- Complex delivery or logistics systems.
- Unnecessary microservices or distributed architecture.

Students will arrange exchanges and campus pickup themselves. The
application will record and track the exchange, not process payments.


========================================================
3. REQUIRED TECHNOLOGY STACK
========================================================

Use the following stack:

- Java 17.
- Maven.
- Apache Tomcat 10.1.
- Jakarta Servlet API compatible with Tomcat 10.1.
- JSP compatible with Tomcat 10.1.
- HTML5.
- CSS3.
- Vanilla JavaScript where needed.
- MySQL 8.x.
- JDBC.
- DAO pattern.
- HttpSession for sessions.
- JUnit for unit tests.
- BCrypt for password hashing.

Use Jakarta Servlet/JSP namespaces compatible with the chosen Tomcat
version. Do not mix incompatible javax.servlet and jakarta.servlet APIs.

Build a Maven WAR application deployable to Apache Tomcat.

Do not use Spring Boot, Spring MVC, JPA, Hibernate, Node.js for the
backend, or another framework that replaces the required JSP, Servlet,
and JDBC implementation.

JSP must be used for the actual web interface. Servlets must receive
and process HTTP requests. JDBC must perform the actual database
operations.

Use JSP Expression Language and JSTL where appropriate. Avoid embedding
Java business logic in JSP scriptlets.

Inspect the installed Java, Maven, MySQL, and Tomcat environments.
Document any missing prerequisites rather than pretending that they
are installed.


========================================================
4. APPLICATION ARCHITECTURE
========================================================

Use a clear separation of responsibilities.

PRESENTATION LAYER:
- JSP pages.
- HTML/CSS.
- Small amounts of JavaScript.
- Forms and user feedback.
- Resource browsing and dashboards.

WEB/CONTROLLER LAYER:
- Java Servlets.
- HTTP request and response handling.
- Session management.
- Role-based access checks.
- Form handling and input validation.
- Navigation and redirects.

BUSINESS LOGIC LAYER:
- Java domain classes.
- Services implementing application rules.
- Resource availability validation.
- Request processing.
- Transaction lifecycle.
- Review eligibility.
- Exception handling.

DATA ACCESS LAYER:
- DAO classes.
- JDBC.
- PreparedStatement.
- MySQL CRUD operations.
- Database transactions for multi-step operations.

FILE HANDLING:
- Secure resource image uploads.
- Image storage and controlled image retrieval.

SERIALIZATION:
- Administrator-controlled resource catalog backup and restoration.

The main request flow should be:

Student/Admin
    -> JSP interface
    -> Servlet
    -> Service/business logic
    -> DAO
    -> JDBC
    -> MySQL

Responses should return to the user through the appropriate Servlet
and JSP flow.

The interface, business logic, and DAO code must not be combined into
one large class.


========================================================
5. AUTHENTICATION AND AUTHORIZATION
========================================================

Implement:

- Registration.
- Login.
- Logout.
- Student profiles.
- Administrator authentication.
- Session management.
- Role-based access control.
- Student verification status.

Student registration fields:

- Full name.
- Email.
- Student ID.
- Password.
- Department.
- Year of study.

Use unique constraints for email and student ID.

Hash passwords with BCrypt. Never store plaintext passwords.

On successful login:

- Verify the supplied password against the stored password hash.
- Regenerate the session ID to reduce session fixation risk.
- Store the authenticated user ID and role in the session.
- Redirect to the correct dashboard.

On logout:

- Invalidate the session.
- Redirect to the login page.

Protect student-only and administrator-only routes with appropriate
Servlet filters or an equivalent centralized authorization mechanism.

Unauthenticated users must not access protected operations.

Students must not modify another student's listings, requests,
transactions, profile, or reviews.

Allow unverified students to browse listings, but require verified
student status before creating listings or submitting requests.

Do not trust user IDs, roles, prices, statuses, or ownership supplied
by the browser. Validate sensitive values on the server.

Provide a documented method to create the first administrator account.
Do not hardcode production credentials or store unencrypted passwords.

For local development, provide a convenient, documented way to create
sample accounts without weakening security.


========================================================
6. RESOURCE MANAGEMENT
========================================================

Create an abstract Resource class.

Required resource types:

- Book.
- LabEquipment.
- StudyMaterial.
- Other, if needed for listings that do not fit the main categories.

Each resource listing should include:

- Resource ID.
- Owner ID.
- Title.
- Description.
- Category.
- Resource type.
- Condition.
- Exchange type.
- Price.
- Lending terms, where applicable.
- Course code, where applicable.
- Campus pickup location.
- Optional image path.
- Status.
- Creation timestamp.
- Update timestamp.

Conditions:

- NEW.
- LIKE_NEW.
- GOOD.
- FAIR.

Exchange types:

- SELL.
- LEND.
- DONATE.

For donations, the price should be zero or null according to the
chosen schema. Validate exchange-specific fields consistently.

Resource availability statuses may include:

- AVAILABLE.
- RESERVED.
- LENT.
- UNAVAILABLE.
- INACTIVE.

Implement:

- Create listing.
- View listing details.
- Edit one's own listing.
- Delete or deactivate one's own listing.
- Browse available listings.
- Search by keyword.
- Filter by category.
- Filter by type.
- Filter by exchange type.
- Filter by condition.
- Filter by price.
- Filter by course code.
- Filter by pickup location.
- Display the owner's relevant public profile information.

Prevent users from editing listings owned by other students.

When a resource is reserved or unavailable, the application must not
allow new active requests for it.


========================================================
7. RESOURCE IMAGE FILE HANDLING
========================================================

Allow students to upload an optional image when creating or editing
a resource listing.

Requirements:

- Validate allowed image formats.
- Validate file size.
- Generate server-controlled filenames.
- Do not trust a filename supplied by the browser.
- Prevent path traversal.
- Store files outside the public web application directory.
- Save only a safe relative path or stored filename in the database.
- Provide controlled image retrieval.
- Handle missing image files gracefully.

Do not allow arbitrary file uploads or arbitrary server-file access.

Document the upload directory and its configuration.


========================================================
8. RESOURCE REQUESTS
========================================================

Create a ResourceRequest domain class. Prefer this name over the
generic class name Request to avoid confusion with HttpServletRequest.

Students must be able to:

- Request an available resource.
- Add an optional message to the request.
- View their outgoing requests.
- View incoming requests for their own listings.
- Cancel eligible pending requests.

Owners must be able to:

- Accept pending requests.
- Reject pending requests.

Request statuses:

- PENDING.
- ACCEPTED.
- REJECTED.
- CANCELLED.
- COMPLETED.

Business rules:

- Students cannot request their own resources.
- Duplicate active requests for the same requester and resource
  must be prevented.
- Unverified students cannot submit requests.
- Unavailable resources cannot be requested.
- Only the resource owner can accept or reject incoming requests.
- Only the requester may cancel their eligible pending request.
- Concurrent requests must not incorrectly reserve a resource twice.

Use appropriate database constraints, transactions, or locking to
protect critical state changes.

Give users clear success and error messages.


========================================================
9. TRANSACTION MANAGEMENT
========================================================

When an owner accepts a request:

1. Validate that the request is still pending.
2. Confirm that the resource is still available.
3. Mark the request as accepted.
4. Create the associated transaction.
5. Reserve the resource.
6. Ensure the changes happen consistently in a database transaction.

The Transaction domain class should capture:

- Transaction ID.
- Request ID.
- Owner ID.
- Requester ID.
- Exchange type.
- Current status.
- Created timestamp.
- Completion timestamp, when applicable.
- Return timestamp, when applicable.

Use statuses appropriate to the exchange lifecycle, such as:

- PENDING_HANDOVER.
- ACTIVE_LOAN.
- COMPLETED.
- CANCELLED.
- OVERDUE, if implemented.

For selling or donating:

- Record the exchange.
- Allow the parties to mark the handover complete.
- Mark the resource unavailable after completion.

For lending:

- Record that the resource has been lent.
- Track the active loan.
- Allow the owner to record its return.
- Mark the transaction completed after return.
- Make the resource available again once returned, provided the
  owner has not deactivated the listing.

Display transaction history to the relevant participants.

Only transaction participants may view or update their transactions.

Validate every status transition on the server.


========================================================
10. REVIEWS AND RATINGS
========================================================

Allow a requester to review a transaction after it has been completed.

A review contains:

- Review ID.
- Transaction ID.
- Reviewer ID.
- Rating from 1 to 5.
- Optional comment.
- Creation timestamp.

Requirements:

- Rating must be between 1 and 5.
- A review cannot be submitted for an incomplete transaction.
- A requester may submit at most one review per transaction.
- Only an authorized participant may review a transaction.
- Review ownership must be validated on the server.
- Reviews should be visible on appropriate resource or user pages.

Use a database uniqueness constraint as well as application-level
validation to prevent duplicate reviews.


========================================================
11. ADMINISTRATION
========================================================

Provide an administrator dashboard.

Implement:

- List registered users.
- View student verification status.
- Verify or reject student verification.
- Manage user status where appropriate.
- Browse and moderate resource listings.
- View requests and transactions.
- View reviews.
- View useful summary statistics.

Do not grant administrator access merely because a user modifies
a role value in an HTTP request.

Student verification is manual in version 1. Do not claim integration
with a real university verification service.


========================================================
12. REQUIRED JAVA DOMAIN CLASSES AND OOP
========================================================

Implement these principal domain classes:

1. User (abstract).
2. Student.
3. Admin.
4. Resource (abstract).
5. Book.
6. LabEquipment.
7. StudyMaterial.
8. ResourceRequest.
9. Transaction.
10. Review.

Implement these interfaces:

- Sellable.
- Lendable.
- Searchable.

You may add service, DAO, exception, filter, Servlet, backup DTO,
and utility classes as needed.

The ten listed classes are the core domain model, not a restriction
on the total number of Java files.

Use OOP meaningfully:

ENCAPSULATION:
- Private fields.
- Appropriate constructors.
- Getters and setters where needed.
- Avoid exposing mutable internal state unnecessarily.

INHERITANCE:
- Student and Admin extend User.
- Book, LabEquipment, and StudyMaterial extend Resource.

POLYMORPHISM:
- Resource subclasses provide appropriate implementations of common
  operations such as displayDetails().
- Demonstrate calls through a Resource reference.

ABSTRACTION:
- User and Resource are abstract where appropriate.

INTERFACES:
- Implement Sellable, Lendable, and Searchable with meaningful behavior.
- Keep interface design sensible for the resource exchange domain.
- Do not create unused interfaces only to satisfy a checklist.

EXCEPTION HANDLING:
- Handle invalid input, missing resources, unavailable resources,
  invalid state transitions, duplicate requests, and authorization errors.
- Create meaningful custom exceptions where beneficial.
- Do not expose internal stack traces or SQL errors to normal users.

COLLECTIONS:
- Use ArrayList/List for collections of resources, requests, and records.
- Use HashMap/Map where key-based lookup or aggregation is useful.
- Use HashSet/Set where uniqueness is required in application logic.
- Use these structures in real application operations, not merely
  as unused demonstrations.

LAMBDA EXPRESSIONS, GENERICS, ITERATORS, AND COMPARATORS may be used
where appropriate, but prioritize the required core functionality.

Write clear, readable Java code with suitable comments.


========================================================
13. MYSQL DATABASE
========================================================

Use MySQL as the persistent source of truth.

Create these principal tables:

A. users

Suggested fields:
- user_id primary key.
- full_name.
- email unique.
- password_hash.
- student_id unique.
- department.
- year_of_study.
- role.
- verification_status.
- created_at.
- updated_at.

B. resources

Suggested fields:
- resource_id primary key.
- owner_id foreign key to users.
- title.
- description.
- category.
- resource_type.
- condition.
- exchange_type.
- price.
- lending_terms.
- course_code.
- pickup_location.
- image_path.
- status.
- created_at.
- updated_at.

C. resource_requests

Suggested fields:
- request_id primary key.
- resource_id foreign key to resources.
- requester_id foreign key to users.
- message.
- status.
- created_at.
- updated_at.

D. exchange_transactions

Suggested fields:
- transaction_id primary key.
- request_id unique foreign key to resource_requests.
- owner_id foreign key to users.
- requester_id foreign key to users.
- exchange_type.
- status.
- created_at.
- completed_at.
- returned_at, where applicable.

E. reviews

Suggested fields:
- review_id primary key.
- transaction_id unique foreign key to exchange_transactions.
- reviewer_id foreign key to users.
- rating.
- comment.
- created_at.

Adjust the final schema as needed to support the actual workflow, but
preserve the five principal entities and their relationships.

Define appropriate data types, foreign keys, indexes, uniqueness
constraints, and status constraints.

Important relationships:

- One student can own many resources.
- One student can create many requests.
- One resource can receive many requests.
- One accepted request can create at most one transaction.
- A completed transaction can have at most one review from the requester.

Use appropriate foreign-key delete behavior so that historical
transactions and reviews are not accidentally destroyed.

Provide:

- database/schema.sql.
- database/sample_data.sql, if useful.
- Clear instructions for creating the database and database user.

Use explicit column names; avoid using ambiguous SQL keywords as
table names.

Use PreparedStatement for all SQL parameters. Never build SQL queries
by concatenating untrusted input.

Use try-with-resources for JDBC connections, statements, and result
sets where appropriate.

Use database transactions for operations such as accepting a request
and creating its associated transaction.


========================================================
14. JDBC AND DAO DESIGN
========================================================

Separate database access from business logic.

Implement suitable DAO classes, such as:

- UserDAO.
- ResourceDAO.
- ResourceRequestDAO.
- TransactionDAO.
- ReviewDAO.

Additional DAO classes are allowed when useful.

Create corresponding services, such as:

- AuthService.
- ResourceService.
- RequestService.
- TransactionService.
- ReviewService.
- AdminService.
- BackupService.

Each service must enforce relevant business rules before calling
the data-access layer.

Servlets must not contain large SQL statements or duplicate business
rules.

DAOs must not render HTML.

Return proper domain objects or collections of domain objects.

Handle missing records and database errors appropriately.


========================================================
15. FILE HANDLING AND SERIALIZATION
========================================================

File handling is a mandatory coursework requirement.

Implement resource image upload and retrieval as described above.

Serialization/deserialization is also mandatory.

Implement an administrator-only resource catalog backup using Java
serialization.

Requirements:

- Create a serializable backup DTO, such as ResourceCatalogBackup.
- Include resource listing information needed for restoration.
- Exclude credentials and password hashes.
- Write backups under a configured server-side backup directory.
- Use ObjectOutputStream to create backup files.
- Use ObjectInputStream to read trusted application-generated backups.
- Apply a deserialization class allowlist/filter.
- Validate backup contents before applying a restore.
- Restore data through the normal service/DAO layers.
- Handle invalid, corrupted, or incompatible backup files gracefully.
- Prevent users from supplying arbitrary paths for backup files.

Provide a small automated test or demonstrable test procedure for
serialization and deserialization.

Serialization should complement the MySQL database, not replace
MySQL as the primary persistence mechanism.


========================================================
16. FRONTEND AND USER EXPERIENCE
========================================================

Build a polished, responsive academic marketplace interface.

The interface should feel like a real campus product rather than
a basic HTML assignment.

Use a consistent design system:

- Clean white or light background.
- Dark blue and teal accents.
- Readable typography.
- Consistent spacing.
- Responsive cards and forms.
- Accessible contrast.
- Clear status labels.
- Useful empty states.
- Proper success and error messages.
- Consistent navigation.
- Mobile and desktop layouts.

Avoid unnecessary animation and excessive decorative elements.

Required pages include:

- Home or landing page.
- Register.
- Login.
- Student dashboard.
- Resource catalog.
- Resource details.
- Add resource.
- Edit resource.
- My listings.
- My outgoing requests.
- Incoming requests.
- Transaction history/details.
- Review form.
- Profile.
- Admin dashboard.
- User verification management.
- Resource moderation.

You may combine related pages if the navigation remains clear.

Use real database data on every functional page. Do not use hardcoded
arrays, static mock listings, or frontend-only operations as substitutes
for backend functionality.

Use JavaScript only for appropriate user-interface interactions.
Critical validation and business rules must be enforced on the server.


========================================================
17. CONFIGURATION AND SECURITY
========================================================

Do not hardcode database passwords or deployment secrets.

Provide an example configuration file and/or environment variable
configuration.

Support configuration of:

- Database URL.
- Database username.
- Database password.
- Resource upload directory.
- Backup directory.

Provide clear local-development instructions.

Additional requirements:

- PreparedStatement for SQL parameters.
- BCrypt password hashing.
- Session checks on protected routes.
- Authorization checks on every protected operation.
- Server-side validation.
- Safe filename generation.
- File upload validation.
- Safe error handling.
- No password hashes in serialized backups.
- No sensitive credentials in logs.
- No committed production secrets.

Do not report successful authentication or database persistence unless
the corresponding operation actually succeeded.


========================================================
18. PROJECT STRUCTURE
========================================================

Create a maintainable Maven web application.

Use an organized package structure similar to:

src/main/java/
    .../model/
    .../dao/
    .../service/
    .../controller/
    .../filter/
    .../exception/
    .../util/
    .../backup/

src/main/webapp/
    .../WEB-INF/views/
    .../assets/css/
    .../assets/js/

database/
    schema.sql
    sample_data.sql

src/test/java/
    ...

Also provide:

- pom.xml.
- README.md.
- .gitignore.
- Example configuration.
- Tests.
- Architecture documentation, where useful.

Choose a consistent package name and apply it throughout the project.

JSP files that should not be accessed directly must be placed behind
WEB-INF and reached through controller Servlets.

Store uploads and backups outside the deployable WAR wherever practical.


========================================================
19. TEAM CONTRIBUTIONS
========================================================

This is a three-member academic team.

Member 1: Shllok — Core Application.
Responsibilities:
- Java domain classes.
- OOP.
- Inheritance.
- Polymorphism.
- Interfaces.
- Exception handling.
- Collections.
- Business rules.

Member 2: Pavan — Data Management.
Responsibilities:
- MySQL database.
- JDBC connectivity.
- DAO implementation.
- Database CRUD operations.
- File handling.
- Serialization/deserialization.
- Backup/restore integration.

Member 3: Shelly — UI and Web Application.
Responsibilities:
- JSP pages.
- Frontend.
- Servlets.
- Request/response handling.
- Session management.
- Navigation.
- Frontend/backend integration.

Organize the code and documentation so these responsibilities can
be understood and divided across team members.

Because these modules depend on each other, define consistent
interfaces between the controller, service, and DAO layers.

All three members should be able to integrate and test their modules.

Include this contribution plan in the README.


========================================================
20. TESTING REQUIREMENTS
========================================================

Create automated tests for important business rules.

At minimum, test:

- Valid and invalid resource data.
- Requesting one's own resource is rejected.
- Duplicate active requests are rejected.
- Unavailable resources cannot be requested.
- Only the owner may accept or reject their incoming requests.
- A second acceptance cannot reserve a resource twice.
- Invalid transaction transitions are rejected.
- Lending and return status changes work correctly.
- Reviews cannot be submitted before transaction completion.
- Duplicate reviews are rejected.
- Users cannot modify resources they do not own.
- Serialization/deserialization produces a valid resource backup.
- Invalid backup content is rejected.

Write the tests so unit tests can run without requiring a live MySQL
server wherever practical. For database-dependent tests, clearly
document the required test database.

Run:

mvn clean test
mvn package

Fix compilation errors, test failures, broken imports, and dependency
problems where possible.

Do not claim that a test passed unless it was actually executed and
passed.

If an external dependency or service prevents an integration test,
explain the issue and run all remaining feasible checks.


========================================================
21. ACCEPTANCE CRITERIA
========================================================

The project is considered complete when the following workflow works:

1. The application starts using the documented instructions.
2. A new student can register.
3. A student can log in and log out.
4. An administrator can verify a student.
5. The verified student can create a resource listing.
6. The listing is stored in MySQL and survives an application restart.
7. The student can upload a resource image.
8. Another student can search and find the resource.
9. The second student can create a request.
10. The owner can accept or reject the request.
11. Acceptance creates a corresponding transaction consistently.
12. A sold or donated resource becomes unavailable when the exchange
    is completed.
13. A lent resource can be marked as returned and made available again.
14. An eligible requester can submit a review after completion.
15. Duplicate reviews and invalid state transitions are rejected.
16. Unauthorized users cannot perform protected operations.
17. Resource data can be serialized into a backup and restored through
    the documented administrator workflow.
18. The required OOP concepts are represented in working Java code.
19. The project builds using Maven.
20. The README accurately explains setup, configuration, database
    initialization, deployment, and the end-to-end demonstration.


========================================================
22. IMPLEMENTATION WORKFLOW
========================================================

Work in the following order.

PHASE 1 — INSPECTION AND DESIGN

- Inspect the current repository.
- Check the installed Java, Maven, MySQL, and Tomcat versions where
  possible.
- Identify existing files and avoid overwriting unrelated work.
- Define the package structure.
- Confirm the database schema and state transitions.
- Write a short implementation checklist.

Do not stop after producing the checklist. Continue implementing.

PHASE 2 — PROJECT FOUNDATION

- Configure Maven.
- Configure the WAR packaging.
- Create the package structure.
- Create the database schema.
- Configure database access without hardcoded credentials.
- Verify that the project compiles.

PHASE 3 — DOMAIN AND DATA LAYER

- Implement domain classes and interfaces.
- Implement validation and custom exceptions.
- Implement DAO classes and JDBC connectivity.
- Implement services and business rules.
- Write unit tests for the core domain and workflow.

PHASE 4 — WEB LAYER

- Implement Servlets.
- Implement authentication and authorization.
- Implement session handling.
- Implement JSP pages and navigation.
- Connect forms and pages to real backend operations.

PHASE 5 — COMPLETE WORKFLOWS

- Implement resource listing and searching.
- Implement requests and owner decisions.
- Implement transaction state changes.
- Implement lending and returns.
- Implement reviews.
- Implement administrator functionality.

PHASE 6 — FILES AND SERIALIZATION

- Implement image upload and retrieval.
- Implement resource catalog backup.
- Implement validated restore functionality.
- Test invalid files and backup handling.

PHASE 7 — USER EXPERIENCE

- Finish responsive styles.
- Improve error messages and empty states.
- Confirm every navigation link and form works.

PHASE 8 — TEST AND DOCUMENT

- Run Maven tests.
- Build the WAR.
- Fix discovered problems.
- Create or update the README.
- Document environment configuration.
- Document database initialization.
- Explain how to deploy to Tomcat.
- Provide test accounts or a safe sample-data setup.
- List all implemented modules.
- List any genuine limitations that remain.

Do not mark unimplemented features as complete.

Prioritize working functionality, readable Java code, reliable database
operations, and assignment compliance over unnecessary complexity.


========================================================
23. FINAL RESPONSE REQUIRED FROM YOU
========================================================

After implementation, report:

1. A summary of the implemented application.
2. The project directory structure.
3. The technologies used.
4. The database tables and relationships.
5. Where the required OOP concepts are implemented.
6. How file handling works.
7. How serialization and deserialization work.
8. The commands to configure the database and run the project.
9. The exact build and test results.
10. A short end-to-end demonstration checklist.
11. Any incomplete features or environment-dependent limitations.

Do not merely claim that everything is complete. Report what was
actually implemented and tested.

BEGIN BY INSPECTING THE REPOSITORY AND ENVIRONMENT, THEN BUILD THE
COMPLETE PROJECT THROUGH THE PHASES ABOVE.
