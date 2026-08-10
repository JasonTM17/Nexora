# Product journeys

All journeys below are **Planned**. “Allowed” means the planned server-side
authorization and tenant policy has allowed the action; it never means that a
route or a client-side control alone allowed it.

## Persona inventory

| Persona | Primary job | Primary planned surface | Must never happen |
| --- | --- | --- | --- |
| Organization owner | Set up a tenant, people and policy | Organization settings, members, roles | A browser-supplied organization or role grants authority. |
| Content creator | Compose, validate, preview and submit content | Pages and builder | A pointer is required to complete a canvas task. |
| Reviewer/publisher | Assess a candidate, decide, publish or roll back | Review queue, page history | Self-approval or a stale/unauthorized transition is silently accepted. |
| Knowledge manager | Manage sources and durable processing | Knowledge bases and document detail | A document/job from another tenant is disclosed. |
| End visitor/member | Read a published page; search/chat within access | Public page, member knowledge/chat | Draft/private content or an unauthorized source is exposed. |
| Platform operator | Diagnose platform health without treating it as a product dashboard | Operational tools outside product IA | Health output is represented as customer-facing uptime evidence. |

## J1 — **Planned** authenticate and select a permitted organization

**Entry:** a protected deep link or an explicit sign-in command.

1. The authentication route explains why sign-in is required and moves focus to
   the sign-in heading. It returns the user to the requested safe route after a
   successful session bootstrap.
2. If membership is established but more than one organization is allowed, the
   organization switcher lists only memberships returned by the server. It has a
   search/filter input only when the list is long enough to need one.
3. The selected organization is a presentation context. The planned server
   derives tenant authority from current membership on every domain request.
4. On success, announce the organization name once and land on the original
   permitted route or the Studio overview. Do not replay a sensitive mutation.

| Condition | Required result |
| --- | --- |
| Anonymous / expired session | Explain the need to sign in; preserve only a safe return path. |
| No organizations | Show an empty membership state with a non-privileged support/return action; do not offer tenant creation unless policy later allows it. |
| Removed membership / forbidden deep link | Show a neutral access-denied page without confirming resource existence; clear the stale context. |
| Network unavailable | Preserve typed sign-in data where safe, identify the offline condition and offer retry; do not claim authentication completed. |

## J2 — **Planned** create, compose and recover a page draft

**Entry:** Pages list → “Create page”, subject to server-confirmed create
permission.

1. The creator provides typed page metadata; slug and SEO errors are attached to
   their fields and summarized at the top with links to each invalid control.
2. The builder opens with labelled library, outline, canvas, inspector and
   preview regions. Insertion, select, move, reorder, duplicate, hide/show,
   delete and undo work from keyboard commands as well as pointer interactions.
3. Autosave declares `Unsaved`, `Saving`, `Saved`, `Failed`, `Offline`,
   `Reconnecting` or `Conflict`, with a concise live-region announcement that
   does not interrupt typing repeatedly.
4. A conflict freezes automatic overwrite. The user can inspect server/current
   revision metadata, copy unsaved work where available, reload, or create an
   intentional follow-up based on the server contract. No option overwrites
   without an explicit concurrency-safe action.
5. Preview is clearly marked **Planned preview** and cannot be presented as a
   public/browser validation result. Submit is available only when server
   validation and authority permit it.

## J3 — **Planned** review, publish and roll back

**Entry:** review queue or a page’s review/history tab.

1. The reviewer opens a candidate with version identity, author, timestamp,
   validation outcome and transition history. The candidate is visually and
   semantically distinguished from the live published version.
2. Approve/reject requires a fresh server decision. Rejection captures a reason
   when the accepted workflow contract requires one; focus returns to the reason
   field if it is missing.
3. A publisher reviews the immutable candidate and confirms the planned publish
   transaction. The confirmation names the effect but does not promise cache or
   Realtime propagation.
4. Success shows the durable version receipt and links to public resolution only
   when the server returns it. An ambiguous timeout remains “publish status
   unknown”; the UI refetches history rather than issuing a blind duplicate.
5. Rollback selects an earlier immutable version and creates a new version; it
   never relabels or mutates the prior published record.

## J4 — **Planned** add and supervise a knowledge source

**Entry:** Knowledge bases → a permitted knowledge base → “Add source”.

1. The upload form states accepted formats and server-enforced limits before a
   file is chosen. It shows file name, detected type, size and remove control in
   a list with accessible progress semantics.
2. The browser receives only a planned, server-authorized private upload action;
   object keys and privileged credentials are never displayed or accepted from
   the user.
3. After submission, document and job state comes from the durable API. Planned
   Realtime may reduce latency; a disconnected channel falls back to polling or
   refresh and says “updates may be delayed”.
4. Queued, running, completed, failed, cancelled and retry-eligible outcomes
   have distinct labels and next actions. A retry/cancel always asks the server
   for current authority/state first.

## J4a — **Planned** change a constrained theme safely

**Entry:** Experience → Themes, subject to server-confirmed theme authority.

1. The editor names the target tenant/site and separates the current published
   theme from an editable preview. Inputs expose token purpose, allowed format
   and accessible contrast feedback; they do not accept arbitrary stylesheets,
   scripts, remote assets or unsafe URLs.
2. Saving has the same explicit `Unsaved`/`Saving`/`Saved`/`Failed`/`Conflict`
   contract as other optimistic edits. A conflict never silently overwrites a
   more recent theme revision.
3. Publication/rollback uses the planned immutable/versioned publish boundary.
   The UI describes the intended public impact but does not promise a live CSS
   update until a durable receipt is returned.

## J5 — **Planned** ask a grounded question and open a source

**Entry:** member knowledge/chat route under a selected permitted organization.

1. The chat route names the knowledge scope, carries a concise safe-use note and
   starts with an empty state that offers examples without asserting data exists.
2. Send creates a visible user-message lifecycle. The assistant lifecycle is
   `queued`, `streaming`, `cancelled`, `failed`, `completed` or `no answer`; a
   partial/cancelled draft is never labelled as a completed answer.
3. Citations are rendered as source controls, not decorative footnotes. Opening
   a citation reauthorizes the current user and source access; lost access says
   “Source unavailable” without revealing content.
4. The no-answer state explains that the system could not ground a response and
   offers query refinement. It never invents a citation or hides a permission
   failure behind a confident answer.
