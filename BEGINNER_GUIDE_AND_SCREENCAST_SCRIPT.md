# Complete Beginner Guide & Screencast Script
### CBD Assignment 2 — Task Manager Spring Boot Application

---

# PART 1 — UNDERSTANDING THE PROJECT

## What Did We Build?

We built a **Task Manager REST API** — think of it like a digital to-do list app for teams.
It runs as a web service (like a mini-website with no front-end) that receives requests and sends back data.

### Real-World Analogy
Imagine you are managing a construction project:
- A **Project** is the building you are constructing (e.g., "New Hospital Wing")
- A **Task** is a specific job within that project (e.g., "Install plumbing on Floor 2")
- Each task has a **status**: OPEN → IN_PROGRESS → DONE (or BLOCKED)
- Each task has a **priority**: LOW, MEDIUM, HIGH, or CRITICAL

Our application lets you create projects, add tasks to them, update task statuses, and enforces business rules (like: you cannot delete a project if it has tasks in progress).

---

## How is the Application Structured? (The Layers)

Think of the application like a restaurant:

```
[ CLIENT / POSTMAN ] → makes a request (like a customer placing an order)
        ↓
[ CONTROLLER LAYER ] → receives the request and routes it (like a waiter)
        ↓
[ SERVICE LAYER ]    → applies business rules and logic (like the chef)
        ↓
[ REPOSITORY LAYER ] → reads/writes to the database (like the kitchen storage)
        ↓
[ DATABASE (H2) ]    → stores all the data (like the pantry)
```

### The Files We Created

**Main Application Files (src/main/java/):**

| File | What It Does |
|------|-------------|
| `TaskManagerApplication.java` | The main entry point — starts the whole application |
| `model/Project.java` | Defines what a Project looks like (id, name, description) |
| `model/Task.java` | Defines what a Task looks like (id, title, status, priority, dueDate) |
| `model/TaskStatus.java` | The allowed statuses: OPEN, IN_PROGRESS, DONE, BLOCKED |
| `model/TaskPriority.java` | The allowed priorities: LOW, MEDIUM, HIGH, CRITICAL |
| `repository/ProjectRepository.java` | Handles saving/reading Projects from the database |
| `repository/TaskRepository.java` | Handles saving/reading Tasks from the database |
| `service/ProjectService.java` | Business rules for Projects (e.g. no duplicate names) |
| `service/TaskService.java` | Business rules for Tasks (e.g. no past due dates, cannot reopen a DONE task) |
| `controller/ProjectController.java` | HTTP endpoints for Projects (POST, GET, DELETE) |
| `controller/TaskController.java` | HTTP endpoints for Tasks (POST, GET, PATCH, DELETE) |
| `exception/GlobalExceptionHandler.java` | Catches errors and returns proper HTTP error messages |
| `dto/` folder | Data Transfer Objects — the shape of data sent to/from clients |
| `mapper/` folder | Converts between database objects and DTO objects |

---

## What is Spring Boot?

Spring Boot is a **Java framework** that makes it easy to build web applications and REST APIs.

- **Without** Spring Boot: you would need to write hundreds of lines of setup code
- **With** Spring Boot: it auto-configures almost everything for you

When you run `mvn spring-boot:run`, Spring Boot:
1. Starts an embedded web server (Tomcat) on port 8080
2. Creates all the database tables automatically
3. Wires all the layers together automatically
4. Makes your API available at `http://localhost:8080`

---

## What is a REST API?

A **REST API** is a way for programs to talk to each other using HTTP (the same protocol your browser uses).

Instead of clicking buttons on a website, you send **requests** like:

| Action | HTTP Method | URL Example | What it does |
|--------|-------------|------------|--------------|
| Create | POST | `/api/projects` | Creates a new project |
| Read | GET | `/api/projects/1` | Gets project with ID 1 |
| Update | PATCH | `/api/tasks/1/status?status=DONE` | Marks task 1 as done |
| Delete | DELETE | `/api/projects/1` | Deletes project 1 |

**Postman** is the tool we use to send these requests visually (like a browser but for APIs).

---

## What is the Database?

We use **H2** — an in-memory database.

- "In-memory" means the data lives in RAM while the app runs
- When you stop the application, the data is cleared
- This is perfect for development and testing — no setup required
- In a real production app, you would use PostgreSQL or MySQL instead

---

# PART 2 — UNDERSTANDING THE TESTS

## What is Software Testing?

Testing is the process of **automatically checking** that your code does what it should.

Instead of manually opening Postman every time you make a change, you write code that does the checking for you. You run the tests and they tell you: ✅ PASS or ❌ FAIL.

**Why test?**
- Catch bugs before they reach real users
- Safely make changes without breaking existing features
- Prove to your team (and lecturer!) that the code works correctly

---

## The Test Pyramid — Three Levels of Testing

The Test Pyramid is a strategy for how many tests to write at each level:

```
        /\
       /  \   ← TOP: End-to-End Tests (fewest, slowest, most realistic)
      /----\
     /      \ ← MIDDLE: Integration Tests (medium amount)
    /--------\
   /          \ ← BOTTOM: Unit Tests (most tests, fastest, most focused)
  /____________\
```

### Level 1 — Unit Tests (Bottom of Pyramid)
**What:** Test ONE small piece of code in isolation — one method, one class.
**How:** Use fake/mocked dependencies so only the code being tested runs.
**Speed:** Very fast — milliseconds each.
**Our files:** `TaskServiceTest.java`, `ProjectServiceTest.java`, `TaskMapperTest.java`

**Analogy:** Testing a single brick before building a wall. You check the brick alone.

### Level 2 — Integration Tests (Middle of Pyramid)
**What:** Test how multiple pieces work together — e.g., Controller + Spring framework.
**How:** Load part of the Spring context. Some real components, some mocked.
**Speed:** Slower than unit tests — seconds each.
**Our files:** `TaskControllerWebMvcTest.java`, `TaskRepositoryIntegrationTest.java`

**Analogy:** Testing how bricks and mortar hold together in a small section of wall.

### Level 3 — End-to-End Tests (Top of Pyramid)
**What:** Test the entire application from HTTP request to database and back.
**How:** Start the full Spring Boot app and make real HTTP calls to it.
**Speed:** Slowest — but most realistic.
**Our files:** `TaskManagerE2ETest.java`

**Analogy:** Testing the whole wall under real conditions.

---

## Our Test Files Explained

### `TaskServiceTest.java` — Unit Tests for Task Business Logic
**What it tests:** The `TaskService` class — the rules for creating and managing tasks.

Tests inside it:
1. ✅ `createTask_whenProjectExistsAndDueDateFuture_returnsTaskDTO`
   — "When I create a task with a valid project and future due date, it should succeed"

2. ✅ `createTask_whenProjectNotFound_throwsResourceNotFoundException`
   — "When I create a task for a project that doesn't exist, it should throw an error"

3. ✅ `createTask_whenDueDateIsInPast_throwsBusinessRuleException`
   — "When I set a due date in the past, it should be rejected"

4. ✅ `updateTaskStatus_toDone_setsCompletedAtTimestamp`
   — "When I mark a task as DONE, the completedAt time should automatically be recorded"

5. ✅ `updateTaskStatus_fromDoneToOpen_throwsBusinessRuleException`
   — "Once a task is DONE, you cannot re-open it"

6. ✅ `getTaskById_whenFound_returnsDTO` — "Get a task by its ID"

7. ✅ `getTaskById_whenNotFound_throwsResourceNotFoundException` — "Getting a non-existent task throws an error"

8. ✅ `deleteTask_whenTaskExists_callsRepositoryDelete` — "Deleting a task removes it from the database"

**Key concept — Mocking:**
In these tests, the database is NOT real. We use **Mockito** to create a fake database that returns whatever we tell it to. This makes tests fast and focused.

```java
// This line says: "when the database is asked for project ID 1, return our fake project"
when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
```

---

### `ProjectServiceTest.java` — Unit Tests for Project Business Logic

Tests inside it:
1. ✅ `createProject_whenNameIsUnique_returnsProjectDTO` — "Create a project with a unique name"
2. ✅ `createProject_whenNameIsDuplicate_throwsBusinessRuleException` — "Reject duplicate project names"
3. ✅ `deleteProject_whenHasInProgressTasks_throwsBusinessRuleException` — "Cannot delete a project with active tasks"
4. ✅ `deleteProject_whenNoInProgressTasks_callsRepositoryDelete` — "Can delete when no tasks are in progress"
5. ✅ `getProjectById_whenNotFound_throwsResourceNotFoundException` — "Error when project not found"

---

### `TaskControllerWebMvcTest.java` — Integration Tests for the Web Layer

**What it tests:** The HTTP layer — does the controller correctly handle requests and responses?

**Key tool:** `MockMvc` — simulates HTTP requests without starting a real server.
The `TaskService` is mocked, so only the controller logic is tested.

Tests inside it:
1. ✅ `createTask_withValidRequest_returns201WithTaskDTO` — "Valid request returns HTTP 201 Created"
2. ✅ `createTask_withBlankTitle_returns400` — "Empty title returns HTTP 400 Bad Request"
3. ✅ `createTask_withNullProjectId_returns400` — "Missing project ID returns HTTP 400"
4. ✅ `getTaskById_whenTaskNotFound_returns404` — "Task not found returns HTTP 404"
5. ✅ `updateTaskStatus_withValidStatus_returns200` — "Status update returns HTTP 200 OK"

---

### `TaskRepositoryIntegrationTest.java` — Integration Tests for the Database Layer

**What it tests:** Does the database query code actually work correctly?
**Key tool:** `@DataJpaTest` — loads only the database layer with a real H2 database.

Tests inside it:
1. ✅ Finding tasks by project ID
2. ✅ Counting tasks by status
3. ✅ Finding critical non-done tasks

---

### `TaskManagerE2ETest.java` — End-to-End Tests

**What it tests:** The full application — real HTTP requests through the real Spring Boot app to a real H2 database.

Tests inside it:
1. ✅ `fullWorkflow_createProjectAndTask_thenCompleteTask`
   — Creates a project, adds a task, marks it DONE, verifies completedAt is set

2. ✅ `createTask_forNonExistentProject_returns404`
   — Full HTTP call to create a task for a project that doesn't exist — gets 404 back

3. ✅ `deleteProject_withInProgressTask_returns409Conflict`
   — Creates project → creates task → moves to IN_PROGRESS → tries to delete project → gets 409 CONFLICT

4. ✅ `getCriticalOpenTasks_returnsOnlyCriticalNonDoneTasks`
   — Creates 3 tasks with different priorities/statuses → verifies only the critical+open one is returned

---

## What is Dependency Injection?

**Dependency Injection (DI)** is how Spring automatically provides the components a class needs.

Without DI (bad):
```java
// The class creates its own database connection — hard to test!
public class TaskService {
    private TaskRepository taskRepository = new TaskRepository(); // hardcoded!
}
```

With DI (good):
```java
// Spring provides the repository — easy to swap with a mock for testing!
public class TaskService {
    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) { // Spring injects this
        this.taskRepository = taskRepository;
    }
}
```

This is why testing is easier — in tests, we inject a **mock** instead of the real repository.

---

# PART 3 — UNDERSTANDING THE JACOCO COVERAGE REPORT

## What is the JaCoCo Report?

The JaCoCo report shows **how much of your code was actually run** during testing.

It is located at: `target/site/jacoco/index.html`

## Reading the Numbers in Your Report

Here is what you saw on screen:

| Package | Coverage | What It Means |
|---------|---------|---------------|
| `service` | **77%** | 77% of service code was tested |
| `model` | **84%** | 84% of model code was tested |
| `controller` | **61%** | 61% of controller code was tested |
| `mapper` | **37%** | Only 37% of the main app startup was tested |
| `dto` | **100%** | All DTO code was tested |
| `mapper` | **100%** | All mapper code was tested |
| `exception` | **100%** | All exception code was tested |
| **TOTAL** | **85%** | 85% of all instructions tested overall |

## Column Meanings

| Column | What It Means |
|--------|--------------|
| **Missed Instructions** | Lines of code NOT run during tests |
| **Cov. (Coverage %)** | Percentage of code that WAS tested |
| **Missed Branches** | If/else decisions where one path was never tested |
| **Lines** | Total number of lines in the code |
| **Methods** | How many methods (functions) exist |
| **Classes** | How many classes (files) exist |

## What Does 85% Mean?

85% overall instruction coverage means:
- Out of 999 total code instructions, **859 were executed** during testing
- Only **140 instructions** were never reached
- This is a **strong result** — typically anything above 70% is considered good
- 100% coverage is not always required or realistic — some code handles rare system errors that are hard to simulate

The colour coding:
- 🟩 **Green** = code that was tested
- 🟥 **Red** = code that was NOT tested

---

# PART 4 — SCREENCAST SCRIPT

## Before You Start Recording

**Checklist:**
- [ ] VS Code is open with the project
- [ ] Terminal is visible and ready
- [ ] Browser is ready to open the JaCoCo report
- [ ] Camera is on
- [ ] Postman is open and collection is imported

---

## Script (Speak These Words While Doing the Actions)

---

### OPENING (30 seconds)

**SAY:**
> "Hello, my name is [Your Name] and this is my screencast for CBD Assignment 2.
> I have built a Task Manager REST API using Spring Boot. This application allows users
> to create projects, add tasks to those projects, update task statuses, and enforces
> business rules such as preventing deletion of projects with active tasks.
> I will now demonstrate the automated test suite and coverage report."

---

### SECTION 1 — Show the Project Structure (30 seconds)

**DO:** Click on the `src` folder in VS Code to expand it. Show the main and test folders.

**SAY:**
> "Here is the project structure. Under `src/main` we have the application code organised
> into layers — controller, service, repository, model, and exception handling.
> Under `src/test` we have three packages — unit tests, integration tests, and end-to-end tests —
> which directly maps to the Test Pyramid."

---

### SECTION 2 — Run the Tests (1 minute)

**DO:** Click on the terminal in VS Code. Type and run:
```
cd c:/CBD/CBD-Assignment-2
mvn test
```

**SAY:**
> "I will now run the full automated test suite using Maven.
> This will execute all tests across all three levels of the Test Pyramid simultaneously."

*Wait for tests to run...*

**SAY (while tests are running):**
> "You can see Maven is compiling the code first, then running the tests.
> The unit tests run first as they are fastest, followed by integration tests,
> then the end-to-end tests which start the full application."

**SAY (when BUILD SUCCESS appears):**
> "All tests have passed — you can see the summary here.
> We have [number] tests total with zero failures and zero errors.
> Maven also generated the JaCoCo coverage report automatically."

---

### SECTION 3 — Open the Coverage Report (1 minute)

**DO:** Open your browser and navigate to:
```
c:/CBD/CBD-Assignment-2/target/site/jacoco/index.html
```
(just drag the file into your browser, or type the path in the browser address bar)

**SAY:**
> "This is the JaCoCo test coverage report generated automatically when we ran the tests.
> It shows us exactly how much of our application code was exercised by the tests."

**DO:** Point to the Total row at the bottom.

**SAY:**
> "Looking at the total row, we can see 85% instruction coverage across the entire application.
> Out of 999 total code instructions, only 140 were not reached by our tests."

**DO:** Click on `com.university.taskmanager.dto`

**SAY:**
> "The DTO and exception packages show 100% coverage — every line was tested.
> The service layer shows 77% coverage — the business logic is well tested,
> with the remaining uncovered code being edge cases in error handling paths."

**DO:** Go back and click on `com.university.taskmanager.service`

**SAY:**
> "Inside the service package, the green highlighting shows covered lines and
> red shows uncovered lines. The 70% branch coverage means that in if-else decisions,
> most paths have been tested."

---

### SECTION 4 — Explain the Test Pyramid Alignment (1 minute)

**DO:** Show the test files in VS Code by expanding src/test

**SAY:**
> "Our test suite deliberately follows the Test Pyramid strategy."

**DO:** Click on `unit/TaskServiceTest.java`

**SAY:**
> "At the base of the pyramid are unit tests. These test individual methods in isolation
> using Mockito to mock dependencies like the database repository.
> They run in milliseconds and we have the most of them.
> For example, this test checks that creating a task with a past due date throws a business rule exception —
> no real database is involved at all."

**DO:** Click on `integration/TaskControllerWebMvcTest.java`

**SAY:**
> "In the middle are integration tests. This file uses @WebMvcTest which loads only
> the Spring web layer — the controller — but mocks the service layer.
> It tests that HTTP requests are correctly mapped and that validation works.
> For example, sending a blank task title should return HTTP 400 Bad Request."

**DO:** Click on `e2e/TaskManagerE2ETest.java`

**SAY:**
> "At the top are the end-to-end tests. These use @SpringBootTest which boots the full application
> on a random port, then TestRestTemplate makes real HTTP calls just like Postman would.
> This test creates a project, adds a task, marks it as done, and verifies the timestamp was set.
> It is the most realistic test but also the slowest."

---

### SECTION 5 — Live Postman Demo (optional, 30 seconds)

**DO:** Switch to Postman. Open the Task Manager API collection.

**SAY:**
> "Finally, here is the Postman collection I created to manually interact with the live API.
> The application is running on port 8080."

**DO:** Run "Create Project" request. Show the 201 response.

**SAY:**
> "Creating a project returns 201 Created with the new project data."

**DO:** Run "Create Task" request. Show the response.

**SAY:**
> "Creating a task returns the task with status OPEN. You can see the projectId links it to the project we just created."

---

### CLOSING (20 seconds)

**SAY:**
> "To summarise — the test suite covers all three levels of the Test Pyramid,
> achieves 85% code coverage, uses dependency injection and mocking appropriately,
> and validates both positive scenarios and business rule violations.
> Thank you."

---

# PART 5 — QUICK REFERENCE CARD

## Key Terms Explained Simply

| Term | Simple Explanation |
|------|--------------------|
| Spring Boot | A Java toolkit that makes building web apps fast and easy |
| REST API | A way for programs to talk to each other using web addresses |
| H2 Database | A temporary database that lives in memory — perfect for testing |
| JUnit | The framework that runs your tests and reports PASS or FAIL |
| Mockito | A tool that creates fake versions of dependencies for testing |
| @Mock | "Create a fake version of this class" |
| @InjectMocks | "Create a real version of this class, but inject all the @Mocks into it" |
| MockMvc | A fake HTTP client for testing controllers without a real server |
| TestRestTemplate | A real HTTP client for full end-to-end testing |
| JaCoCo | A tool that measures how much code was run during testing |
| Coverage % | The percentage of your code that tests actually executed |
| Test Pyramid | A strategy: many fast unit tests, fewer slow end-to-end tests |
| Dependency Injection | Spring automatically provides the components a class needs |
| DTO | Data Transfer Object — the shape of data sent to/from clients |
| HTTP 200 | Success — the request worked fine |
| HTTP 201 | Created — a new resource was successfully created |
| HTTP 400 | Bad Request — the data you sent was invalid |
| HTTP 404 | Not Found — the resource you asked for doesn't exist |
| HTTP 409 | Conflict — a business rule was violated |

---

*Document prepared for CBD Assignment 2 — Task Manager Spring Boot Application*
