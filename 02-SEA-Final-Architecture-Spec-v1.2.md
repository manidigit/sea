# FlashLearn — SEA Final Architecture Specification

**Document:** `02-SEA-Final-Architecture-Spec-v1.2.md`  
**Status:** FROZEN  
**Predecessor:** `01-Architecture-Design.md`  
**Purpose:** Final architecture specification and source of truth for Stage 03 Transition Specification.

---

## 1. Scope

This document freezes the application architecture after the initial architecture/design stage.

The implementation sequence is:

`Architecture → Transition State Machine → SQLite/Room Schema → Full SQL → DAO/Repository → Application`

A later implementation stage MUST NOT silently change this specification.

---

## 2. Architectural Principles

FlashLearn uses:

- Clean Architecture
- Offline-First local persistence
- Jetpack Compose + Material 3
- Kotlin
- Room / SQLite
- Coroutines + Flow
- Hilt for dependency injection
- Domain logic independent from Android SDK and Room
- Repository interfaces in the Domain layer
- Repository implementations in the Data layer
- Explicit Entity ↔ Domain Model mapping
- Review scheduling implemented as domain logic, not ViewModel logic

### Dependency direction

```text
UI (Compose)
    ↓
ViewModel
    ↓
UseCase
    ↓
Domain Repository Interface
    ↑
Data Repository Implementation
    ↓
DAO
    ↓
Room
    ↓
SQLite
```

The `domain` layer MUST remain independently unit-testable.

---

## 3. Package / Module Structure

```text
app/
└── src/main/java/com/app/flashlearn/
    ├── core/
    │   ├── util/
    │   └── di/
    ├── database/
    │   ├── entity/
    │   ├── dao/
    │   ├── FlashLearnDatabase.kt
    │   └── migration/
    ├── data/
    │   ├── repository/
    │   ├── mapper/
    │   ├── ai/
    │   └── importexport/
    ├── domain/
    │   ├── model/
    │   ├── repository/
    │   └── usecase/
    ├── presentation/
    │   ├── home/
    │   ├── review/
    │   ├── vocabulary/
    │   ├── add/
    │   ├── settings/
    │   └── statistics/
    ├── ui/
    │   ├── theme/
    │   └── components/
    └── navigation/
```

---

## 4. Domain Model

### 4.1 Language

```text
code
displayName
```

`code` is the primary identifier, for example:

- `fa`
- `es`
- `en`

---

### 4.2 Concept

A Concept represents the logical learning item.

```text
id
uuid
contentType
categoryId
favorite
active
createdAt
updatedAt
```

Supported `contentType` values:

```text
WORD
PHRASE
SENTENCE
IDIOM
VERB
EXPRESSION
DIALOGUE
```

`uuid` is unique and is used to prevent collisions during import/export.

`active` provides soft-delete/archive semantics.

---

### 4.3 Content

A Concept does not contain fixed language-specific columns.

Each language-specific representation is stored as a Content record.

```text
id
conceptId
languageCode
text
pronunciation
definition
example
grammarNote
usageNote
```

Required invariant:

```text
UNIQUE(conceptId, languageCode)
```

Therefore, each Concept has at most one Content record per language.

The source-language text is also stored as Content using the active `sourceLanguage`.

The UI determines which Content is displayed as front/back according to the active `LanguagePair`.

---

### 4.4 LearningState

Each Concept has exactly one learning state.

```text
conceptId
stage
difficulty
nextReviewAt
monthlyWrongCount
totalCorrect
totalWrong
lastReviewedAt
```

Allowed stages:

```text
DAILY
WEEKLY
MONTHLY
LEARNED
```

Allowed difficulty values:

```text
EASY
MEDIUM
HARD
VERY_HARD
```

`nextReviewAt` uses Epoch milliseconds.

---

### 4.5 ReviewHistory

Review history is append-only and MUST NOT be deleted as part of normal review-state changes.

```text
id
conceptId
sessionId
reviewStage
reviewDate
isCorrect
previousStatus
newStatus
previousDifficulty
newDifficulty
responseTimeMs
```

Foreign keys:

```text
conceptId → Concept
sessionId → ReviewSession
```

Indexes:

```text
conceptId
sessionId
reviewDate
```

---

### 4.6 ReviewSession

```text
id
startedAt
endedAt
reviewType
```

Example ID:

```text
2026-08-16-001
```

A ReviewSession groups the ReviewHistory records generated during a review session.

---

### 4.7 Category

```text
id
name
isCustom
```

A Concept may reference a Category.

---

### 4.8 Tag

```text
id
name
```

`name` is unique.

---

### 4.9 ConceptTag

Many-to-many relationship:

```text
conceptId
tagId
```

Composite primary key:

```text
(conceptId, tagId)
```

---

### 4.10 LanguagePair

```text
id
sourceLanguage
targetLanguage
isActive
```

The active LanguagePair controls the current learning direction.

Changing the active direction MUST NOT modify Concept data.

---

### 4.11 AppSettings

Simple key-value settings storage for:

- theme
- active language direction
- AI settings
- review settings

API keys MUST NOT be stored in this settings model.

---

## 5. Review Domain Rules

The review algorithm belongs to the Domain layer and MUST NOT be implemented directly inside a ViewModel.

### 5.1 DAILY

```text
Correct:
    stage = WEEKLY
    nextReviewAt = now + 7 days

Incorrect:
    stage = DAILY
    stage does not advance
```

### 5.2 WEEKLY

```text
Correct:
    stage = MONTHLY
    nextReviewAt = now + 30 days

Incorrect:
    stage = DAILY
    difficulty = max(currentDifficulty, MEDIUM)
```

### 5.3 MONTHLY

```text
Correct:
    stage = LEARNED
    if first-time success through all stages:
        difficulty = EASY

Incorrect:
    monthlyWrongCount += 1
    stage = DAILY

    if monthlyWrongCount > 1:
        difficulty = VERY_HARD
    else:
        difficulty = HARD
```

### 5.4 LEARNED

Optional review may create ReviewHistory.

Normal review does not change the stage.

---

## 6. Due-Card Selection

A card is eligible for scheduled review only when its scheduled review time has arrived.

Conceptual query:

```sql
SELECT *
FROM concept c
JOIN learning_state ls
    ON ls.conceptId = c.id
WHERE ls.stage = :stage
  AND ls.nextReviewAt <= :now;
```

This rule applies to scheduled Weekly/Monthly review selection.

Daily items follow the Daily-stage rules defined by the transition specification.

---

## 7. Review Data Integrity

Every submitted review answer MUST produce the required state transition and a corresponding ReviewHistory record.

The history record MUST preserve:

```text
previous state
new state
previous difficulty
new difficulty
review stage
answer correctness
response time
session
timestamp
```

Review history is audit data and is not rewritten to reflect later state changes.

---

## 8. Repository Responsibilities

### Domain repositories

Expose business-oriented interfaces only.

Examples:

```text
ConceptRepository
LearningStateRepository
ReviewRepository
ReviewSessionRepository
LanguageRepository
CategoryRepository
TagRepository
SettingsRepository
```

### Data repositories

Implement Domain repository interfaces and coordinate:

```text
DAO
Mapper
Local data source
AI provider
Import/export services
```

The Domain layer MUST NOT directly depend on Room DAO classes.

---

## 9. Import / Export

Import/export operates on stable `uuid` values.

The import system MUST avoid creating duplicate Concepts when an existing Concept has the same UUID.

Language-specific Content remains normalized and is not represented by fixed columns such as:

```text
spanish_text
english_text
persian_text
```

Instead:

```text
Content(conceptId, languageCode, text, ...)
```

is used.

---

## 10. Offline-First Requirement

Core learning functionality MUST work without network access.

The local Room/SQLite database is the operational source for:

- vocabulary
- translations
- learning state
- review scheduling
- review history
- sessions
- categories
- tags
- language pairs
- settings

AI functionality is an optional external capability and MUST NOT make ordinary vocabulary review dependent on network availability.

---

## 11. Navigation Architecture

Primary navigation:

```text
NavGraph
├── home
├── review
│   ├── reviewTypeSelect
│   └── reviewSession/{type}
├── vocabulary
│   ├── vocabularyList
│   └── conceptDetail/{id}
├── add
│   ├── addManual
│   ├── addAI
│   ├── addPasteText
│   └── addImportFile
└── settings
    ├── settingsHome
    ├── backupRestore
    └── aiSettings
```

Additional destinations:

```text
statistics
onboarding
```

Onboarding is shown only during first-run setup.

---

## 12. User Flow

### First run

```text
Splash
  ↓
Onboarding
  ↓
Select source / target language
  ↓
Home Dashboard
```

### Normal operation

```text
Home
 ├── Daily / Weekly / Monthly
 │      ↓
 │   Review Session
 │      ↓
 │   Result
 │      ↓
 │   Dashboard
 │
 ├── Vocabulary
 │      ↓
 │   Search / Filter / Edit / Delete / Favorite
 │
 ├── Add
 │   ├── Manual
 │   ├── AI
 │   ├── Paste Text
 │   └── Import File
 │
 ├── Statistics
 │
 └── Settings
     ├── Theme
     ├── Language Pair
     ├── Backup / Restore
     └── AI Settings
```

---

## 13. Presentation Architecture

Compose screens consume UI state exposed by ViewModels.

The ViewModel:

- receives user actions
- invokes UseCases
- observes Flow-based state
- exposes immutable UI state

The ViewModel MUST NOT contain the core review transition algorithm.

The review transition MUST be testable independently of Android UI.

---

## 14. Design System Baseline

### Light theme

```text
Primary:    #4F46E5
Secondary:  #14B8A6
Background: #FAFAFA
Surface:    #FFFFFF
Success:    #22C55E
Error:      #EF4444
Warning:    #F59E0B
Due:        #3B82F6
Learned:    #8B5CF6
```

### Dark theme

```text
Primary:    #818CF8
Secondary:  #2DD4BF
Background: #121212
Surface:    #1E1E1E
Success:    #4ADE80
Error:      #F87171
Warning:    #FBBF24
Due:        #60A5FA
Learned:    #A78BFA
```

### Layout

```text
Spacing: 4, 8, 12, 16, 24, 32 dp
Cards: 16 dp radius
Buttons: 12 dp radius
Chips: full-rounded
Elevation: 1–3 dp
```

Typography MUST support both Persian and Latin scripts.

The baseline recommendation is Vazirmatn for Persian with the standard Material font for Latin content.

Icons:

```text
Inactive: Outline
Active/Selected: Filled
```

---

## 15. Architecture Invariants

The following are frozen invariants:

1. `Concept` is language-neutral.
2. Language-specific text is stored in `Content`.
3. `(conceptId, languageCode)` is unique.
4. Every Concept has one LearningState.
5. ReviewHistory is append-only.
6. Review transitions belong to Domain logic.
7. ViewModels do not own review business rules.
8. Domain has no dependency on Android SDK or Room.
9. Data layer performs Entity ↔ Domain mapping.
10. Room/SQLite is the local persistence layer.
11. The app remains usable offline for core learning functions.
12. Language direction is represented by LanguagePair.
13. Changing language direction does not mutate Concept records.
14. Import/export uses stable UUIDs.
15. API keys are not stored in AppSettings.
16. Later stages MUST preserve these architectural invariants unless a new version of this document explicitly supersedes them.

---

## 16. Stage 03 Boundary

Stage 03 MUST convert the review rules in this document into a complete, deterministic and testable state-transition specification.

Stage 03 MUST define, without ambiguity:

- initial LearningState creation
- exact transition conditions
- exact timestamps
- correct/incorrect behavior
- difficulty escalation
- monthlyWrongCount behavior
- ReviewHistory creation
- session lifecycle
- interruption/resume behavior
- duplicate-answer protection
- ordering and eligibility of review cards

Stage 03 is the direct source for the database constraints and queries defined in Stages 04 and 05.

---

## 17. Stage 04 / 05 Boundary

Stage 04 MUST derive:

- Room entities
- relations
- indexes
- constraints
- foreign keys
- migrations
- DAO-facing schema

Stage 05 MUST derive:

- complete SQLite DDL
- indexes
- constraints
- required queries
- seed/reference data where required

Neither Stage 04 nor Stage 05 may invent a different learning algorithm.

---

## 18. Freeze Declaration

**Version:** v1.2  
**Status:** FROZEN  
**Next dependent specification:** `03-Transition-Spec-v1.0.md`

This document is the architectural source of truth for subsequent implementation stages.

Any architectural change after this point requires a new version and MUST NOT silently modify this frozen specification.
