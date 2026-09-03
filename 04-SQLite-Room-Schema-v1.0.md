# FlashLearn — SQLite / Room Schema Specification

**Document:** `04-SQLite-Room-Schema-v1.0.md`  
**Status:** FROZEN  
**Predecessors:** `02-SEA-Final-Architecture-Spec-v1.2.md`, `03-Transition-Spec-v1.0.md`  
**Purpose:** Define the canonical SQLite/Room schema derived from the frozen architecture and transition rules.

---

## 1. Scope

This document defines:

- database tables
- primary keys
- foreign keys
- unique constraints
- indexes
- nullability
- data types
- Room entity boundaries
- relationships
- migration requirements
- persistence constraints required by the review state machine

This document does **not** redefine learning transitions. Those are owned by Stage 03.

---

## 2. Database

Recommended Room database:

```kotlin
@Database(
    entities = [
        LanguageEntity::class,
        LanguagePairEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        ConceptEntity::class,
        ContentEntity::class,
        LearningStateEntity::class,
        ReviewSessionEntity::class,
        ReviewHistoryEntity::class,
        ConceptTagEntity::class,
        AppSettingEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class FlashLearnDatabase : RoomDatabase()
```

Database engine:

```text
SQLite via Android Room
```

The schema uses foreign-key enforcement and indexed foreign-key columns.

---

## 3. Conventions

### IDs

Internal relational IDs use:

```text
INTEGER PRIMARY KEY
```

Concept also carries a stable external UUID:

```text
TEXT NOT NULL UNIQUE
```

UUID is used for import/export identity.

### Timestamps

All timestamps are stored as:

```text
INTEGER
```

representing Unix Epoch milliseconds.

### Booleans

SQLite/Room representation:

```text
INTEGER
0 = false
1 = true
```

### Enums

Enum values are stored as explicit stable strings.

This avoids coupling persisted values to Kotlin enum ordinal positions.

---

# 4. Tables

## 4.1 language

Stores supported languages.

```text
language
--------
code             TEXT PRIMARY KEY
displayName      TEXT NOT NULL
```

Constraints:

```text
PRIMARY KEY(code)
```

Example:

```text
fa
es
en
```

---

## 4.2 language_pair

Stores language directions.

```text
language_pair
-------------
id                    INTEGER PRIMARY KEY
sourceLanguage        TEXT NOT NULL
targetLanguage        TEXT NOT NULL
isActive              INTEGER NOT NULL DEFAULT 0
```

Foreign keys:

```text
sourceLanguage → language.code
targetLanguage → language.code
```

Constraint:

```text
UNIQUE(sourceLanguage, targetLanguage)
```

Index:

```text
idx_language_pair_active(isActive)
```

Application invariant:

```text
at most one active pair
```

If enforced at database level, Stage 05 MUST implement the corresponding SQLite-compatible constraint/index strategy.

---

## 4.3 category

```text
category
--------
id             INTEGER PRIMARY KEY
name           TEXT NOT NULL
isCustom       INTEGER NOT NULL DEFAULT 0
```

Constraint:

```text
UNIQUE(name)
```

---

## 4.4 concept

Language-neutral learning item.

```text
concept
-------
id             INTEGER PRIMARY KEY
uuid           TEXT NOT NULL UNIQUE
contentType    TEXT NOT NULL
categoryId     INTEGER NULL
favorite       INTEGER NOT NULL DEFAULT 0
active         INTEGER NOT NULL DEFAULT 1
createdAt      INTEGER NOT NULL
updatedAt      INTEGER NOT NULL
```

Foreign key:

```text
categoryId → category.id
```

Delete behavior:

```text
ON DELETE SET NULL
```

Indexes:

```text
idx_concept_category(categoryId)
idx_concept_active(active)
idx_concept_favorite(favorite)
idx_concept_contentType(contentType)
```

Allowed `contentType` values:

```text
WORD
PHRASE
SENTENCE
IDIOM
VERB
EXPRESSION
DIALOGUE
```

The database stores the value as TEXT.

---

## 4.5 content

Language-specific representation of a Concept.

```text
content
-------
id              INTEGER PRIMARY KEY
conceptId       INTEGER NOT NULL
languageCode    TEXT NOT NULL
text            TEXT NOT NULL
pronunciation   TEXT NULL
definition      TEXT NULL
example         TEXT NULL
grammarNote     TEXT NULL
usageNote       TEXT NULL
```

Foreign keys:

```text
conceptId → concept.id
languageCode → language.code
```

Delete behavior:

```text
conceptId:
    ON DELETE CASCADE

languageCode:
    ON DELETE RESTRICT
```

Constraint:

```text
UNIQUE(conceptId, languageCode)
```

Indexes:

```text
idx_content_concept(conceptId)
idx_content_language(languageCode)
idx_content_language_text(languageCode, text)
```

A Concept can therefore contain:

```text
one Spanish Content
one English Content
one Persian Content
...
```

without changing the schema.

---

## 4.6 learning_state

Exactly one current learning state per Concept.

```text
learning_state
-------------
conceptId             INTEGER PRIMARY KEY
stage                 TEXT NOT NULL
difficulty            TEXT NOT NULL
nextReviewAt          INTEGER NULL
monthlyWrongCount     INTEGER NOT NULL DEFAULT 0
totalCorrect          INTEGER NOT NULL DEFAULT 0
totalWrong            INTEGER NOT NULL DEFAULT 0
lastReviewedAt        INTEGER NULL
```

Foreign key:

```text
conceptId → concept.id
```

Delete behavior:

```text
ON DELETE CASCADE
```

The primary key is also the foreign key.

Therefore:

```text
Concept 1 ─── 1 LearningState
```

Allowed stage values:

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

Checks:

```text
monthlyWrongCount >= 0
totalCorrect >= 0
totalWrong >= 0
```

For `LEARNED` state:

```text
nextReviewAt IS NULL
```

For scheduled states:

```text
nextReviewAt IS NOT NULL
```

These invariants may be enforced either through SQLite checks or application/domain validation, but Stage 05 MUST preserve them.

Indexes:

```text
idx_learning_state_stage(stage)
idx_learning_state_due(stage, nextReviewAt)
idx_learning_state_difficulty(difficulty)
```

The composite due index is the primary index for review selection.

---

## 4.7 review_session

Groups review attempts.

```text
review_session
--------------
id             TEXT PRIMARY KEY
startedAt      INTEGER NOT NULL
endedAt        INTEGER NULL
reviewType     TEXT NOT NULL
```

`id` is a stable session identifier.

Recommended review types:

```text
DAILY
WEEKLY
MONTHLY
LEARNED
```

Indexes:

```text
idx_review_session_startedAt(startedAt)
idx_review_session_type(reviewType)
```

A session with no answers is valid.

---

## 4.8 review_history

Append-only audit record for accepted answers.

```text
review_history
--------------
id                    INTEGER PRIMARY KEY
conceptId             INTEGER NOT NULL
sessionId             TEXT NOT NULL
reviewAttemptId       TEXT NOT NULL
reviewStage           TEXT NOT NULL
reviewDate            INTEGER NOT NULL
isCorrect             INTEGER NOT NULL
previousStatus        TEXT NOT NULL
newStatus             TEXT NOT NULL
previousDifficulty    TEXT NOT NULL
newDifficulty         TEXT NOT NULL
responseTimeMs        INTEGER NULL
```

Foreign keys:

```text
conceptId → concept.id
sessionId → review_session.id
```

Delete behavior:

```text
conceptId → ON DELETE CASCADE
sessionId → ON DELETE RESTRICT
```

Idempotency constraint:

```text
UNIQUE(sessionId, reviewAttemptId)
```

Indexes:

```text
idx_review_history_concept(conceptId)
idx_review_history_session(sessionId)
idx_review_history_date(reviewDate)
idx_review_history_stage(reviewStage)
idx_review_history_concept_date(conceptId, reviewDate)
```

ReviewHistory MUST NOT be updated during ordinary state transitions.

---

## 4.9 tag

```text
tag
---
id             INTEGER PRIMARY KEY
name           TEXT NOT NULL UNIQUE
```

---

## 4.10 concept_tag

Many-to-many relation.

```text
concept_tag
-----------
conceptId      INTEGER NOT NULL
tagId          INTEGER NOT NULL
```

Primary key:

```text
PRIMARY KEY(conceptId, tagId)
```

Foreign keys:

```text
conceptId → concept.id ON DELETE CASCADE
tagId → tag.id ON DELETE CASCADE
```

Index:

```text
idx_concept_tag_tag(tagId)
```

---

## 4.11 app_setting

Simple application settings.

```text
app_setting
-----------
key            TEXT PRIMARY KEY
value          TEXT NULL
updatedAt      INTEGER NOT NULL
```

Examples:

```text
theme
activeLanguagePairId
reviewSettings
onboardingCompleted
```

API credentials MUST NOT be persisted here.

---

# 5. Entity Relationship Diagram

```text
language
   │
   ├──────────────┐
   │              │
   ▼              ▼
content       language_pair
   │
   │
   ▼
concept ───────── category
   │
   ├────────────── learning_state
   │
   ├────────────── review_history ────── review_session
   │
   └────────────── concept_tag ───────── tag
```

Cardinality:

```text
Language       1 ─── N Content
Language       1 ─── N LanguagePair
Category       1 ─── N Concept
Concept        1 ─── N Content
Concept        1 ─── 1 LearningState
Concept        1 ─── N ReviewHistory
ReviewSession  1 ─── N ReviewHistory
Concept        N ─── N Tag
```

---

# 6. Room Entity Definitions

## 6.1 ConceptEntity

```kotlin
@Entity(
    tableName = "concept",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["categoryId"]),
        Index(value = ["active"]),
        Index(value = ["favorite"]),
        Index(value = ["contentType"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class ConceptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String,
    val contentType: String,
    val categoryId: Long?,
    val favorite: Boolean,
    val active: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
```

---

## 6.2 ContentEntity

```kotlin
@Entity(
    tableName = "content",
    indices = [
        Index(value = ["conceptId"]),
        Index(value = ["languageCode"]),
        Index(value = ["conceptId", "languageCode"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = ConceptEntity::class,
            parentColumns = ["id"],
            childColumns = ["conceptId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LanguageEntity::class,
            parentColumns = ["code"],
            childColumns = ["languageCode"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class ContentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conceptId: Long,
    val languageCode: String,
    val text: String,
    val pronunciation: String?,
    val definition: String?,
    val example: String?,
    val grammarNote: String?,
    val usageNote: String?
)
```

---

## 6.3 LearningStateEntity

```kotlin
@Entity(
    tableName = "learning_state",
    indices = [
        Index(value = ["stage"]),
        Index(value = ["stage", "nextReviewAt"]),
        Index(value = ["difficulty"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ConceptEntity::class,
            parentColumns = ["id"],
            childColumns = ["conceptId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LearningStateEntity(
    @PrimaryKey
    val conceptId: Long,
    val stage: String,
    val difficulty: String,
    val nextReviewAt: Long?,
    val monthlyWrongCount: Int,
    val totalCorrect: Int,
    val totalWrong: Int,
    val lastReviewedAt: Long?
)
```

---

## 6.4 ReviewSessionEntity

```kotlin
@Entity(
    tableName = "review_session",
    indices = [
        Index(value = ["startedAt"]),
        Index(value = ["reviewType"])
    ]
)
data class ReviewSessionEntity(
    @PrimaryKey
    val id: String,
    val startedAt: Long,
    val endedAt: Long?,
    val reviewType: String
)
```

---

## 6.5 ReviewHistoryEntity

```kotlin
@Entity(
    tableName = "review_history",
    indices = [
        Index(value = ["conceptId"]),
        Index(value = ["sessionId"]),
        Index(value = ["reviewDate"]),
        Index(value = ["reviewStage"]),
        Index(value = ["conceptId", "reviewDate"]),
        Index(value = ["sessionId", "reviewAttemptId"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = ConceptEntity::class,
            parentColumns = ["id"],
            childColumns = ["conceptId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ReviewSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class ReviewHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conceptId: Long,
    val sessionId: String,
    val reviewAttemptId: String,
    val reviewStage: String,
    val reviewDate: Long,
    val isCorrect: Boolean,
    val previousStatus: String,
    val newStatus: String,
    val previousDifficulty: String,
    val newDifficulty: String,
    val responseTimeMs: Long?
)
```

---

# 7. DAOs Required by Schema

The following DAO boundaries are required.

## ConceptDao

```text
insert
update
archive/deactivate
findById
findByUuid
search
observeAll
observeFavorites
```

## ContentDao

```text
insert
update
delete
findByConcept
findByConceptAndLanguage
searchByText
```

## LearningStateDao

```text
insert
update
findByConcept
observeByConcept
findDueByStage
```

## ReviewSessionDao

```text
insert
closeSession
findById
observeById
```

## ReviewHistoryDao

```text
insert
findByConcept
findBySession
findByAttempt
```

## CategoryDao

```text
insert
update
delete
observeAll
```

## TagDao

```text
insert
delete
findByName
observeAll
```

## ConceptTagDao

```text
insert
delete
findTagsForConcept
findConceptsForTag
```

## LanguageDao

```text
insert
findByCode
observeAll
```

## LanguagePairDao

```text
insert
update
findActive
observeActive
```

## AppSettingDao

```text
get
upsert
delete
observe
```

---

# 8. Required Transaction

The accepted-review persistence operation MUST be exposed through a transaction boundary above individual DAO calls.

Logical operation:

```text
BEGIN

1. Read current LearningState
2. Validate expected current state
3. Calculate transition in Domain
4. Update LearningState
5. Insert ReviewHistory
6. Commit

END
```

A failed transaction MUST leave the database in its previous consistent state.

---

# 9. Optimistic State Validation

Before applying a transition, persistence should verify that the state being updated is still the state from which the transition was calculated.

At minimum, the update condition should identify:

```text
conceptId
current stage
current difficulty
```

or use an equivalent version/token mechanism.

If no row is updated because the expected state no longer matches, the operation MUST be retried from the latest persisted state rather than applying a stale transition.

This prevents double-submit and concurrent-session corruption.

---

# 10. Foreign-Key Policy

Foreign keys are mandatory for relational integrity.

Summary:

```text
concept.categoryId
    → category.id
    ON DELETE SET NULL

content.conceptId
    → concept.id
    ON DELETE CASCADE

content.languageCode
    → language.code
    ON DELETE RESTRICT

learning_state.conceptId
    → concept.id
    ON DELETE CASCADE

review_history.conceptId
    → concept.id
    ON DELETE CASCADE

review_history.sessionId
    → review_session.id
    ON DELETE RESTRICT

concept_tag.conceptId
    → concept.id
    ON DELETE CASCADE

concept_tag.tagId
    → tag.id
    ON DELETE CASCADE
```

---

# 11. Index Strategy

The schema intentionally indexes:

### Review scheduling

```text
learning_state(stage, nextReviewAt)
```

Primary use:

```text
WHERE stage = ?
  AND nextReviewAt <= ?
```

### Content lookup

```text
content(conceptId)
content(languageCode)
content(conceptId, languageCode)
```

### History

```text
review_history(conceptId)
review_history(sessionId)
review_history(reviewDate)
review_history(conceptId, reviewDate)
```

### Relationships

```text
concept_tag(tagId)
```

Indexes MUST be justified by actual query patterns; redundant indexes SHOULD NOT be introduced in Stage 05.

---

# 12. Soft Delete

Concept deletion from the user-facing vocabulary list should normally use:

```text
active = 0
```

rather than physically deleting the Concept.

This preserves:

- Concept identity
- LearningState
- ReviewHistory
- import/export identity

A hard delete, if ever exposed as a future maintenance operation, is a separate destructive operation and is not part of ordinary vocabulary management.

---

# 13. Import / Export Constraints

Import MUST resolve Concepts using:

```text
concept.uuid
```

not the local integer primary key.

When a UUID already exists:

```text
update/merge existing Concept
```

must be used according to the import policy.

When a UUID does not exist:

```text
create Concept
create Content records
create LearningState
```

The import operation MUST preserve:

```text
uuid
content language
learning state
difficulty
review counters
```

when those fields are present in the supported export format.

---

# 14. Initial LearningState Creation

Every newly created Concept that enters the learning system MUST receive:

```text
stage = DAILY
difficulty = EASY
monthlyWrongCount = 0
totalCorrect = 0
totalWrong = 0
lastReviewedAt = NULL
nextReviewAt = creation time
```

Creation of Concept and its initial LearningState SHOULD occur in one transaction.

---

# 15. Migration Policy

Room schema version starts at:

```text
version = 1
```

Every future schema change MUST:

1. increment the Room version
2. provide an explicit Migration
3. preserve user data
4. update exported Room schema
5. update this specification's version if the logical schema changes
6. add migration tests

Destructive fallback migrations MUST NOT be used for production data.

---

# 16. Schema-Level Invariants

The following are frozen:

1. Concept identity is an internal integer ID plus stable UUID.
2. Content is normalized by Concept and language.
3. `(conceptId, languageCode)` is unique.
4. Each Concept has one LearningState.
5. LearningState primary key equals Concept foreign key.
6. ReviewHistory is append-only.
7. `(sessionId, reviewAttemptId)` is unique.
8. ReviewSession groups ReviewHistory.
9. Core review timestamps are Epoch milliseconds.
10. Enum-like values are persisted as stable TEXT values.
11. Due-review selection uses `(stage, nextReviewAt)`.
12. Foreign keys are enforced.
13. Core review state and history are persisted transactionally.
14. API keys are not stored in `app_setting`.
15. UUID, not local integer ID, is the import/export identity.
16. Ordinary deletion is soft deletion through `concept.active`.
17. Schema does not redefine Stage 03 learning rules.

---

# 17. Stage 05 Boundary

Stage 05, `05-Full-SQL-v1.0.md`, MUST translate this document into executable SQLite DDL and required SQL statements.

Stage 05 MUST NOT:

- add alternative learning states
- change column semantics
- remove required indexes
- change foreign-key behavior
- replace UUID identity with local IDs
- redefine review transitions

Any such change requires a new schema version.

---

# 18. Freeze Declaration

**Version:** v1.0  
**Status:** FROZEN  
**Next document:** `05-Full-SQL-v1.0.md`

This specification is the canonical database/Room schema for the current FlashLearn architecture.
