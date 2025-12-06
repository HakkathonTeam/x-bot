# X-Bot Project Schedule

**Project Duration:** December 8 - December 21, 2024 (14 days)
**Deadline:** December 21 (strict) - everything must be ready

---

## Team Members
- **Ivan** - Team Leader
- **Lex** - Bot Core
- **Alexey** - JSON Parser
- **Vica** - HTML Parser, Mentions
- **Nickolay** - Data/Excel
- **Vladimir** - DevOps & QA

---

## Schedule Overview

| Week | Days | Focus |
|------|------|-------|
| Week 1 | Dec 8-14 | Setup + Core Development |
| Week 2 | Dec 15-20 | Integration + Testing + Polish |
| Deadline | Dec 21 | Final delivery |

---

## Daily Schedule

### WEEK 1: Foundation & Core Development

#### Day 1 - Sunday, December 8
**Focus:** Project Setup

| Who | Task |
|-----|------|
| Ivan | Create GitHub repo, Maven project structure, base interfaces |
| Nickolay | Create all data models (User, ChatMessage, ExtractionResult) |
| Vladimir | Create Dockerfile, docker-compose.yml, .gitignore, .env.example |
| Lex | Study TelegramBots library, set up bot token |
| Alexey | Study Telegram JSON export format, prepare samples |
| Vica | Study Telegram HTML export format, prepare samples |

**End of Day Result:** Project skeleton ready, models defined, Docker config ready

---

#### Day 2 - Monday, December 9
**Focus:** Bot Core + Parser Interfaces

| Who | Task |
|-----|------|
| Ivan | Define ChatHistoryParser interface, review Day 1 code |
| Lex | Implement XBot class, /start command |
| Alexey | Start JsonChatParser implementation |
| Vica | Start HtmlChatParser implementation |
| Nickolay | Start ExcelGenerator - basic workbook creation |
| Vladimir | Set up logback.xml, write first unit tests for models |

**End of Day Result:** Bot responds to /start, both parser skeletons ready

---

#### Day 3 - Tuesday, December 10
**Focus:** Commands + Parsers Core

| Who | Task |
|-----|------|
| Ivan | Code review, help with blockers |
| Lex | Implement /help command, start file upload handler |
| Alexey | Complete JsonChatParser - extract authors |
| Vica | Complete HtmlChatParser - extract authors |
| Nickolay | ExcelGenerator - create sheets, headers, basic styling |
| Vladimir | Unit tests for JsonChatParser |

**End of Day Result:** Bot handles /start, /help; both parsers extract users

---

#### Day 4 - Wednesday, December 11
**Focus:** File Handling + Mentions

| Who | Task |
|-----|------|
| Ivan | Code review, integration planning |
| Lex | Complete file upload handler (single file) |
| Alexey | Handle nested text structures in JSON, edge cases |
| Vica | Implement MentionExtractor (@username regex) |
| Nickolay | ExcelGenerator - populate data rows, auto-size columns |
| Vladimir | Unit tests for HtmlChatParser, MentionExtractor |

**End of Day Result:** Bot accepts file uploads; mentions extracted; Excel generates

---

#### Day 5 - Thursday, December 12
**Focus:** Multi-file + Parser Factory

| Who | Task |
|-----|------|
| Ivan | Code review, start integration work |
| Lex | Implement multiple file handling (up to 10 files) |
| Alexey | Implement ParserFactory (auto-detect JSON/HTML) |
| Vica | Integrate MentionExtractor with both parsers |
| Nickolay | UserExtractor - deduplication, separate participants/mentions/channels |
| Vladimir | Unit tests for ParserFactory, sample test files |

**End of Day Result:** Both parsers working; multi-file upload works; format auto-detection

---

#### Day 6 - Friday, December 13
**Focus:** Output Logic + Integration Start

| Who | Task |
|-----|------|
| Ivan | Integration: connect parsers to bot |
| Lex | Implement output logic (text list < 50, Excel >= 51) |
| Alexey | Filter deleted accounts in JSON |
| Vica | Filter deleted accounts in HTML |
| Nickolay | Finalize Excel with all columns, export date |
| Vladimir | Integration tests with real export files |

**End of Day Result:** Bot can process files and return results

---

#### Day 7 - Saturday, December 14
**Focus:** First Integration Milestone

| Who | Task |
|-----|------|
| All | Full integration testing |
| Ivan | Fix integration bugs, code review |
| Lex | Fix bot issues found in testing |
| Alexey | Fix JSON parser issues found in testing |
| Vica | Fix HTML parser issues found in testing |
| Nickolay | Fix Excel issues found in testing |
| Vladimir | Document all bugs found |

**End of Day Result:** MVP working end-to-end (may have bugs)

---

### WEEK 2: Integration, Testing & Polish

#### Day 8 - Sunday, December 15
**Focus:** Bug Fixes + Edge Cases

| Who | Task |
|-----|------|
| Ivan | Prioritize bugs, assign fixes |
| Lex | Handle edge cases: empty files, wrong format errors |
| Alexey | Handle JSON encoding edge cases |
| Vica | Handle HTML encoding edge cases |
| Nickolay | Handle empty results, large datasets |
| Vladimir | More integration tests, edge case tests |

**End of Day Result:** Major bugs fixed, edge cases handled

---

#### Day 9 - Monday, December 16
**Focus:** Error Handling + UX

| Who | Task |
|-----|------|
| Ivan | Code review, test on real chats |
| Lex | User-friendly error messages, progress indicators |
| Alexey | Improve JSON parser error handling |
| Vica | Improve HTML parser error handling |
| Nickolay | Excel formatting polish |
| Vladimir | Test with various real Telegram exports |

**End of Day Result:** Good error handling, user-friendly messages

---

#### Day 10 - Tuesday, December 17
**Focus:** Privacy + Cleanup

| Who | Task |
|-----|------|
| Ivan | Security review - ensure no data leaks |
| Lex | Verify files deleted after processing |
| Alexey | Verify no temp files left (JSON) |
| Vica | Verify no temp files left (HTML) |
| Nickolay | Verify Excel created in memory only |
| Vladimir | Test privacy: check no files remain on server |

**End of Day Result:** Privacy requirements verified

---

#### Day 11 - Wednesday, December 18
**Focus:** Docker + Deployment Test

| Who | Task |
|-----|------|
| Ivan | Full system test in Docker |
| Vladimir | Test Docker build, fix any issues |
| Vladimir | Test docker-compose up flow |
| Lex | Fix any Docker-related bot issues |
| Alexey + Vica | Cross-test parsers, fix found issues |
| Nickolay | Help with testing |

**End of Day Result:** Bot runs correctly in Docker

---

#### Day 12 - Thursday, December 19
**Focus:** Documentation

| Who | Task |
|-----|------|
| Ivan | Write architecture description |
| Lex | Write bot commands documentation |
| Alexey | Document JSON format support, limitations |
| Vica | Document HTML format support, @mention rules |
| Nickolay | Create example Excel export |
| Vladimir | Write README.md (setup, run, usage instructions) |

**End of Day Result:** All documentation complete

---

#### Day 13 - Friday, December 20
**Focus:** Final Testing + Buffer

| Who | Task |
|-----|------|
| All | Full QA testing |
| Vladimir | Final integration tests |
| Ivan | Final code review |
| All | Fix any last-minute bugs |
| Vladimir | Prepare example exports, screenshots |

**End of Day Result:** Everything tested and working

---

#### Day 14 - Saturday, December 21 (DEADLINE)
**Focus:** Final Delivery

| Who | Task |
|-----|------|
| Ivan | Final check, merge all to main |
| Vladimir | Verify Docker works from clean clone |
| All | Final demo run |
| Ivan | Submit/present project |

**DELIVERY:**
- GitHub repo complete
- Docker working
- Documentation ready
- Example exports ready

---

## Milestones Summary

| Date | Milestone |
|------|-----------|
| Dec 8 | Project setup complete |
| Dec 10 | Bot responds to commands |
| Dec 12 | Both parsers working |
| Dec 14 | **MVP: End-to-end working** |
| Dec 17 | Bug fixes complete |
| Dec 19 | Documentation complete |
| Dec 20 | Final QA complete |
| Dec 21 | **DELIVERY** |

---

## Risk Buffer

- Day 13 (Dec 20) is buffer day for unexpected issues
- If ahead of schedule, use for:
  - Additional testing
  - UI improvements
  - Performance optimization

---

## Daily Standups

Recommended: 15-min daily sync
- What did you complete?
- What will you do today?
- Any blockers?

---

## Critical Path

```
Models (Day 1)
    → JSON Parser (Alexey) ─────┐
    → HTML Parser (Vica)  ──────┼→ Integration (Days 6-7)
    → Bot Core (Lex)      ──────┘        │
    → Excel (Nickolay)    ───────────────┘
                                         │
                                         ▼
                              Testing (Days 8-10)
                                         │
                                         ▼
                              Docker (Day 11)
                                         │
                                         ▼
                              Docs (Day 12)
                                         │
                                         ▼
                              Final QA (Day 13)
                                         │
                                         ▼
                              DELIVERY (Day 14)
```

**Blocking dependencies:**
1. Models must be done before parsers start (Day 1)
2. Both parsers must work before integration (Day 6)
3. Integration must work before serious QA (Day 8)
4. Everything must work before documentation (Day 12)

---

## Parallel Work Streams

```
Stream 1 (Lex):        Bot Core → File Handling → Output Logic → Error Handling
Stream 2 (Alexey):     JSON Parser → ParserFactory → Edge Cases → Docs
Stream 3 (Vica):       HTML Parser → Mentions → Edge Cases → Docs
Stream 4 (Nickolay):   Models → Excel → UserExtractor → Polish
Stream 5 (Vladimir):   Docker → Tests → More Tests → README
Stream 6 (Ivan):       Interfaces → Code Review → Integration → Final Review
```
