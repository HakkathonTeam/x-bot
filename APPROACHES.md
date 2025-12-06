# X-Bot: Architecture Approaches

## Problem Statement

The task requires extracting user information from Telegram chat exports, including:
- Username (@username)
- Name (First + Last)
- Bio/About
- Registration Date
- Has Channel

**Challenge:** Telegram chat exports contain only `user_id` and `display_name`, NOT `@username` or profile details.

---

## Available Data Sources

| Source | What It Contains | Limitations |
|--------|------------------|-------------|
| **Chat Export (JSON/HTML)** | user_id, display_name, message text | No @username, no bio |
| **@mentions in text** | @username only | No user_id, no name |
| **Bot API** | Full profile | Only if user messaged the bot |
| **Client API (MTProto)** | Full profile | Requires phone auth |

---

## Approach A: Pure Java Bot (MVP)

### Architecture
```
User ──► Telegram Bot (Java) ──► Bot API
                │
                ▼
         Parse chat export
                │
                ▼
         Return available data only
```

### What We Can Extract
| Field | Participants | Mentions |
|-------|--------------|----------|
| User ID | Yes | No |
| Display Name | Yes | No |
| @username | **No** | Yes |
| Bio | No | No |
| Reg Date | No | No |
| Has Channel | No | No |

### Advantages
- Simple architecture
- Fast development (fits 2-week timeline)
- No external dependencies
- No authentication complexity
- No risk of account ban
- Team knows Java

### Weaknesses
- Cannot retrieve @username for message authors
- Cannot get bio, registration date, channel info
- Limited value for "marketing/analytics" use case

### Effort
- **Development:** ~10 days
- **Risk:** Low
- **Completeness:** ~40% of required fields

---

## Approach B: Java Bot + Python Resolver Microservice

### Architecture
```
User ──► Telegram Bot (Java) ──► Bot API
                │
                │ HTTP request
                ▼
         Python Service ──► Client API (Telethon)
         (resolves user IDs)      │
                                  ▼
                           Full user profiles
```

### What We Can Extract
| Field | Participants | Mentions |
|-------|--------------|----------|
| User ID | Yes | Resolved |
| Display Name | Yes | Resolved |
| @username | **Yes** | Yes |
| Bio | **Yes** | **Yes** |
| Reg Date | No* | No* |
| Has Channel | **Partial** | **Partial** |

*Registration date is never available via any API

### Advantages
- Can resolve @username by user_id
- Can get bio/about
- Can detect linked channel
- Java team works on bot (familiar)
- Python service is isolated/small

### Weaknesses
- Two languages/runtimes
- Requires dedicated Telegram account (phone number)
- Docker setup more complex (2 containers)
- Rate limits on user lookups (~30/sec)
- Risk of account ban if aggressive
- Session security concerns

### Effort
- **Development:** ~12-14 days (tight)
- **Risk:** Medium
- **Completeness:** ~70% of required fields

### Additional Requirements
- Phone number for service account
- Python developer (or learning curve)
- api_id + api_hash from my.telegram.org

---

## Approach C: Full Python (Pyrogram/Telethon)

### Architecture
```
User ──► Telegram Bot (Pyrogram)
                │
                ├──► Bot mode: receive files, send results
                │
                └──► Client mode: resolve user profiles
```

### What We Can Extract
| Field | Participants | Mentions |
|-------|--------------|----------|
| User ID | Yes | Resolved |
| Display Name | Yes | Resolved |
| @username | **Yes** | Yes |
| Bio | **Yes** | **Yes** |
| Reg Date | No* | No* |
| Has Channel | **Partial** | **Partial** |

### Advantages
- Single codebase
- Pyrogram/Telethon support both Bot + Client mode
- Best libraries for Telegram
- Simpler Docker (1 container)
- More flexible for future features

### Weaknesses
- Team must learn Python
- Rewrite from scratch (no Java)
- Still requires phone number for client mode
- Same rate limit / ban risks as Approach B

### Effort
- **Development:** ~12-14 days
- **Risk:** Medium-High (new language)
- **Completeness:** ~70% of required fields

---

## Approach D: Java Bot with Optional Resolution

### Architecture
```
User ──► Telegram Bot (Java) ──► Bot API
                │
                ▼
         Parse chat export
                │
           ┌────┴────┐
           ▼         ▼
      Basic Mode   Enhanced Mode
      (no auth)    (with TDLib)
```

### Concept
- MVP works without user resolution (like Approach A)
- Optional: integrate TDLib (Telegram's official C++ library with Java bindings)
- User can provide own session for enhanced mode

### Advantages
- Delivers working MVP fast
- Enhancement can be added later
- Stays in Java ecosystem
- User controls their own session (privacy)

### Weaknesses
- TDLib is complex to integrate
- Enhanced mode requires user auth flow
- More development for full feature

### Effort
- **MVP:** ~10 days
- **Enhanced:** +5-7 days (post-hackathon)
- **Risk:** Low for MVP
- **Completeness:** 40% MVP → 70% Enhanced

---

## Comparison Matrix

| Criteria | A: Pure Java | B: Java+Python | C: Full Python | D: Java+TDLib |
|----------|--------------|----------------|----------------|---------------|
| **Dev Time** | 10 days | 12-14 days | 12-14 days | 10+5 days |
| **Complexity** | Low | Medium | Medium | Medium-High |
| **Username** | No | Yes | Yes | Yes (enhanced) |
| **Bio** | No | Yes | Yes | Yes (enhanced) |
| **Team Skills** | Java ✓ | Java + Python | Python | Java + C++ |
| **Phone Needed** | No | Yes | Yes | Yes (enhanced) |
| **Ban Risk** | None | Medium | Medium | Medium |
| **Docker** | Simple | 2 containers | Simple | Complex |
| **Hackathon Fit** | Best | Tight | Risky | OK |

---

## Risk Analysis

### Account Ban Risk (Approaches B, C, D-enhanced)

| Action | Risk Level |
|--------|------------|
| <100 lookups/day | Low |
| 100-1000 lookups/day | Medium |
| >1000 lookups/day | High |
| Rapid consecutive requests | High |

**Mitigation:**
- Add delays between requests (1-2 sec)
- Cache resolved users
- Use dedicated "burner" account

### Timeline Risk

| Approach | Fits 2 Weeks? |
|----------|---------------|
| A: Pure Java | Yes, comfortable |
| B: Java+Python | Yes, but tight |
| C: Full Python | Risky if team doesn't know Python |
| D: Java+TDLib | MVP yes, enhanced no |

---

## Recommendation

### For Hackathon Deadline (Dec 21)

**Primary: Approach A (Pure Java MVP)**
- Guaranteed delivery
- Document limitations clearly
- Task says "если возможно" (if possible) for bio/reg date

**Optional Enhancement: Approach B or D**
- Add after MVP works
- If time permits before deadline

### Suggested Strategy

```
Week 1 (Dec 8-14):  Build Approach A (full MVP)
                    ↓
                    MVP Working by Dec 14
                    ↓
Week 2 (Dec 15-18): If ahead of schedule → try Approach B
                    If behind → polish MVP
                    ↓
Dec 19-21:          Final testing, documentation
```

---

## Decision Questions for Team

1. **Risk tolerance:** Safe MVP or ambitious full-feature?

2. **Phone number:** Do we have a spare number for service account?

3. **Python skills:** Anyone comfortable with Python?

4. **Partner expectations:** Is "if possible" acceptable for missing fields?

5. **Post-hackathon:** Will project continue after Dec 21?

---

## Quick Reference

| If you want... | Choose |
|----------------|--------|
| Guaranteed delivery | **Approach A** |
| Maximum features | **Approach B or C** |
| Stay in Java only | **Approach A or D** |
| Simplest Docker | **Approach A or C** |
| Best user data | **Approach B or C** |
