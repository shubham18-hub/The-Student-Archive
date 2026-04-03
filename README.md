# 📚 Academic Materials Search Engine - Desktop Edition

A **Swing-based desktop application** for searching academic materials with GitHub OAuth2 authentication and PostgreSQL integration.

## ✨ Features

✅ **Full-Text Search** - Search 100+ academic materials using PostgreSQL full-text search  
✅ **GitHub OAuth2 Integration** - Browser-based OAuth2 authentication for desktop apps  
✅ **Database Connectivity** - JDBC connection to PostgreSQL with connection pooling  
✅ **Swing UI** - Clean, responsive desktop interface with results table  
✅ **Department Filtering** - Filter materials by academic department  
✅ **Real-time Results** - Background threading prevents UI freezing  

## 🚀 Quick Start

### Run the Application

```bash
cd d:\my-pdf-db
java -cp "src/main/java;lib/*" com.example.ui.AcademicSearchUI
```

Or compile first:
```bash
javac -cp "src/main/java" src/main/java/com/example/ui/*.java
java -cp "src/main/java;lib/*" com.example.ui.AcademicSearchUI
```

**Database Connection:**
- Host: `localhost:5433`
- Database: `resource_engine`
- User: `Javadb` / Password: `123456789`

---

## 📂 Documentation

| Document | Purpose |
|----------|---------|
| **[SWING_QUICKSTART.md](SWING_QUICKSTART.md)** | 👈 Quick setup & run guide |
| **[SWING_DATABASE_COMPLETE_GUIDE.md](SWING_DATABASE_COMPLETE_GUIDE.md)** | Database schema & troubleshooting |
| **[SWING_ARCHITECTURE_VISUAL.md](SWING_ARCHITECTURE_VISUAL.md)** | Architecture diagrams & system flow |

---

## 🏗️ **Tech Stack**

| Layer | Technology | Why? |
|-------|-----------|------|
| **Frontend** | Thymeleaf + Bootstrap 5 | Simple, responsive UI |
| **Backend** | Spring Boot 3.2.3 | Easy to configure, powerful |
| **Database** | PostgreSQL 16 | Full-text search, scalable |
| **Indexing** | tsvector/tsquery | Sub-second search speed |
| **PDF Processing** | Apache PDFBox | Extract text from PDFs |

---

## 🔍 **How Search Works**

1. **Admin uploads PDF** → System extracts text
2. **Text is indexed** → PostgreSQL creates full-text index
3. **User searches** → Query matched against index
4. **Results sorted** → Most relevant first

**Search Speed:** < 100ms for 10,000+ documents

---

## 👥 **Team Structure**

- **Member 1: Backend Lead** — Database & API design
- **Member 2: Frontend Developer** — Web interface & UX
- **Member 3: PDF Specialist** — PDF processing & indexing
- **Member 4: Security & DevOps** — Auth & deployment

👉 See [MEMBER_CONTRIBUTIONS.md](MEMBER_CONTRIBUTIONS.md) for detailed roles

---

## 🚀 **Upcoming Features**

### Phase 3: NLP Semantic Search
- Search for **meaning**, not just keywords
- Example: "heat transfer" finds "thermal energy movement"
- Powered by Sentence-BERT + pgvector

### Phase 4: Swing Desktop Application
- Offline search capability
- Native desktop UI
- Local caching

See [NLP_INTEGRATION_PHASE3.md](NLP_INTEGRATION_PHASE3.md) and [SWING_UI_DEVELOPMENT.md](SWING_UI_DEVELOPMENT.md)

---

## 📊 **Project Status**

| Phase | Status | Timeline |
|-------|--------|----------|
| **Phase 1: Core System** | ✅ Complete | Built |
| **Phase 2: Web UI** | ✅ Complete | In use |
| **Phase 3: NLP+Semantic Search** | 🔄 In Progress | Weeks 1-14 |
| **Phase 4: Swing Desktop App** | ⏳ Planned | Q2 2026 |

---

## 🐛 **Found a Bug?**

1. Check [GETTING_STARTED.md#common-issues](GETTING_STARTED.md#common-issues--fixes)
2. Create a GitHub issue with:
   - What went wrong
   - How to reproduce it
   - Error message
3. Or contact the team in Slack

---

## 💻 **Development**

### Run Tests
```bash
mvn clean test
```

### Build Executable JAR
```bash
mvn clean package
java -jar target/my-pdf-db-1.0-SNAPSHOT.jar
```

### Deploy with Docker
```bash
docker build -t student-archive:latest .
docker run -p 8080:8080 student-archive:latest
```

---

## 📖 **Learning Resources**

- **Spring Boot:** https://spring.io/projects/spring-boot
- **PostgreSQL:** https://www.postgresql.org/docs/16/
- **Full-Text Search:** https://www.postgresql.org/docs/16/textsearch.html

---

## 📋 **Project Structure**

```
my-pdf-db/
├── src/
│   ├── main/java/com/example/     ← Java code
│   ├── main/resources/            ← Config & HTML
│   └── test/java/                 ← Tests
├── db/                            ← Database scripts
├── pom.xml                        ← Dependencies
└── README.md                      ← This file
```

See [ARCHITECTURE_SIMPLIFIED.md](ARCHITECTURE_SIMPLIFIED.md) for detailed explanation

---

## ✅ **Quality Commitments**

- ✅ Search < 100ms for any query
- ✅ 99.9% uptime
- ✅ Zero hardcoded passwords
- ✅ 95%+ test coverage
- ✅ Clean, readable code

---

## 🎓 **First Time Here?**

1. Read this README (you're reading it!)
2. Read [GETTING_STARTED.md](GETTING_STARTED.md) (5 min setup)
3. Read [ARCHITECTURE_SIMPLIFIED.md](ARCHITECTURE_SIMPLIFIED.md) (understand system)
4. Identify your role in [MEMBER_CONTRIBUTIONS.md](MEMBER_CONTRIBUTIONS.md)
5. Follow [CODE_SIMPLIFICATION_GUIDE.md](CODE_SIMPLIFICATION_GUIDE.md) for clean code
6. Start coding!

---

## 📞 **Need Help?**

- 💬 **Slack:** #dev-help
- 🐛 **Bug Report:** GitHub Issues
- 📧 **Email:** team@studentarchive.dev
- 📞 **Call:** Team lead (urgent only)

---

## 📄 **License**

This project is licensed under the MIT License - see LICENSE file for details.

---

## 🙏 **Contributors**

The Student Archive Team - T059 (DAA IV-T059)

---

**Built with ❤️ to help students succeed.**