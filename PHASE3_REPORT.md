# B-Tech JAVA-IV-T042 — PROJECT PROPOSAL

---

## PROJECT AND TEAM INFORMATION

**Project Title**

> The-Student-Archive

---

### Student / Team Information

| Role | Name | Student ID | Email |
|---|---|---|---|
| Team Lead | Thakur, Vansh | 240212372 | vanshthakur0508@gmail.com |
| Team Member 2 | Kumar, Shubham | 240212368 | shubhamattrik@gmail.com |
| Team Member 3 | Sharma, Dikshant | 240210XX | sharma.dikshant101@gmail.com |
| Team Member 4 | Negi, Sumeet | 24021062 | sumeetnegi4429@gmail.com |

---

---

# PROJECT PROGRESS DESCRIPTION (35 pts)

---

## Project Abstract (2 pts)

This project presents **"The Student Archive"**, an intelligent academic search engine designed to solve the problem of scattered and unstructured study materials. Students often struggle to find notes, previous year question papers, and other resources across platforms like WhatsApp, Google Drive, and emails.

The system converts a passive storage system into an active search engine by enabling full-text search within documents. It uses PostgreSQL full-text search with GIN indexing to provide fast and relevant results. PDF documents are processed using Apache PDFBox, and their content is indexed automatically.

A SHA-256 hashing mechanism is used to prevent duplicate files. The system is built using Spring Boot and runs as a desktop application using Java Swing with `WebApplicationType.NONE`. It includes secure authentication using GitHub OAuth2 with role-based access control (ADMIN / USER / GUEST). Phase 3 adds an embedded PDF viewer, an NLP query processor with synonym expansion and stemming, and a full admin control panel — making academic resources easily searchable, viewable, and manageable from a single desktop interface.

---

## Updated Project Approach and Architecture (2 pts)

The system follows a modular multi-layer architecture consisting of UI, auth, NLP, service, and data layers.

The **UI layer** is developed using Java Swing (`AcademicSearchUI.java`) to provide a responsive desktop interface with real-time search, role badges, and an embedded PDF viewer. The application runs without a web server (`WebApplicationType.NONE`).

The **auth layer** handles GitHub OAuth2 login via a local callback server (`LocalOAuthServer.java`), token persistence (`TokenManager.java`), and role resolution (`RoleManager.java`). Three roles are supported: ADMIN, USER, and GUEST. Admin access is controlled by a whitelist of GitHub usernames.

The **NLP layer** (`NLPQueryProcessor.java`) processes raw user queries through a pipeline: stopword removal → suffix stemming → synonym expansion → PostgreSQL `to_tsquery` string. An intent detector classifies queries as `YEAR_FILTER`, `DEPT_FILTER`, `SUBJECT_SEARCH`, or `GENERAL`.

The **service layer** (`SearchService.java`, `PDFToDatabase.java`) performs NLP-enhanced full-text search queries and the PDF ingestion pipeline. The **data layer** uses PostgreSQL with `tsvector` and GIN indexing. Metadata is managed using JPA, while high-performance queries are executed using `JdbcTemplate`.

The system works by scanning PDF files on startup, extracting their content, storing it in the database, and allowing users to search through the indexed data with NLP-enhanced queries. This architecture ensures scalability, maintainability, and high performance.

---

## Flow Diagram (2 pts)

```
┌─────────────────────────────────────────────────────────────┐
│                     User Interaction                        │
│         Query typed in AcademicSearchUI (Swing)             │
└──────────────────────────┬──────────────────────────────────┘
                           │ SwingWorker (background thread)
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    NLP Query Processor                      │
│   Stopword removal → Stemming → Synonym expansion           │
│   Intent detection (YEAR / DEPT / SUBJECT / GENERAL)        │
│   Output: to_tsquery compatible string                      │
└──────────────────────────┬──────────────────────────────────┘
                           │ Expanded tsquery string
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                     Search Service                          │
│   to_tsquery('english', ?) or plainto_tsquery (fallback)    │
│   ORDER BY ts_rank(document_vector, ...) DESC LIMIT 100     │
└──────────────────────────┬──────────────────────────────────┘
                           │ SQL Execution
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                   PostgreSQL Engine                         │
│   ts_vector Full-Text Search via GIN Index                  │
└──────────────────────────┬──────────────────────────────────┘
                           │ 200 OK / ResultSet
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                   Result Rendering                          │
│   JTable rows updated on EDT                                │
│   Status bar shows NLP explanation                          │
│   Double-click → PDFViewerWindow (embedded renderer)        │
└─────────────────────────────────────────────────────────────┘

GitHub OAuth Flow:
User clicks "Login with GitHub"
        ↓
LocalOAuthServer starts on port 8888
        ↓
Desktop.browse() opens GitHub authorize URL
        ↓
User approves → GitHub redirects to localhost:8888/callback
        ↓
Code extracted → POST to GitHub token endpoint
        ↓
Access token → GET /user from GitHub API
        ↓
RoleManager.getRole(login) → ADMIN or USER
        ↓
UI updates: role badge shown, Admin Panel button visible for ADMIN
```

---

## Tasks Completed (7 pts)

| Task Completed | Team Member |
|---|---|
| NLP Query Processor — stopword removal, suffix stemming, synonym expansion, intent detection (`NLPQueryProcessor.java`) | Sumeet Negi |
| Role-Based Access Control — ADMIN / USER / GUEST roles via GitHub login whitelist (`RoleManager.java`) | Dikshant Sharma |
| Embedded PDF Viewer — PDFBox `PDFRenderer`, page navigation, zoom in/out, SwingWorker rendering (`PDFViewerWindow.java`) | Vansh Thakur |
| Admin Control Panel — view all records, delete with confirmation, per-department stats, view PDF (`AdminPanel.java`) | Shubham Kumar |
| Role-aware Main UI — role badges, Admin Panel button, NLP status bar explanation, Exact Search checkbox (`AcademicSearchUI.java`) | All Members |
| NLP-enhanced SearchService — `to_tsquery` with NLP expansion, `plainto_tsquery` fallback on syntax error (`SearchService.java`) | Sumeet Negi |
| GitHubUserProfile role integration — `getRole()`, `isAdmin()`, `getRoleLabel()` methods added (`GitHubUserProfile.java`) | Dikshant Sharma |
| Session restore with role — login state and role badge restored on app restart from saved token | Shubham Kumar |
| Double-click PDF open — opens `PDFViewerWindow` instead of system default app | Vansh Thakur |
| pom.xml updated — added `pdfbox-tools` dependency for `PDFRenderer` | Shubham Kumar |

---

## Challenges / Roadblocks (7 pts)

| Challenge | Solution |
|---|---|
| **Swing EDT freeze during search** — Running JDBC queries directly on the Event Dispatch Thread caused the entire UI to freeze until results returned, making the app appear crashed. | Wrapped all database calls in `SwingWorker<>`. `doInBackground()` runs the query on a background thread; `done()` updates the JTable safely back on the EDT. Progress bar shows during execution. |
| **NLP-expanded `to_tsquery` syntax errors** — The NLP processor builds a `|`-joined tsquery string. If any synonym contained special characters or spaces, PostgreSQL rejected the query with a syntax error, returning zero results instead of falling back. | Added a try-catch in `SearchService.runSearch()`. If `to_tsquery` fails, it automatically retries with `plainto_tsquery` on the original raw query. Users never see an error — they just get results. |
| **PDFBox `PDFRenderer` not in base dependency** — `PDFRenderer` is part of `pdfbox-tools`, not the core `pdfbox` artifact. The class was missing at compile time even though `pdfbox` was in `pom.xml`, causing a `ClassNotFoundException` at runtime. | Added `pdfbox-tools:2.0.30` as a separate dependency in `pom.xml`. Both artifacts must be present for rendering to work. |
| **PDF page rendering blocking the UI** — Calling `renderer.renderImage()` on the EDT caused the viewer window to freeze on large or complex PDF pages, sometimes for several seconds. | Each page render is also wrapped in its own `SwingWorker`. The toolbar buttons are disabled during rendering and re-enabled in `done()`. The scroll pane resets to the top after each page change. |
| **Role assignment without a database** — Storing roles in the database would require a new table, migrations, and admin tooling. For a desktop app this was over-engineered. | Implemented `RoleManager.java` with a hardcoded `Set<String>` of admin GitHub usernames. Role is resolved live on every call — never stale. Adding a new admin requires only adding their GitHub username to the set. |
| **Admin panel accessible without login** — During early testing, the Admin Panel button was visible before login, and calling `new AdminPanel(null)` caused a `NullPointerException` inside the panel constructor. | Added a double-check: `RoleManager.isAdmin(adminLogin)` is called at the top of the `AdminPanel` constructor. If it returns false, an "Access Denied" dialog is shown and the window does not open. The button is also hidden in the UI unless the logged-in user is an ADMIN. |
| **`PDDocument` file handle leak** — Opening multiple PDFs without closing the previous `PDDocument` caused file handle exhaustion on Windows, eventually throwing `IOException: too many open files`. | Overrode `dispose()` in `PDFViewerWindow` to always call `document.close()` when the window is closed. `JFrame.DISPOSE_ON_CLOSE` ensures `dispose()` is called on the X button. |
| **NLP over-expansion returning irrelevant results** — Early synonym maps were too broad. Searching "os" expanded to `operating | system | kernel | os` and returned unrelated results containing the word "system". | Tuned the synonym map to academic-domain terms only. Added the "Exact (no NLP)" checkbox so users can bypass expansion entirely when they want precise matching. |

---

## Tasks Pending (7 pts)

| Task Pending | Team Member |
|---|---|
| GIN index creation in PostgreSQL — `CREATE INDEX ON academic_materials USING GIN(document_vector)` (still manual SQL) | Shubham Kumar |
| Pagination for search results — currently hard-limited to 100 rows; need page/offset controls in the UI | Sumeet Negi |
| Filtering dropdowns — filter by department, year, semester using SQL `WHERE` clause extension | All Members |
| Token file encryption — `~/.academic-search-auth.json` stores the OAuth token in plaintext | Dikshant Sharma |
| NLP synonym map expansion — more subjects, more degree abbreviations, Hindi transliteration support | All Members |
| Admin: file upload feature — allow admin to add new PDFs through the UI without restarting the app | Vansh Thakur |
| Admin: re-index trigger button — run `PDFToDatabase` pipeline on demand from the Admin Panel | Shubham Kumar |

---

## Progress Overview (2 pts)

The project is currently around **90% complete**. All core and advanced features — PDF ingestion, full-text search, NLP query processing, embedded PDF viewer, GitHub OAuth2 authentication, role-based access control (ADMIN/USER/GUEST), session persistence, and the Admin Control Panel — have been successfully implemented and tested. The remaining 10% covers polish items: GIN index automation, pagination, filtering dropdowns, token encryption, and admin file upload. The architecture has matured from a planned web app (Phase 2) into a fully functional desktop application with a clean layered structure and intelligent search capabilities.

---

## Deliverables Progress (2 pts)

The current status of project deliverables is as follows:

- Academic search engine system (Swing desktop): **Completed**
- Full-text search implementation (PostgreSQL tsvector + ts_rank): **Completed**
- NLP query processor (stopword removal, stemming, synonym expansion): **Completed**
- Embedded PDF viewer (PDFBox PDFRenderer, page navigation, zoom): **Completed**
- Role-based access control (ADMIN / USER / GUEST): **Completed**
- Admin Control Panel (view, delete, stats): **Completed**
- GitHub OAuth2 authentication: **Completed**
- Session persistence (token file + role restore): **Completed**
- Docker-based PostgreSQL setup: **Completed**
- GIN index (manual SQL): **Pending**
- Pagination: **Pending**
- Filtering dropdowns: **Pending**
- Token file encryption: **Pending**
- Admin file upload feature: **Pending**

---

## Testing and Validation Status (2 pts)

| Test Type | Status | Notes |
|---|---|---|
| Maven Build | Pass | `mvn compile` — zero errors, Java 21, Spring Boot 3.2.3 |
| Application Startup | Pass | Spring Boot starts with `WebApplicationType.NONE`, Swing window opens |
| PDF Ingestion Pipeline | Pass | Walks `JAVA DATABASE/` folder, indexes all PDFs on startup |
| Duplicate Prevention | Pass | SHA-256 check skips already-indexed files on re-run |
| Full-Text Search (plain) | Pass | `plainto_tsquery` returns correctly ranked results |
| NLP Search | Pass | `to_tsquery` with synonym expansion returns broader, relevant results |
| NLP Fallback | Pass | If `to_tsquery` syntax fails, retries with `plainto_tsquery` automatically |
| Exact Search (no NLP) | Pass | Checkbox bypasses NLP, uses `plainto_tsquery` directly |
| Zero-Match Handling | Pass | Returns empty table with "Found 0 results" in status bar |
| GitHub OAuth Login | Pass | Browser opens, callback received on port 8888, profile loaded |
| Role Assignment — ADMIN | Pass | Admin GitHub username → purple ADMIN badge + Admin Panel button visible |
| Role Assignment — USER | Pass | Non-admin GitHub username → green USER badge, no Admin Panel button |
| Session Restore | Pass | Reopening app restores login and role badge from token file |
| Embedded PDF Viewer | Pass | Double-click opens `PDFViewerWindow`, pages render correctly |
| PDF Page Navigation | Pass | Prev/Next buttons navigate pages, scroll resets to top |
| PDF Zoom | Pass | Zoom In/Out works from 50% to 400% |
| Admin Panel Access Control | Pass | Non-admin users cannot open Admin Panel (double-checked in constructor) |
| Admin Delete Record | Pass | Selected record deleted from DB with confirmation dialog |
| Admin Stats Panel | Pass | Total count and per-department breakdown load correctly |
| Logout | Pass | Token file deleted, UI resets, role badge hidden |

---

## Codebase Information (2 pts)

The project is maintained in a Git repository with proper version control practices. Sensitive information such as database credentials and OAuth secrets are excluded using `.gitignore`. The codebase is structured into modules including NLP, auth, UI, service, and data layers. Important commits include implementation of NLP query processing, embedded PDF viewer, role-based access control, and admin panel.

**GitHub:** https://github.com/shubham18-hub/The-Student-Archive

```
com.example/
├── DatabaseSetup.java              ← Spring Boot entry point (WebApplicationType.NONE)
├── AcademicMaterial.java           ← JPA entity → academic_materials table
├── AcademicMaterialRepository.java ← Spring Data JPA repository
├── PDFToDatabase.java              ← CommandLineRunner: PDF ingestion pipeline
├── nlp/
│   └── NLPQueryProcessor.java      ← Stopword removal, stemming, synonym expansion, intent detection
├── auth/
│   ├── RoleManager.java            ← ADMIN / USER / GUEST role resolution
│   └── TokenManager.java          ← Token save/load/delete from JSON file
└── ui/
    ├── AcademicSearchUI.java       ← Main Swing window (role-aware, NLP search, PDF viewer)
    ├── SearchService.java          ← NLP-enhanced + raw JDBC search queries
    ├── DatabaseConnection.java     ← Plain JDBC connection helper
    ├── PDFViewerWindow.java        ← Embedded PDF viewer (PDFRenderer, navigation, zoom)
    ├── AdminPanel.java             ← Admin-only: view all, delete, stats, view PDF
    ├── GitHubOAuthClient.java      ← Full OAuth2 browser flow
    ├── GitHubUserProfile.java      ← GitHub user data model + role methods
    └── LocalOAuthServer.java       ← Embedded HTTP server for OAuth callback
```
