# 17 — Future AI

## Status

FUTURE. Do not implement in V1.

## Purpose

The AI assistant should reason about the user's actual study data instead of acting as a generic chatbot.

## Future capabilities

### Progress
- “How am I doing this week?”
- “How many hours did I study?”
- “Did I reach my target?”

### Analysis
- “Why was this week lower?”
- “When do I usually study the longest?”
- “What app do I use most?”

### Planning
- “I have an exam in 10 days.”
- “Create a study plan using my available time.”

### Reflection
- “What should I focus on tomorrow?”
- “What subject have I neglected?”

## Data access model

The AI should use a scoped data service.

```text
AI Chat
  ↓
Intent/Query layer
  ↓
Study Analytics API
  ↓
Authorized user/profile data
  ↓
Response generation
```

The model should not receive unrestricted database credentials.

## Privacy

The user must understand what study data is sent to an AI provider.

Potential future controls:
- AI data sharing toggle
- local-only analytics mode
- delete AI conversation history
- per-query consent for sensitive data

## Hallucination controls

For numeric answers:
- calculate from structured data
- cite date range internally
- distinguish tracked versus manual

Example:
> “Between Sep 15 and Sep 21, automatic tracking recorded 18h 42m.”

Do not make unsupported claims about mental state or personal discipline.

## Offline AI

Not required initially.

When offline:
- core study tracking continues.
- queued AI actions may be handled later if a future architecture supports it.

## AI and target adjustments

AI can recommend target changes, but the user must approve changes.

The AI must not silently modify:
- targets
- Study Apps
- profile security
- notification settings
- cloud permissions
