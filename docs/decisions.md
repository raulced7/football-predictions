# Technical decisions

[← Back to README](../README.md)

## Design

**Organization by feature.** Each business area (`group`, `prediction`, `standings`…) has its own package with all its
layers, instead of global `controller` / `service` / `repository` packages.

**Domain events for scoring.** Finishing or cancelling a match publishes `FixtureFinishedEvent` or
`FixtureCancelledEvent`, handled synchronously in the same transaction, so results and points are always consistent.
*Trade-off:* an `AFTER_COMMIT` async listener would respond faster, but a scoring failure could leave a result without points.

**State transitions in the entity.** `postpone()`, `reschedule()`, `suspend()`, `cancel()` and `finish()` validate
the current state. Admin endpoints and the sync share exactly the same rules.

**Adapter for the external API.** `FootballDataProvider` hides football-data.org: the rest of the application works
with its own model and statuses. Changing provider (or moving to a paid plan) only requires a new implementation.

## Dates and rounds

**Scoring round vs. official round.** A postponed match scores in the round in progress when it is played, so the
standings of closed rounds never change weeks later.

**Robust round start dates (`starts_at`).** A round starts at its earliest "regular" match, ignoring postponed,
cancelled and **brought-forward** matches (more than 4 days before the round's median kick-off, computed with
`percentile_disc`). It is recomputed while the round has not started, to follow provisional times being confirmed,
and frozen once it starts. The scoring-round rule only looks at these dates.

**Injectable `Clock`.** Every time-dependent rule uses an injected `Clock`, so deadlines and the sync window are
testable with a fixed time.

## Data and persistence

**Atomic upsert for predictions.** `INSERT … ON CONFLICT (user_id, fixture_id) DO UPDATE` makes create-or-update a
single atomic statement. `PUT` is idempotent by design.

**Standings in SQL.** Points, exact hits and positions are computed in one query using `SUM`,
`COUNT(…) FILTER (WHERE …)` and `RANK() OVER (…)`, starting from group members so players without predictions still
appear with 0 points.

**Avoiding N+1 queries.** `@EntityGraph` loads rounds together with fixtures, and "my round" view uses two queries
(fixtures + the user's predictions) joined in memory instead of one query per match.

**DTO projections.** Member lists and counts are read with `SELECT new …` projections instead of loading full entities.

**Database constraints as the last line of defence.** Unique email (case-insensitive index on `lower(email)`), one
prediction per user and match, non-negative goals and "a finished match has a result" are enforced by PostgreSQL.

**Partial transitions in one transaction.** The sync applies changes through entity methods, not transactional
services: an exception crossing a `@Transactional` boundary marks the whole transaction as rollback-only, even if
it is caught later. This way a failing match is skipped without aborting the sync.

## Security

**Stateless JWT with Spring Security OAuth2 Resource Server.** Tokens are signed with HS256 and validated for
signature, expiration and issuer. The user id is always taken from the token, never from the request.

**Roles in the JWT.** Avoids a database lookup per request. *Trade-off:* a revoked role stays valid until the token
expires (2 h); short-lived access tokens with refresh tokens would reduce that window.

**Same error for unknown email and wrong password,** so registered emails cannot be discovered.

**`404` instead of `403` for groups.** Non-members cannot find out which group ids exist.

**Hidden predictions never leave the server.** Before kick-off only the number of predictions is returned.

## Concurrency

**Pessimistic lock when joining groups.** `SELECT … FOR UPDATE` on the group row serializes joins, so two
simultaneous joins cannot exceed the member limit.

**No overlapping syncs.** A `ReentrantLock` with `tryLock()` prevents the daily, live and manual syncs from running
at the same time. *Limitation:* it only works within one instance; several instances would need a distributed lock
such as ShedLock.

## Synchronization

**HTTP outside the transaction.** Data is downloaded first and written by a separate `@Transactional` component, so a
slow provider never holds a database connection.

**Polling only when needed.** The live job checks the local database first and calls the API only while
provider-linked matches are in the play window.

## Edge cases found while testing

- **Non-chronological test data** made a later round look "started" and captured rescheduled matches. This revealed
  the same risk with real brought-forward matches and led to the `starts_at` design.
- **Manually created fixtures** (no `external_id`) kept the live sync polling the provider for 24 hours; the play
  window now only considers provider-linked fixtures.
- **Mocks created inside another `when(...)`** caused Mockito's `UnfinishedStubbing`; mocks are always created before stubbing.

## Possible improvements

- Distributed lock for scheduled jobs (ShedLock) when running several instances
- Refresh tokens and token revocation
- Joker multiplier (double points for one match per round)
- More competitions, including knockout rules (90-minute result)
- Notifications before each round closes
- React PWA client and deployment to AWS
