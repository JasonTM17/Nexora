# Wireflows

These wireflows are **Planned** interaction contracts, not implemented flows.
They make failure, authority and responsive behavior explicit for later design
and build work.

## W1 — protected deep link and tenant context

```mermaid
flowchart TD
  A["Open protected route"] --> B{"Session currently valid?"}
  B -- "No / unknown" --> C["Sign-in route\nstate: auth required"]
  C --> D{"Session established?"}
  D -- "No" --> E["Auth error or recovery\nkeep safe return path"]
  D -- "Yes" --> F["Fetch allowed memberships\nplanned server authority"]
  B -- "Yes" --> F
  F --> G{"Exactly one permitted membership?"}
  G -- "No memberships" --> H["Empty/denied membership state\nclear stale context"]
  G -- "Several" --> I["Organization chooser\nonly server-returned options"]
  G -- "One" --> J["Authorize target route"]
  I --> J
  J --> K{"Allowed now?"}
  K -- "No" --> L["Neutral access-denied\nno resource disclosure"]
  K -- "Yes" --> M["Render route with named context"]
```

At 375px, the organization control is a labelled button that opens a focus
managed dialog/drawer. On desktop it may be in the persistent Studio header.
In both forms selection alone does not change authority; planned route data is
refetched after the server evaluates current membership.

## W2 — draft builder, autosave and conflict recovery

```mermaid
flowchart LR
  A["Page list"] --> B["Create or open draft"]
  B --> C["Builder regions: library, outline, canvas, inspector"]
  C --> D["Edit via pointer or keyboard command"]
  D --> E["Unsaved"] --> F["Saving"]
  F --> G{"Planned API result"}
  G -- "Saved" --> H["Saved + restrained announcement"]
  G -- "Validation error" --> I["Field/outline error + summary"]
  G -- "Offline" --> J["Offline / reconnecting\nno false saved state"]
  G -- "Conflict" --> K["Stop overwrite; refetch revision metadata"]
  K --> L["Reload, compare/copy, or explicit follow-up"]
  C --> M["Hide/show, duplicate, delete"]
  M --> N["Undo or explicit recovery\nfocus returns to affected outline item"]
```

The desktop canvas is a precision workspace. At 375px, the same draft remains
inspectable and editable through the outline, property inspector and keyboard
commands; preview remains available, while precision layout composition is
clearly marked desktop-oriented. No horizontal canvas scroll is required to
reach save, undo or navigation.

## W3 — review, publication and rollback

```mermaid
flowchart TD
  A["Creator submits candidate"] --> B["In review queue"]
  B --> C["Reviewer opens immutable candidate + history"]
  C --> D{"Fresh planned permission\nand transition check"}
  D -- "Denied / stale" --> E["Denied or conflict state\nrefetch durable history"]
  D -- "Reject" --> F["Reason capture when required"] --> G["Rejected: editable draft path"]
  D -- "Approve" --> H["Approved candidate"]
  H --> I["Publisher confirmation"]
  I --> J{"Publish transaction result"}
  J -- "Success" --> K["Durable version receipt\npublic resolution link if returned"]
  J -- "Unknown / timeout" --> L["Status unknown; refetch history\nnever blind re-publish"]
  K --> M["Choose prior immutable version"] --> N["Confirm planned rollback"] --> O["New latest version"]
```

The review route puts candidate state, decision controls and reasons before
secondary metadata at 375px. Desktop may position candidate, history and
decision context side-by-side. In both layouts approval/publish/rollback use
textual confirmation and never rely on a color-only status or a client-side
workflow transition.

## W4 — document lifecycle and grounded chat

```mermaid
flowchart TD
  A["Select knowledge base"] --> B["Choose file + validate metadata"]
  B --> C["Planned server-authorized private upload"]
  C --> D["Durable document/job record"]
  D --> E{"Job lifecycle"}
  E -- "Queued / running" --> F["Progress from durable API\nRealtime is supplemental"]
  E -- "Failed / cancelled" --> G["Cause + permitted retry/cancel action"]
  E -- "Completed" --> H["Source available under current authority"]
  H --> I["Open chat in named knowledge scope"]
  I --> J["Send question"]
  J --> K{"Assistant lifecycle"}
  K -- "Streaming" --> L["Partial draft + cancel"]
  K -- "No answer" --> M["Honest no-answer + refine prompt"]
  K -- "Failed" --> N["Failure + retry only when safe"]
  K -- "Completed" --> O["Answer with citation controls"]
  O --> P{"Reauthorize source open"}
  P -- "Allowed" --> Q["Open sanitized authorized source"]
  P -- "Lost access" --> R["Source unavailable; reveal no content"]
```

At 375px, sources open in a labelled modal/drawer and focus returns to the
citation control when closed. On desktop, a persistent source rail is allowed.
Progress, streaming and citations remain secondary to durable authorization:
the UI never exposes raw prompts, provider credentials, hidden reasoning or
unauthorized source metadata.
