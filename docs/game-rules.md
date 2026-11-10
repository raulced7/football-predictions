# Game rules

[← Back to README](../README.md)

## How the game works

1. A user creates a **friend group** for a competition season (for example LaLiga 2026/27) and shares its invite code.
2. Members **predict the score** of every match. Each match closes at its own kick-off time.
3. When a match finishes, predictions are **scored automatically**.
4. Each group has a **season standing** and a **standing per round**.

A group lives as long as its season: when the season ends, the group becomes read-only (standings remain visible)
or can be deleted by its owner. A new group is created for the next season.

## Rules

| Rule | Behaviour |
|---|---|
| Scoring | 3 points for the exact score, 1 for the correct outcome (home win / draw / away win), 0 otherwise |
| Deadline | Each match closes at its own kick-off time |
| One prediction per match | It counts in every group of that competition |
| Missing prediction | 0 points |
| Tie-break | More exact scores first; if still tied, players share the position |
| Other members' predictions | Revealed only once the match has started; before that, only the number of predictions is shown |
| Joining mid-season | Predictions are per user, not per group: everything predicted earlier in the season counts. Someone who never predicted before starts with 0 points |

## Groups

| Rule | Behaviour |
|---|---|
| Members | Up to 50 per group |
| Groups per user | Up to 10 active groups owned by the same user |
| Joining | With the 8-character invite code (no ambiguous characters such as `0`/`O` or `1`/`I`); joining twice is harmless |
| Leaving | Members can leave; their predictions are kept and reappear if they rejoin |
| Owner | Cannot leave the group, but can delete it. Only the owner can delete it |
| Visibility | Non-members get `404` for a group, its standings and predictions |
| Finished season | Nobody can join, leave or create groups for it; standings remain visible |

## Special cases

| Case | Behaviour |
|---|---|
| **Postponed match** | Keeps its predictions. When it gets a new date it reopens for predictions and scores in the round **in progress when it is played**, so closed rounds never change |
| **Kick-off time confirmed or changed** | If the round has not started, the round start date is updated |
| **Brought-forward match** | Played weeks before its round; it does not change when its round starts, so it cannot pull other matches into that round |
| **Suspended match** (stopped after kick-off) | Predictions stay closed, even if it is rescheduled. It scores when completed |
| **Cancelled match** | Scores nothing; points already awarded are removed |
| **Corrected result** | Points are recalculated from scratch, never added on top |
| **Awarded result** (administrative decision) | Counts with the official awarded score |
| **Season end** | The season only finishes when every match is finished or cancelled, so pending postponed matches always score |
