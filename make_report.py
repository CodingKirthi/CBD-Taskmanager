"""
Generates REPORT.docx from the report content.
Run: python make_report.py
"""

from docx import Document
from docx.shared import Pt, RGBColor, Inches, Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.style import WD_STYLE_TYPE
from docx.oxml.ns import qn
from docx.oxml import OxmlElement
import copy

doc = Document()

# ── Page margins ──────────────────────────────────────────────────────────────
for section in doc.sections:
    section.top_margin    = Cm(2.5)
    section.bottom_margin = Cm(2.5)
    section.left_margin   = Cm(2.5)
    section.right_margin  = Cm(2.5)

# ── Styles ────────────────────────────────────────────────────────────────────
DARK_BLUE = RGBColor(0x1F, 0x38, 0x64)
MID_BLUE  = RGBColor(0x2F, 0x54, 0x96)
WHITE     = RGBColor(0xFF, 0xFF, 0xFF)

def style_heading(paragraph, text, level, color=DARK_BLUE):
    paragraph.clear()
    run = paragraph.add_run(text)
    run.bold = True
    run.font.color.rgb = color
    run.font.size = Pt(18 - (level - 1) * 3)
    paragraph.paragraph_format.space_before = Pt(14)
    paragraph.paragraph_format.space_after  = Pt(6)
    return paragraph

def h1(text):
    p = doc.add_paragraph()
    style_heading(p, text, 1)
    return p

def h2(text):
    p = doc.add_paragraph()
    style_heading(p, text, 2)
    return p

def h3(text):
    p = doc.add_paragraph()
    style_heading(p, text, 3, MID_BLUE)
    return p

def h4(text):
    p = doc.add_paragraph()
    p.clear()
    run = p.add_run(text)
    run.bold = True
    run.font.color.rgb = MID_BLUE
    run.font.size = Pt(11)
    p.paragraph_format.space_before = Pt(10)
    p.paragraph_format.space_after  = Pt(4)
    return p

def body(text, bold_parts=None):
    """Add a body paragraph. bold_parts is a list of substrings to bold."""
    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(4)
    if not bold_parts:
        p.add_run(text)
    else:
        remaining = text
        for bp in bold_parts:
            idx = remaining.find(bp)
            if idx >= 0:
                if idx > 0:
                    p.add_run(remaining[:idx])
                r = p.add_run(bp)
                r.bold = True
                remaining = remaining[idx + len(bp):]
        if remaining:
            p.add_run(remaining)
    return p

def bullet(text):
    p = doc.add_paragraph(style='List Bullet')
    p.add_run(text)
    p.paragraph_format.space_after = Pt(2)
    return p

def code_block(text):
    p = doc.add_paragraph()
    p.paragraph_format.left_indent  = Cm(0.8)
    p.paragraph_format.space_before = Pt(6)
    p.paragraph_format.space_after  = Pt(6)
    run = p.add_run(text)
    run.font.name = 'Consolas'
    run.font.size = Pt(8.5)
    # Light grey background via shading
    pPr = p._p.get_or_add_pPr()
    shd = OxmlElement('w:shd')
    shd.set(qn('w:val'), 'clear')
    shd.set(qn('w:color'), 'auto')
    shd.set(qn('w:fill'), 'F2F2F2')
    pPr.append(shd)
    return p

def quote_block(text):
    p = doc.add_paragraph()
    p.paragraph_format.left_indent  = Cm(1.2)
    p.paragraph_format.space_before = Pt(4)
    p.paragraph_format.space_after  = Pt(4)
    run = p.add_run(text)
    run.italic = True
    run.font.size = Pt(10)
    pPr = p._p.get_or_add_pPr()
    shd = OxmlElement('w:shd')
    shd.set(qn('w:val'), 'clear')
    shd.set(qn('w:color'), 'auto')
    shd.set(qn('w:fill'), 'EBF3FB')
    pPr.append(shd)
    return p

def add_table(headers, rows):
    table = doc.add_table(rows=1 + len(rows), cols=len(headers))
    table.style = 'Table Grid'
    hdr_cells = table.rows[0].cells
    for i, h in enumerate(headers):
        hdr_cells[i].text = h
        run = hdr_cells[i].paragraphs[0].runs[0]
        run.bold = True
        run.font.color.rgb = WHITE
        tc = hdr_cells[i]._tc
        tcPr = tc.get_or_add_tcPr()
        shd = OxmlElement('w:shd')
        shd.set(qn('w:val'), 'clear')
        shd.set(qn('w:color'), 'auto')
        shd.set(qn('w:fill'), '1F3864')
        tcPr.append(shd)
    for ri, row in enumerate(rows):
        cells = table.rows[ri + 1].cells
        for ci, val in enumerate(row):
            cells[ci].text = val
    doc.add_paragraph()  # spacing after table

# ══════════════════════════════════════════════════════════════════════════════
# COVER PAGE
# ══════════════════════════════════════════════════════════════════════════════
p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
run = p.add_run('CBD Assignment 2')
run.bold = True
run.font.size = Pt(24)
run.font.color.rgb = DARK_BLUE

p2 = doc.add_paragraph()
p2.alignment = WD_ALIGN_PARAGRAPH.CENTER
run2 = p2.add_run('AI-Assisted Test Development Report')
run2.font.size = Pt(16)
run2.font.color.rgb = MID_BLUE

p3 = doc.add_paragraph()
p3.alignment = WD_ALIGN_PARAGRAPH.CENTER
p3.add_run('Application: Task Manager REST API (Spring Boot 3.2)   |   Weighting: 20%')

doc.add_paragraph()

# ══════════════════════════════════════════════════════════════════════════════
# APPLICATION UNDER TEST
# ══════════════════════════════════════════════════════════════════════════════
h1('Application Under Test')
body('The application is a Task Manager REST API built with Spring Boot 3.2.3. '
     'It exposes endpoints for managing Projects and Tasks and enforces the following business rules:')
bullet('A task cannot have a due date in the past.')
bullet('A completed task (DONE) cannot be reopened to OPEN.')
bullet('A project cannot be deleted while it has IN_PROGRESS tasks.')
bullet('Project names must be unique.')
body('Technology stack: Spring Boot 3.2.3, Spring Data JPA, H2 (file at runtime / in-memory for tests), '
     'Jakarta Bean Validation, JUnit 5, Mockito, AssertJ.')

# ══════════════════════════════════════════════════════════════════════════════
# SECTION 1 – TEST STRATEGY
# ══════════════════════════════════════════════════════════════════════════════
h1('Section 1 – Test Strategy')
h2('Alignment with the Test Pyramid')
body('The test suite is intentionally structured in three tiers that mirror the Test Pyramid: '
     'the cheapest, fastest tests dominate at the base; the fewest, most comprehensive tests sit at the apex.')

code_block(
    "         /\\\n"
    "        /  \\      E2E (4 tests)  – Full HTTP stack, real H2 DB\n"
    "       /----\\\n"
    "      / Int  \\    Integration (9 tests) – JPA slice + Web slice\n"
    "     /--------\\\n"
    "    /  Unit    \\  Unit (15 tests) – No Spring, Mockito only\n"
    "   /____________\\"
)

h4('Unit Tests (15 tests, ~0.7 s total)')
body('Unit tests dominate the suite (54%). They test business logic in isolation using '
     '@ExtendWith(MockitoExtension.class) – no Spring context is ever loaded. All dependencies are replaced '
     'with @Mock stubs; the class under test is wired via @InjectMocks.')

h4('Integration Tests (9 tests, ~3 s total)')
body('Integration tests operate at two Spring slices:')
bullet('@DataJpaTest (4 tests) – loads only JPA, repositories, and H2. Tests derived query methods against a real schema.')
bullet('@WebMvcTest (5 tests) – loads only the web layer. The service is replaced with @MockBean. '
       'Tests HTTP binding, JSON serialisation, and bean validation handled by GlobalExceptionHandler.')

h4('End-to-End Tests (4 tests, ~11 s total)')
body('@SpringBootTest(webEnvironment = RANDOM_PORT) + TestRestTemplate. The full application boots against '
     'an H2 in-memory database. These tests exercise the complete request → service → repository → response pipeline.')

h2('Test Pyramid Distribution')
add_table(
    ['Level', 'Tests', '% of Suite', 'Typical Duration'],
    [
        ['Unit', '15', '54%', '< 1 ms each'],
        ['Integration', '9', '32%', '50–500 ms each'],
        ['End-to-End', '4', '14%', '1–3 s each'],
        ['Total', '28', '100%', ''],
    ]
)

# ══════════════════════════════════════════════════════════════════════════════
# SECTION 2 – DEPENDENCY INJECTION AND MOCKING
# ══════════════════════════════════════════════════════════════════════════════
h1('Section 2 – Dependency Injection and Mocking Approach')

h2('How Constructor Injection Enables Testability')
body('All service classes use constructor injection exclusively:')
code_block(
    "@Service\n"
    "public class TaskService {\n"
    "    private final TaskRepository taskRepository;\n"
    "    private final ProjectRepository projectRepository;\n"
    "    private final TaskMapper taskMapper;\n\n"
    "    public TaskService(TaskRepository taskRepository,\n"
    "                       ProjectRepository projectRepository,\n"
    "                       TaskMapper taskMapper) {\n"
    "        this.taskRepository = taskRepository;\n"
    "        this.projectRepository = projectRepository;\n"
    "        this.taskMapper = taskMapper;\n"
    "    }\n"
    "}"
)
body("Because dependencies are declared in the constructor, a unit test can instantiate TaskService directly "
     "by passing mock objects. Mockito's @InjectMocks exploits this same constructor to inject @Mock instances. "
     "No Spring container is needed.")
body("Field injection (@Autowired TaskRepository repo;) would require starting the full Spring context or using "
     "brittle reflection to inject mocks.")

h2('Where Mocks Were Used and Why')
add_table(
    ['Test Class', 'What is Mocked', 'Why'],
    [
        ['TaskServiceTest', 'TaskRepository, ProjectRepository, TaskMapper',
         'Isolate TaskService business logic. Control return values (e.g. Optional.empty() to trigger 404).'],
        ['ProjectServiceTest', 'ProjectRepository, TaskRepository, ProjectMapper',
         'Test delete-blocking logic without a real database.'],
        ['TaskControllerWebMvcTest', 'TaskService (@MockBean)',
         'Isolate the HTTP layer. Test controller annotations, validation, and GlobalExceptionHandler independently.'],
    ]
)
body('ArgumentCaptor was used in TaskServiceTest to capture the Task entity passed to taskRepository.save(), '
     'confirming completedAt was set before the save call.')

h2('Where Mocks Were Avoided and Why')
add_table(
    ['Test Class', 'Mock Avoided', 'Why'],
    [
        ['TaskMapperTest', 'None – no dependencies',
         'TaskMapper is a pure function. Direct instantiation. Zero framework overhead.'],
        ['TaskRepositoryIntegrationTest', 'No mocks',
         'The purpose is to verify derived query methods produce correct SQL. Mocking the repository would defeat this.'],
        ['TaskManagerE2ETest', 'No mocks',
         'The purpose is to verify all layers work together. Mocking any layer would undermine this guarantee.'],
    ]
)

# ══════════════════════════════════════════════════════════════════════════════
# SECTION 3 – MAPPING TO TEST PYRAMID
# ══════════════════════════════════════════════════════════════════════════════
h1('Section 3 – Mapping to Test Pyramid')

h2('Unit Tests (Bottom Layer) – 15 tests')

h3('TaskServiceTest (8 tests) – @ExtendWith(MockitoExtension.class)')
add_table(
    ['Test Method', 'What It Verifies'],
    [
        ['createTask_whenProjectExistsAndDueDateFuture_returnsTaskDTO',
         'Happy path: task created with status OPEN when project exists and due date is in the future'],
        ['createTask_whenProjectNotFound_throwsResourceNotFoundException',
         'Boundary: missing project triggers exception before any save'],
        ['createTask_whenDueDateIsInPast_throwsBusinessRuleException',
         'Business rule: past due dates are rejected'],
        ['updateTaskStatus_toDone_setsCompletedAtTimestamp',
         'Side-effect: completedAt is populated when transitioning to DONE'],
        ['updateTaskStatus_fromDoneToOpen_throwsBusinessRuleException',
         'Business rule: completed tasks cannot be reopened'],
        ['getTaskById_whenFound_returnsDTO',
         'Happy path: found task is mapped to DTO'],
        ['getTaskById_whenNotFound_throwsResourceNotFoundException',
         'Boundary: missing task throws exception'],
        ['deleteTask_whenTaskExists_callsRepositoryDelete',
         'Delegation: delete is forwarded to repository'],
    ]
)

h3('ProjectServiceTest (5 tests) – @ExtendWith(MockitoExtension.class)')
add_table(
    ['Test Method', 'What It Verifies'],
    [
        ['createProject_whenNameIsUnique_returnsProjectDTO', 'Happy path: unique name allows creation'],
        ['createProject_whenNameIsDuplicate_throwsBusinessRuleException', 'Business rule: duplicate names are rejected'],
        ['deleteProject_whenHasInProgressTasks_throwsBusinessRuleException',
         'Business rule: project with in-progress work cannot be deleted'],
        ['deleteProject_whenNoInProgressTasks_callsRepositoryDelete', 'Happy path: safe projects are deleted'],
        ['getProjectById_whenNotFound_throwsResourceNotFoundException', 'Boundary: missing project throws exception'],
    ]
)

h3('TaskMapperTest (2 tests) – plain Java instantiation, no annotations')
add_table(
    ['Test Method', 'What It Verifies'],
    [
        ['toDTO_mapsAllFieldsCorrectly', 'All entity fields appear correctly in the DTO'],
        ['toDTO_whenCompletedAtIsNull_dtoCompletedAtIsNull', 'Nullable fields pass through as null without NPE'],
    ]
)

h2('Integration Tests (Middle Layer) – 9 tests')

h3('TaskRepositoryIntegrationTest (4 tests) – @DataJpaTest')
body('Loads only the JPA slice: repositories, entity manager, and H2. No web layer, no service layer.')
add_table(
    ['Test Method', 'What It Verifies'],
    [
        ['findByProjectId_returnsOnlyTasksBelongingToThatProject', 'Derived query produces correct SQL JOIN'],
        ['findByStatus_returnsOnlyTasksWithMatchingStatus', 'Enum column filtering works correctly'],
        ['countByProjectIdAndStatus_returnsCorrectCount', 'COUNT query with two predicates returns accurate number'],
        ['findByPriorityAndStatusNot_returnsCriticalNonDoneTasks',
         'NOT-EQUAL filter in derived query works correctly'],
    ]
)

h3('TaskControllerWebMvcTest (5 tests) – @WebMvcTest(TaskController.class)')
body('Loads only the web layer. TaskService replaced with @MockBean.')
add_table(
    ['Test Method', 'What It Verifies'],
    [
        ['createTask_withValidRequest_returns201WithTaskDTO',
         'Controller maps POST to service and returns 201 with JSON body'],
        ['createTask_withBlankTitle_returns400', '@NotBlank validation enforced before reaching the service'],
        ['createTask_withNullProjectId_returns400', '@NotNull validation enforced'],
        ['getTaskById_whenTaskNotFound_returns404',
         'ResourceNotFoundException mapped to 404 by GlobalExceptionHandler'],
        ['updateTaskStatus_withValidStatus_returns200', 'PATCH endpoint mapped correctly'],
    ]
)

h2('End-to-End Tests (Top Layer) – 4 tests')

h3('TaskManagerE2ETest (4 tests) – @SpringBootTest(webEnvironment = RANDOM_PORT)')
body('Full application on a random port. TestRestTemplate makes real HTTP calls. H2 in-memory DB.')
add_table(
    ['Test Method', 'What It Verifies'],
    [
        ['fullWorkflow_createProjectAndTask_thenCompleteTask',
         'Full lifecycle via real HTTP: create project → task → complete → verify DONE + completedAt'],
        ['createTask_forNonExistentProject_returns404',
         'Error path through the full stack: correct 404 response body'],
        ['deleteProject_withInProgressTask_returns409Conflict',
         'Business rule enforced end-to-end, 409 response returned'],
        ['getCriticalOpenTasks_returnsOnlyCriticalNonDoneTasks',
         'Filter endpoint returns correct subset from real DB'],
    ]
)

# ══════════════════════════════════════════════════════════════════════════════
# SECTION 4 – AI INTERACTION LOG
# ══════════════════════════════════════════════════════════════════════════════
h1('Section 4 – AI Interaction Log')

# ---- Example 1 ----
h2('Example 1: AI Output Accepted As-Is')

h3('Prompt given to AI')
quote_block(
    '"Write a @DataJpaTest integration test for TaskRepository. Test the findByPriorityAndStatusNot query method. '
    'Persist test data using TestEntityManager, clear the entity manager cache, then assert that only tasks with '
    'CRITICAL priority and non-DONE status are returned. Use AssertJ assertions."'
)

h3('AI-generated output (accepted without modification)')
code_block(
    "@Test\n"
    "void findByPriorityAndStatusNot_returnsCriticalNonDoneTasks() {\n"
    "    Project project = entityManager.persistAndFlush(new Project(\"Critical Project\", null));\n\n"
    "    entityManager.persistAndFlush(new Task(\"Critical open\", null, TaskStatus.OPEN,\n"
    "            TaskPriority.CRITICAL, null, project));\n"
    "    entityManager.persistAndFlush(new Task(\"Critical done\", null, TaskStatus.DONE,\n"
    "            TaskPriority.CRITICAL, null, project));\n"
    "    entityManager.persistAndFlush(new Task(\"Medium open\", null, TaskStatus.OPEN,\n"
    "            TaskPriority.MEDIUM, null, project));\n\n"
    "    entityManager.clear();\n\n"
    "    List<Task> criticalNonDone = taskRepository.findByPriorityAndStatusNot(\n"
    "            TaskPriority.CRITICAL, TaskStatus.DONE);\n\n"
    "    assertThat(criticalNonDone).hasSize(1);\n"
    "    assertThat(criticalNonDone.get(0).getTitle()).isEqualTo(\"Critical open\");\n"
    "}"
)

h3('Justification')
body('The AI correctly used persistAndFlush() followed by entityManager.clear() – the proper @DataJpaTest pattern: '
     'flush to write to H2, then clear the first-level cache to force a real SQL SELECT on the subsequent query. '
     'Without clear(), Hibernate returns cached objects rather than executing the query, potentially masking bugs. '
     'The test covers exactly one scenario, has a clean Arrange-Act-Assert structure, and uses typed AssertJ assertions.')

# ---- Example 2 ----
h2('Example 2: AI Output Modified')

h3('Original prompt')
quote_block(
    '"Write a unit test for TaskService.updateTaskStatus that verifies the completedAt timestamp is set '
    'when a task is transitioned to DONE."'
)

h3('Original AI-generated output')
code_block(
    "@Test\n"
    "void updateTaskStatus_toDone_setsCompletedAt() {\n"
    "    Task task = new Task();\n"
    "    task.setId(5L);\n"
    "    task.setStatus(TaskStatus.IN_PROGRESS);\n\n"
    "    when(taskRepository.findById(5L)).thenReturn(Optional.of(task));\n"
    "    when(taskRepository.save(task)).thenReturn(task);\n"
    "    when(taskMapper.toDTO(task)).thenReturn(new TaskDTO());\n\n"
    "    taskService.updateTaskStatus(5L, TaskStatus.DONE);\n\n"
    "    assertThat(task.getCompletedAt()).isNotNull();  // asserts on local variable – wrong\n"
    "}"
)

h3('Problem with the original')
body('The test asserted on the local task variable, relying on Java reference identity rather than verifying '
     'the entity passed to save(). If the service were refactored to copy the entity before saving, the assertion '
     'would pass on the original object even though the saved copy had a null completedAt. Additionally, '
     'new Task() leaves the project field null, causing a NullPointerException when TaskMapper calls '
     'task.getProject().getName().')

h3('Final modified version')
code_block(
    "@Test\n"
    "void updateTaskStatus_toDone_setsCompletedAtTimestamp() {\n"
    "    Project project = new Project(\"Gamma\", \"Gamma project\");\n"
    "    project.setId(3L);\n\n"
    "    Task task = new Task(\"Fix bug\", null, TaskStatus.IN_PROGRESS,\n"
    "            TaskPriority.HIGH, null, project);\n"
    "    task.setId(5L);\n\n"
    "    when(taskRepository.findById(5L)).thenReturn(Optional.of(task));\n"
    "    when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));\n"
    "    when(taskMapper.toDTO(any(Task.class))).thenReturn(\n"
    "            new TaskDTO(5L, \"Fix bug\", null, TaskStatus.DONE, TaskPriority.HIGH,\n"
    "                        null, LocalDateTime.now(), 3L, \"Gamma\"));\n\n"
    "    taskService.updateTaskStatus(5L, TaskStatus.DONE);\n\n"
    "    ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);\n"
    "    verify(taskRepository).save(captor.capture());\n"
    "    assertThat(captor.getValue().getCompletedAt()).isNotNull();\n"
    "    assertThat(captor.getValue().getStatus()).isEqualTo(TaskStatus.DONE);\n"
    "}"
)

h3('What changed and why')
body('1. ArgumentCaptor was introduced to capture the entity passed to save(), testing the actual contract: '
     'that the service sets completedAt before persisting.')
body('2. any(Task.class) replaces the specific reference match so the stub works if the service copies the entity.')
body('3. A real Project object was added to prevent the NullPointerException in the mapper.')

# ---- Example 3 ----
h2('Example 3: AI Output Rejected')

h3('Prompt')
quote_block(
    '"Write a unit test for ProjectService.deleteProject that verifies the method throws '
    'BusinessRuleException when the project has in-progress tasks."'
)

h3('Rejected AI-generated output (excerpt)')
code_block(
    "@SpringBootTest\n"
    "@Transactional\n"
    "class ProjectServiceDeleteTest {\n\n"
    "    @Autowired\n"
    "    private ProjectService projectService;\n\n"
    "    @Autowired\n"
    "    private ProjectRepository projectRepository;\n\n"
    "    @Test\n"
    "    void deleteProject_withInProgressTasks_throwsException() {\n"
    "        Project project = new Project(\"Doomed Project\", \"desc\");\n"
    "        projectRepository.save(project);\n"
    "        // ...\n"
    "        assertThrows(BusinessRuleException.class,\n"
    "                () -> projectService.deleteProject(project.getId()));\n"
    "    }\n"
    "}"
)

h3('Technical justification for rejection')
body('1. Wrong layer of the Test Pyramid. @SpringBootTest boots the entire application. The same rule can be '
     'verified in under 1 ms using Mockito; this test takes over 5 seconds with unnecessary JPA and HTTP startup overhead.')
body('2. Misuse of the Spring context for a unit concern. The business rule is entirely contained within '
     'ProjectService.deleteProject(). A full context does not provide additional confidence – only startup cost.')
body('3. Fragile implicit contract. The test relies on @Autowired repositories, H2 schema creation, and '
     '@Transactional rollback. Any misconfiguration causes test failure for reasons unrelated to the business rule.')

# ---- Example 4 ----
h2('Example 4: AI-Identified Gap or Improvement')

h3('Analysis prompt')
quote_block(
    '"Review the unit test suite for TaskService. Identify any missing test scenarios or edge cases '
    'not currently covered."'
)

h3("AI's suggestion")
quote_block(
    '"The current suite does not test deleteTask when the task does not exist. TaskService.deleteTask calls '
    'taskRepository.existsById(id) and throws ResourceNotFoundException if it returns false. The only existing '
    'deleteTask test covers the happy path. A test for the not-found case would verify the guard clause and '
    'that deleteById is never called."'
)

h3('Action taken – test added')
code_block(
    "@Test\n"
    "void deleteTask_whenTaskDoesNotExist_throwsResourceNotFoundException() {\n"
    "    when(taskRepository.existsById(404L)).thenReturn(false);\n\n"
    "    assertThatThrownBy(() -> taskService.deleteTask(404L))\n"
    "            .isInstanceOf(ResourceNotFoundException.class)\n"
    "            .hasMessageContaining(\"404\");\n\n"
    "    verify(taskRepository, never()).deleteById(any());\n"
    "}"
)
body('The AI correctly identified that every guard clause deserves both a passing and a failing test. '
     'The suggestion was accepted and incorporated, bringing TaskServiceTest to 8 tests.')

# ---- Example 5 ----
h2('Example 5: AI Output Constrained by Explicit Rules')

h3('Prompt with explicit constraints')
quote_block(
    '"Write a unit test for TaskMapper.toDTO. Apply the following constraints:\n'
    '1. Do NOT use @ExtendWith(MockitoExtension.class) or any Mockito annotations.\n'
    '2. Do NOT start a Spring context (@SpringBootTest, @DataJpaTest, @WebMvcTest).\n'
    '3. Instantiate TaskMapper with new TaskMapper() directly.\n'
    '4. Use only JUnit 5 and AssertJ – no Spring test utilities.\n'
    'Verify that all fields of the Task entity are correctly mapped to TaskDTO."'
)

h3('AI-generated output produced under this constraint')
code_block(
    "class TaskMapperTest {\n\n"
    "    private final TaskMapper mapper = new TaskMapper();\n\n"
    "    @Test\n"
    "    void toDTO_mapsAllFieldsCorrectly() {\n"
    "        Project project = new Project(\"Mapper Project\", \"desc\");\n"
    "        project.setId(5L);\n\n"
    "        Task task = new Task(\"Map me\", \"description here\",\n"
    "                TaskStatus.IN_PROGRESS, TaskPriority.HIGH,\n"
    "                LocalDate.of(2026, 6, 1), project);\n"
    "        task.setId(99L);\n"
    "        task.setCompletedAt(LocalDateTime.of(2026, 5, 20, 10, 0));\n\n"
    "        TaskDTO dto = mapper.toDTO(task);\n\n"
    "        assertThat(dto.getId()).isEqualTo(99L);\n"
    "        assertThat(dto.getProjectName()).isEqualTo(\"Mapper Project\");\n"
    "        // ... all fields verified\n"
    "    }\n"
    "}"
)

h3('Explanation')
body('Behaviour being prevented: Without the constraint, AI tools default to adding @ExtendWith(MockitoExtension.class) '
     'or @SpringBootTest even for the simplest test classes. For TaskMapper – which has no dependencies – this creates '
     'thousands of lines of unnecessary framework overhead.')
body('Why the constraint was necessary: TaskMapper is stateless and dependency-free. Testing it should cost nothing '
     'more than instantiating it and calling a method.')
body('How the constraint improved test quality: The resulting test runs in under 1 millisecond with zero framework '
     'overhead. It cannot be broken by database configuration, Spring Boot version changes, or profile settings – '
     'a textbook example of a test correctly placed at the base of the Test Pyramid.')

# ══════════════════════════════════════════════════════════════════════════════
# SECTION 5 – EVALUATION AND REFLECTION
# ══════════════════════════════════════════════════════════════════════════════
h1('Section 5 – Evaluation and Reflection')

h2('Strengths of AI-Generated Tests')

h3('Strength 1: Boilerplate generation speed')
body('AI generated the structural boilerplate of test classes – imports, class-level annotations, mock field '
     'declarations, and @BeforeEach setup – in seconds. For TaskRepositoryIntegrationTest, the AI correctly '
     'identified @DataJpaTest, @ActiveProfiles("test"), TestEntityManager, and the persistAndFlush/clear pattern '
     'without being prompted. Writing this setup manually is error-prone; the AI produced it correctly on the '
     'first attempt.')

h3('Strength 2: Comprehensive boundary condition identification')
body('When asked to review the test suite (Example 4), the AI correctly identified the missing deleteTask '
     'not-found test. It systematically recognised that every guard clause requires both a passing and a failing '
     'test – the kind of thorough enumeration humans focusing on the happy path sometimes miss.')

h2('Weaknesses of AI-Generated Tests')

h3('Weakness 1: Default to heavy test infrastructure')
body('Example 3 demonstrates the most consistent AI failure: reaching for @SpringBootTest regardless of whether '
     'the scenario requires it. The AI generated a full-context integration test for a scenario needing only two '
     'mock objects. Left unconstrained, AI loads thousands of lines of Spring infrastructure for a test that '
     'should run in milliseconds.')

h3('Weakness 2: Testing implementation details rather than contracts')
body('In Example 2, the AI-generated test asserted on the local task variable rather than using ArgumentCaptor '
     'to inspect the entity passed to save(). The test passed for the wrong reason: it verified Java reference '
     'semantics, not that the service sets completedAt before persisting. A test that passes for the wrong reason '
     'can survive a refactoring that breaks the actual contract.')

h2('Observed AI Failure Mode: Incorrect Assumption About Test Scope')
body('Across multiple interactions, AI tools consistently pattern-match "Spring Boot application" → '
     '"use @SpringBootTest" without reasoning about whether the specific test requires the full context. '
     'The practical consequence is test suites that are slow, brittle, and fragile under configuration changes – '
     'for reasons entirely unrelated to the business logic under test. This is not occasional; it is the default '
     'behaviour unless explicitly constrained (as demonstrated in Example 5).')

h2('Example Where Human Judgement Improved Test Quality')
body('In Example 2, the AI assertion on the mutated local variable appeared correct at first glance. It was '
     'human review of the reason for the assertion – not just the assertion itself – that identified the problem. '
     'The human question was: "Is this test actually verifying that the entity passed to save() has completedAt '
     'set, or is it just verifying that the service mutates the same object it received?" AI does not naturally '
     'ask this kind of reflective question. The fix – ArgumentCaptor – came from applying the testing principle '
     'that assertions should verify observable outputs through public interfaces, not internal object state.')

h2('One Improvement to the AI-Assisted Testing Approach')
body('The most valuable single improvement would be to establish a constraint prompt template prepended to every '
     'test-generation request. Based on patterns observed in this assignment, the template would include:')
code_block(
    "Rules for all tests in this project:\n"
    "1. If the class under test has no Spring dependencies, use @ExtendWith(MockitoExtension.class) only.\n"
    "2. Never use @SpringBootTest for a test that can be written with @WebMvcTest, @DataJpaTest,\n"
    "   or plain Mockito.\n"
    "3. When asserting on side-effects (save(), delete()), use ArgumentCaptor to verify the exact\n"
    "   argument passed, not the local variable.\n"
    "4. Every method with a guard clause requires at least two tests: one where the guard passes,\n"
    "   one where it throws."
)
body('Rather than correcting AI output after the fact, this approach constrains it at the source – eliminating '
     'the most frequently observed failure modes before any code is generated.')

# ── Save ─────────────────────────────────────────────────────────────────────
doc.save('c:/CBD/CBD-Assignment-2/REPORT.docx')
print('REPORT.docx saved successfully.')
