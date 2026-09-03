-- FlashLearn — Full SQLite SQL Specification
-- Document: 05-Full-SQL-v1.0.md
-- Status: FROZEN
-- Derived from: 04-SQLite-Room-Schema-v1.0.md

PRAGMA foreign_keys = ON;

BEGIN;

CREATE TABLE language (
    code TEXT NOT NULL PRIMARY KEY,
    displayName TEXT NOT NULL
);

CREATE TABLE category (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    isCustom INTEGER NOT NULL DEFAULT 0 CHECK (isCustom IN (0, 1))
);

CREATE TABLE language_pair (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sourceLanguage TEXT NOT NULL,
    targetLanguage TEXT NOT NULL,
    isActive INTEGER NOT NULL DEFAULT 0 CHECK (isActive IN (0, 1)),
    FOREIGN KEY (sourceLanguage) REFERENCES language(code) ON DELETE RESTRICT,
    FOREIGN KEY (targetLanguage) REFERENCES language(code) ON DELETE RESTRICT,
    UNIQUE (sourceLanguage, targetLanguage),
    CHECK (sourceLanguage <> targetLanguage)
);

CREATE TABLE concept (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    uuid TEXT NOT NULL UNIQUE,
    contentType TEXT NOT NULL CHECK (
        contentType IN (
            'WORD', 'PHRASE', 'SENTENCE', 'IDIOM',
            'VERB', 'EXPRESSION', 'DIALOGUE'
        )
    ),
    categoryId INTEGER,
    favorite INTEGER NOT NULL DEFAULT 0 CHECK (favorite IN (0, 1)),
    active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL,
    FOREIGN KEY (categoryId) REFERENCES category(id) ON DELETE SET NULL
);

CREATE TABLE content (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    conceptId INTEGER NOT NULL,
    languageCode TEXT NOT NULL,
    text TEXT NOT NULL,
    pronunciation TEXT,
    definition TEXT,
    example TEXT,
    grammarNote TEXT,
    usageNote TEXT,
    FOREIGN KEY (conceptId) REFERENCES concept(id) ON DELETE CASCADE,
    FOREIGN KEY (languageCode) REFERENCES language(code) ON DELETE RESTRICT,
    UNIQUE (conceptId, languageCode)
);

CREATE TABLE learning_state (
    conceptId INTEGER PRIMARY KEY,
    stage TEXT NOT NULL CHECK (
        stage IN ('DAILY', 'WEEKLY', 'MONTHLY', 'LEARNED')
    ),
    difficulty TEXT NOT NULL CHECK (
        difficulty IN ('EASY', 'MEDIUM', 'HARD', 'VERY_HARD')
    ),
    nextReviewAt INTEGER,
    monthlyWrongCount INTEGER NOT NULL DEFAULT 0
        CHECK (monthlyWrongCount >= 0),
    totalCorrect INTEGER NOT NULL DEFAULT 0
        CHECK (totalCorrect >= 0),
    totalWrong INTEGER NOT NULL DEFAULT 0
        CHECK (totalWrong >= 0),
    lastReviewedAt INTEGER,
    FOREIGN KEY (conceptId) REFERENCES concept(id) ON DELETE CASCADE,
    CHECK (
        (stage = 'LEARNED' AND nextReviewAt IS NULL)
        OR
        (stage <> 'LEARNED' AND nextReviewAt IS NOT NULL)
    )
);

CREATE TABLE review_session (
    id TEXT PRIMARY KEY,
    startedAt INTEGER NOT NULL,
    endedAt INTEGER,
    reviewType TEXT NOT NULL CHECK (
        reviewType IN ('DAILY', 'WEEKLY', 'MONTHLY', 'LEARNED')
    ),
    CHECK (endedAt IS NULL OR endedAt >= startedAt)
);

CREATE TABLE review_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    conceptId INTEGER NOT NULL,
    sessionId TEXT NOT NULL,
    reviewAttemptId TEXT NOT NULL,
    reviewStage TEXT NOT NULL CHECK (
        reviewStage IN ('DAILY', 'WEEKLY', 'MONTHLY', 'LEARNED')
    ),
    reviewDate INTEGER NOT NULL,
    isCorrect INTEGER NOT NULL CHECK (isCorrect IN (0, 1)),
    previousStatus TEXT NOT NULL CHECK (
        previousStatus IN ('DAILY', 'WEEKLY', 'MONTHLY', 'LEARNED')
    ),
    newStatus TEXT NOT NULL CHECK (
        newStatus IN ('DAILY', 'WEEKLY', 'MONTHLY', 'LEARNED')
    ),
    previousDifficulty TEXT NOT NULL CHECK (
        previousDifficulty IN ('EASY', 'MEDIUM', 'HARD', 'VERY_HARD')
    ),
    newDifficulty TEXT NOT NULL CHECK (
        newDifficulty IN ('EASY', 'MEDIUM', 'HARD', 'VERY_HARD')
    ),
    responseTimeMs INTEGER CHECK (
        responseTimeMs IS NULL OR responseTimeMs >= 0
    ),
    FOREIGN KEY (conceptId) REFERENCES concept(id) ON DELETE CASCADE,
    FOREIGN KEY (sessionId) REFERENCES review_session(id) ON DELETE RESTRICT,
    UNIQUE (sessionId, reviewAttemptId)
);

CREATE TABLE tag (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE concept_tag (
    conceptId INTEGER NOT NULL,
    tagId INTEGER NOT NULL,
    PRIMARY KEY (conceptId, tagId),
    FOREIGN KEY (conceptId) REFERENCES concept(id) ON DELETE CASCADE,
    FOREIGN KEY (tagId) REFERENCES tag(id) ON DELETE CASCADE
);

CREATE TABLE app_setting (
    key TEXT PRIMARY KEY,
    value TEXT,
    updatedAt INTEGER NOT NULL
);

CREATE INDEX idx_language_pair_active
    ON language_pair(isActive);

CREATE INDEX idx_concept_category
    ON concept(categoryId);

CREATE INDEX idx_concept_active
    ON concept(active);

CREATE INDEX idx_concept_favorite
    ON concept(favorite);

CREATE INDEX idx_concept_contentType
    ON concept(contentType);

CREATE INDEX idx_content_concept
    ON content(conceptId);

CREATE INDEX idx_content_language
    ON content(languageCode);

CREATE INDEX idx_content_language_text
    ON content(languageCode, text);

CREATE INDEX idx_learning_state_stage
    ON learning_state(stage);

CREATE INDEX idx_learning_state_due
    ON learning_state(stage, nextReviewAt);

CREATE INDEX idx_learning_state_difficulty
    ON learning_state(difficulty);

CREATE INDEX idx_review_session_startedAt
    ON review_session(startedAt);

CREATE INDEX idx_review_session_type
    ON review_session(reviewType);

CREATE INDEX idx_review_history_concept
    ON review_history(conceptId);

CREATE INDEX idx_review_history_session
    ON review_history(sessionId);

CREATE INDEX idx_review_history_date
    ON review_history(reviewDate);

CREATE INDEX idx_review_history_stage
    ON review_history(reviewStage);

CREATE INDEX idx_review_history_concept_date
    ON review_history(conceptId, reviewDate);

CREATE INDEX idx_concept_tag_tag
    ON concept_tag(tagId);

-- At most one active language pair.
CREATE UNIQUE INDEX uq_language_pair_one_active
    ON language_pair(isActive)
    WHERE isActive = 1;

COMMIT;


-- ============================================================
-- REQUIRED INSERTS / UPDATES
-- ============================================================

-- New Concept + initial LearningState.
-- Both operations belong in one application transaction.
--
-- INSERT INTO concept (
--     uuid, contentType, categoryId, favorite, active, createdAt, updatedAt
-- ) VALUES (?, ?, ?, 0, 1, ?, ?);
--
-- INSERT INTO learning_state (
--     conceptId, stage, difficulty, nextReviewAt,
--     monthlyWrongCount, totalCorrect, totalWrong, lastReviewedAt
-- ) VALUES (?, 'DAILY', 'EASY', ?, 0, 0, 0, NULL);


-- ============================================================
-- REVIEW SELECTION
-- ============================================================

-- DAILY
SELECT
    c.id,
    c.uuid,
    c.contentType,
    c.categoryId,
    c.favorite,
    c.active,
    ls.stage,
    ls.difficulty,
    ls.nextReviewAt
FROM concept AS c
JOIN learning_state AS ls
    ON ls.conceptId = c.id
WHERE c.active = 1
  AND ls.stage = 'DAILY'
  AND ls.nextReviewAt <= :now
  AND NOT EXISTS (
      SELECT 1
      FROM review_history AS rh
      WHERE rh.conceptId = c.id
        AND rh.sessionId = :sessionId
  )
ORDER BY ls.nextReviewAt ASC, c.id ASC;


-- WEEKLY
SELECT
    c.id,
    c.uuid,
    c.contentType,
    c.categoryId,
    c.favorite,
    c.active,
    ls.stage,
    ls.difficulty,
    ls.nextReviewAt
FROM concept AS c
JOIN learning_state AS ls
    ON ls.conceptId = c.id
WHERE c.active = 1
  AND ls.stage = 'WEEKLY'
  AND ls.nextReviewAt <= :now
  AND NOT EXISTS (
      SELECT 1
      FROM review_history AS rh
      WHERE rh.conceptId = c.id
        AND rh.sessionId = :sessionId
  )
ORDER BY ls.nextReviewAt ASC, c.id ASC;


-- MONTHLY
SELECT
    c.id,
    c.uuid,
    c.contentType,
    c.categoryId,
    c.favorite,
    c.active,
    ls.stage,
    ls.difficulty,
    ls.nextReviewAt
FROM concept AS c
JOIN learning_state AS ls
    ON ls.conceptId = c.id
WHERE c.active = 1
  AND ls.stage = 'MONTHLY'
  AND ls.nextReviewAt <= :now
  AND NOT EXISTS (
      SELECT 1
      FROM review_history AS rh
      WHERE rh.conceptId = c.id
        AND rh.sessionId = :sessionId
  )
ORDER BY ls.nextReviewAt ASC, c.id ASC;


-- OPTIONAL LEARNED
SELECT
    c.id,
    c.uuid,
    c.contentType,
    c.categoryId,
    c.favorite,
    c.active,
    ls.stage,
    ls.difficulty,
    ls.nextReviewAt
FROM concept AS c
JOIN learning_state AS ls
    ON ls.conceptId = c.id
WHERE c.active = 1
  AND ls.stage = 'LEARNED'
  AND NOT EXISTS (
      SELECT 1
      FROM review_history AS rh
      WHERE rh.conceptId = c.id
        AND rh.sessionId = :sessionId
  )
ORDER BY c.id ASC;


-- ============================================================
-- CONTENT / VOCABULARY QUERIES
-- ============================================================

SELECT *
FROM content
WHERE conceptId = :conceptId
ORDER BY languageCode ASC;

SELECT *
FROM content
WHERE conceptId = :conceptId
  AND languageCode = :languageCode
LIMIT 1;

SELECT c.*, ct.*
FROM concept AS c
JOIN content AS ct
    ON ct.conceptId = c.id
WHERE c.active = 1
  AND ct.languageCode = :languageCode
  AND ct.text LIKE :query
ORDER BY c.id ASC;


-- ============================================================
-- REVIEW SESSION
-- ============================================================

INSERT INTO review_session (
    id, startedAt, endedAt, reviewType
) VALUES (
    :sessionId, :startedAt, NULL, :reviewType
);

UPDATE review_session
SET endedAt = :endedAt
WHERE id = :sessionId
  AND endedAt IS NULL;


-- ============================================================
-- IDEMPOTENT REVIEW PERSISTENCE
-- ============================================================

-- The application MUST calculate the transition from the current
-- persisted state before issuing this update.
--
-- The expected current stage/difficulty prevent stale UI state from
-- overwriting a newer transition.

UPDATE learning_state
SET
    stage = :newStage,
    difficulty = :newDifficulty,
    nextReviewAt = :newNextReviewAt,
    monthlyWrongCount = :newMonthlyWrongCount,
    totalCorrect = :newTotalCorrect,
    totalWrong = :newTotalWrong,
    lastReviewedAt = :now
WHERE conceptId = :conceptId
  AND stage = :expectedStage
  AND difficulty = :expectedDifficulty;


-- The following insert is the second half of the same transaction.
-- UNIQUE(sessionId, reviewAttemptId) prevents duplicate retries.

INSERT INTO review_history (
    conceptId,
    sessionId,
    reviewAttemptId,
    reviewStage,
    reviewDate,
    isCorrect,
    previousStatus,
    newStatus,
    previousDifficulty,
    newDifficulty,
    responseTimeMs
) VALUES (
    :conceptId,
    :sessionId,
    :reviewAttemptId,
    :reviewStage,
    :now,
    :isCorrect,
    :previousStatus,
    :newStatus,
    :previousDifficulty,
    :newDifficulty,
    :responseTimeMs
);


-- ============================================================
-- LOOKUPS
-- ============================================================

SELECT *
FROM learning_state
WHERE conceptId = :conceptId;

SELECT *
FROM review_history
WHERE conceptId = :conceptId
ORDER BY reviewDate DESC, id DESC;

SELECT *
FROM review_history
WHERE sessionId = :sessionId
ORDER BY id ASC;

SELECT *
FROM review_history
WHERE sessionId = :sessionId
  AND reviewAttemptId = :reviewAttemptId
LIMIT 1;

SELECT *
FROM language_pair
WHERE isActive = 1
LIMIT 1;

SELECT *
FROM app_setting
WHERE key = :key
LIMIT 1;


-- ============================================================
-- BACKUP / RESTORE ORDER
-- ============================================================

-- Recommended restore order:
--
-- 1. language
-- 2. category
-- 3. tag
-- 4. language_pair
-- 5. concept
-- 6. content
-- 7. learning_state
-- 8. review_session
-- 9. review_history
-- 10. concept_tag
-- 11. app_setting
--
-- Foreign keys must remain enabled during a valid restore.
