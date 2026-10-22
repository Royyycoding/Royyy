# Topia AI - Intelligent English Learning Assistant

Topia AI is a comprehensive Android application designed to elevate English learning through multimodal AI: multi-turn conversational tutoring, voice speaking practice, camera-based textbook test generation with strict no-hint typing evaluation, and a trilingual vocabulary book with interactive flip flashcards.

### User Review & Critical Decisions

> [!IMPORTANT]
> The plan outlines the full architecture and features for Topia AI. Please review the key design selections below:

- **Strict No-Hint Typing Test**: In compliance with the user brief, the test generator from textbook/worksheet photos extracts fill-in-the-blank, spelling, and sentence completion questions where hints are completely hidden during the test to maximize recall. Instant feedback and detailed grammatical explanations are revealed only after submission.
- **Tri-Fold Vocabulary Card Structure**: Every word card stores (1) English headword, (2) Korean meaning (한국어 뜻), and (3) English definition, complete with pronunciation audio and flashcard flip interactions.
- **Multimodal AI Architecture**: Uses `gemini-3.8-flash` for rapid text chat and camera OCR test extraction, `gemini-4-argon` with high thinking for in-depth grammar analyses, and speech synthesis for pronunciation.
- **Offline-First Persistence**: Leverages Android Room DB for the vocabulary notebook, user-created decks, and test history so learners can review flashcards anywhere.

---

### 1. Overview & Core Concept

- **What It Does**: Topia AI empowers learners to converse freely in English with an intelligent AI tutor, generate customized typing tests by snapping photos of textbook pages or workbooks without giving away answers, and master essential vocabulary using an English-Korean definition flashcard system.
- **Target Audience**: Students, test takers (TOEIC, TOEFL, CSAT/수능), and ESL/EFL learners seeking active recall practice rather than passive multiple-choice drills.
- **Key Value**: Bridges the gap between passive reading and active production through camera-to-test conversion and interactive flashcard mastery.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **AI Chat & Speaking Tutor**:
   - Learner opens the Chat screen, selects a tutor persona (e.g. Daily Conversationalist, Grammar Specialist, Interview Coach), and engages in text or spoken dialogue.
   - Tap-to-listen button speaks AI messages with natural intonation.
2. **Camera Photo Test Generator**:
   - Learner taps "Snap & Test" to either take a photo with the camera or pick an image of a workbook page/article.
   - Gemini analyzes the text and automatically extracts key vocabulary and grammar challenges into a rigorous typing quiz.
   - Learner types answers in focused input fields without hints. Upon submitting, the app scores answers, highlights spelling differences, and displays Korean explanations.
   - One-tap "Add Missed Words to Voca Book" instantly saves challenging terms.
3. **Vocabulary Book & Flashcards**:
   - Browse saved words with Korean meaning and English definition.
   - "Flashcard Mode" launches full-screen animated 3D flip cards: Front shows English word with pronunciation; Back reveals Korean meaning, English definition, and contextual example sentence.
   - "Know" vs "Review Later" sorting keeps track of study progress.

#### Visual Identity & Theme
- **Aesthetic Direction**: Premium modern educational editorial with deep Indigo (`#1A237E`), Electric Cyan (`#00B4D8`), and soft Warm Slate backgrounds (`#F8F9FA` in light, `#121826` in dark).
- **Typography**: Clean, highly legible sans-serif hierarchy with strong heading weights and comfortable line-height for English & Korean bilingual reading.
- **Micro-Interactions**: Smooth 180-degree card flip animation using Compose `graphicsLayer(rotationY)`, animated progress bars for test completion, and haptic feedback on typing verification.

---

### 3. Key Product Decisions & Trade-Offs

- **Typing Test Without Hints**:
  - *Chosen Approach*: Input fields require exact typing without reveal buttons or letter-count indicators during test-taking, followed by fuzzy/exact match scoring and grammatical error breakdown upon completion.
  - *Why*: Research shows active retrieval testing without hints yields significantly higher long-term memory retention than multiple-choice or hint-assisted tests.
- **Camera OCR via Gemini Multimodal REST API**:
  - *Chosen Approach*: Direct image base64 transmission to Gemini API with structured JSON output schema for parsed questions.
  - *Why*: Eliminates the need for heavy on-device OCR models while allowing Gemini to simultaneously parse text, detect difficulty, and formulate contextual typing prompts in a single network pass.
- **Local Room Database for Vocabulary & History**:
  - *Chosen Approach*: SQLite Room DB stores all words, definitions, mastery levels, and past quiz scores locally.
  - *Why*: Instant access, zero network latency when flipping flashcards, and reliable offline review.

---

### 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                      Topia AI Application                    │
├─────────────────────────────────────────────────────────────┤
│                         UI Layer                            │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────────────┐ │
│  │   AI Chat    │ │  Photo Test  │ │ Voca Book & Flashcard│ │
│  │  Tutor View  │ │  Generator   │ │       Deck View      │ │
│  └──────┬───────┘ └──────┬───────┘ └──────────┬───────────┘ │
├─────────┼────────────────┼────────────────────┼─────────────┤
│         ▼                ▼                    ▼             │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                       ViewModels                       │ │
│  │     (ChatViewModel, TestViewModel, VocaViewModel)      │ │
│  └──────┬────────────────┬────────────────────┬───────────┘ │
├─────────┼────────────────┼────────────────────┼─────────────┤
│         ▼                ▼                    ▼             │
│  ┌───────────────────────┐       ┌────────────────────────┐ │
│  │  Gemini Remote API    │       │   Local Room Database  │ │
│  │ - Multimodal Photo Q  │       │ - Vocabulary Entities  │ │
│  │ - Chat Tutor Stream   │       │ - Test Results Record  │ │
│  │ - High-Thinking Notes │       │ - Flashcard Decks      │ │
│  └───────────────────────┘       └────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

#### Core Data Entities
- **VocabularyWord**: `id`, `word`, `koreanMeaning`, `englishDefinition`, `exampleSentence`, `masteryLevel`, `createdAt`
- **TestQuestion**: `id`, `sourcePhotoUri`, `questionText`, `correctAnswer`, `userAnswer`, `isCorrect`, `explanation`
- **ChatMessage**: `id`, `sender` (User/AI), `content`, `timestamp`, `audioUri`
