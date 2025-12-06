# X-Bot Team Structure & Responsibilities

## Team Members

| Name | Role | Primary Focus |
|------|------|---------------|
| **Ivan** | Team Leader | Architecture, Integration, Code Review |
| **Lex** | Backend Developer | Bot Core, Telegram API |
| **Alexey** | Backend Developer | JSON Parser |
| **Nickolay** | Backend Developer | Excel Generation, Data Models |
| **Vladimir** | DevOps & QA | Docker, Testing, Documentation |
| **Vica** | Backend Developer | HTML Parser, Mention Extraction |

---

## Detailed Responsibilities

### Ivan - Team Leader

**Primary:** Project coordination, architecture decisions, code review

**Tasks:**
- [ ] Define overall architecture and module interfaces
- [ ] Set up project structure (Maven, packages)
- [ ] Create base interfaces (`ChatHistoryParser`, `UserExtractor`)
- [ ] Code review all pull requests
- [ ] Integration of all modules
- [ ] Resolve blockers and technical decisions
- [ ] Final testing and acceptance
- [ ] Coordinate with partner (BotCreators)

**Key Files:**
- `pom.xml`
- `XBotApplication.java`
- All interfaces and base classes

---

### Lex - Bot Core Developer

**Primary:** Telegram Bot implementation, command handlers, file handling

**Tasks:**
- [ ] Set up TelegramBots library integration
- [ ] Implement `XBot.java` main bot class
- [ ] Implement `/start` command handler
- [ ] Implement `/help` command handler
- [ ] Implement file upload handling (single & multiple files)
- [ ] Add file count validation (max 10 files)
- [ ] Send progress/status messages to users
- [ ] Handle output: send text list (< 50 users) or Excel file (>= 51)
- [ ] Error handling and user-friendly messages

**Key Files:**
- `bot/XBot.java`
- `bot/handler/CommandHandler.java`
- `bot/handler/FileHandler.java`
- `util/Constants.java`

**Dependencies:** Needs interfaces from Ivan, Excel bytes from Nickolay

---

### Alexey - JSON Parser Developer

**Primary:** JSON chat history parsing

**Tasks:**
- [ ] Implement `ChatHistoryParser` interface
- [ ] Implement `JsonChatParser` using Jackson
  - Parse message authors (from, from_id)
  - Extract user metadata (firstName, lastName)
  - Handle nested text structures
  - Handle array and object text formats
- [ ] Implement `ParserFactory` for format auto-detection
- [ ] Filter deleted accounts in JSON
- [ ] Handle encoding (UTF-8) correctly

**Key Files:**
- `parser/ChatHistoryParser.java`
- `parser/JsonChatParser.java`
- `parser/ParserFactory.java`

**Dependencies:** Needs data models from Nickolay

---

### Vica - HTML Parser & Mentions Developer

**Primary:** HTML chat history parsing and mention extraction

**Tasks:**
- [ ] Implement `HtmlChatParser` using Jsoup
  - Parse `<div class="message">` blocks
  - Extract author from `<div class="from_name">`
  - Extract text content from `<div class="text">`
  - Handle multiple HTML file parts
- [ ] Implement `MentionExtractor` service
  - Regex for @username extraction
  - Handle mentions in both JSON and HTML text
  - Validate username format (5-32 chars, starts with letter)
- [ ] Filter deleted accounts in HTML
- [ ] Handle encoding (UTF-8) correctly

**Key Files:**
- `parser/HtmlChatParser.java`
- `service/MentionExtractor.java`

**Dependencies:** Needs data models from Nickolay, interface from Alexey

---

### Nickolay - Data & Excel Developer

**Primary:** Data models, Excel generation, user extraction logic

**Tasks:**
- [ ] Create `User` model (record or class)
  - Fields: id, username, firstName, lastName, bio, registrationDate, hasChannel
  - Proper equals/hashCode for deduplication
- [ ] Create `ChatMessage` model
- [ ] Create `ExtractionResult` model (participants, mentions, channels)
- [ ] Implement `UserExtractor` service
  - Deduplicate users (Set-based)
  - Separate participants vs mentions vs channels
- [ ] Implement `ExcelGenerator` using Apache POI
  - Create workbook with 3 sheets: Participants, Mentions, Channels
  - Add headers: Export Date, Username, Name, Description, Registration Date, Has Channel
  - Style headers (bold, background color)
  - Auto-size columns
  - Return as byte array (no file storage)

**Key Files:**
- `model/User.java`
- `model/ChatMessage.java`
- `model/ExtractionResult.java`
- `service/UserExtractor.java`
- `service/ExcelGenerator.java`

**Dependencies:** None (provides models to others)

---

### Vladimir - DevOps & QA

**Primary:** Docker, testing, documentation, CI/CD

**Tasks:**
- [ ] Create `Dockerfile` (eclipse-temurin:21-jre-alpine)
- [ ] Create `docker-compose.yml`
- [ ] Create `.env.example` with BOT_TOKEN, BOT_USERNAME
- [ ] Set up `.gitignore`
- [ ] Configure `logback.xml` for logging
- [ ] Write unit tests (JUnit 5)
  - Test JSON parser with sample data
  - Test HTML parser with sample data
  - Test mention extraction regex
  - Test Excel generation
  - Test user deduplication
- [ ] Create sample Telegram export files for testing
- [ ] Write `README.md` with:
  - Project description
  - How to run (Docker)
  - How to use the bot (user instructions)
  - Technical limitations
- [ ] Create example Excel export for documentation
- [ ] Final QA testing before delivery

**Key Files:**
- `Dockerfile`
- `docker-compose.yml`
- `.env.example`
- `.gitignore`
- `src/main/resources/logback.xml`
- `README.md`
- `src/test/java/**/*Test.java`

**Dependencies:** Needs working code from all team members

---

## Module Dependencies

```
┌─────────────────────────────────────────────────────────────┐
│                      Ivan (Architecture)                     │
│                   Interfaces & Integration                   │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│      Lex      │    │    Alexey     │    │   Nickolay    │
│   Bot Core    │    │  JSON Parser  │    │  Data/Excel   │
│               │    │               │    │               │
│ - Commands    │    │ - JSON parse  │    │ - Models      │
│ - File upload │    │ - Factory     │    │ - Extraction  │
│ - Send result │    │               │    │ - Excel gen   │
└───────┬───────┘    └───────┬───────┘    └───────┬───────┘
        │                    │                    │
        │                    │                    │
        │            ┌───────────────┐            │
        │            │     Vica      │            │
        │            │  HTML Parser  │            │
        │            │               │            │
        │            │ - HTML parse  │            │
        │            │ - Mentions    │            │
        │            └───────┬───────┘            │
        │                    │                    │
        │         ┌──────────┴──────────┐         │
        │         │   All use Models    │         │
        │         │   from Nickolay     │         │
        │         └─────────────────────┘         │
        │                                         │
        └──────────────► Lex uses ◄───────────────┘
                      Excel bytes from
                        Nickolay
                              │
                              ▼
               ┌─────────────────────────────┐
               │          Vladimir           │
               │       DevOps & QA           │
               │                             │
               │ - Docker                    │
               │ - Tests all modules         │
               │ - Documentation             │
               └─────────────────────────────┘
```

---

## Development Order

### Stage 1 - Foundation (Parallel)
| Who | Task |
|-----|------|
| Ivan | Project setup, interfaces |
| Nickolay | Data models |
| Vladimir | Docker setup, .gitignore |

### Stage 2 - Core Development (Parallel)
| Who | Task |
|-----|------|
| Lex | Bot core, commands |
| Alexey | JSON parser, ParserFactory |
| Vica | HTML parser, MentionExtractor |
| Nickolay | Excel generator, UserExtractor |
| Vladimir | Write tests (as code becomes available) |

### Stage 3 - Integration
| Who | Task |
|-----|------|
| Ivan | Integrate all modules |
| Lex | Connect parsers + Excel to bot |
| Alexey + Vica | Cross-test parsers |
| Vladimir | Integration testing |

### Stage 4 - Finalization
| Who | Task |
|-----|------|
| Vladimir | Final QA, README, examples |
| Ivan | Code review, final approval |
| All | Bug fixes |

---

## Communication

- **Code Repository:** GitHub
- **Branching Strategy:**
  - `main` - stable, production-ready
  - `develop` - integration branch
  - `feature/<name>` - individual features
- **Pull Requests:** Required, reviewed by Ivan
- **Merge Policy:** Squash merge to keep history clean

---

## Definition of Done

A task is complete when:
- [ ] Code compiles without errors
- [ ] Unit tests pass
- [ ] Code reviewed by Ivan
- [ ] Merged to develop branch
- [ ] Works in Docker container

---

## Team Contacts

| Name | Role | Focus Area |
|------|------|------------|
| Ivan | Team Leader | Questions about architecture, blockers |
| Lex | Bot Core | Telegram Bot API, file handling |
| Alexey | JSON Parser | JSON parsing, format detection |
| Vica | HTML Parser | HTML parsing, @mentions |
| Nickolay | Data/Excel | Models, Excel output |
| Vladimir | DevOps/QA | Docker, tests, documentation |
