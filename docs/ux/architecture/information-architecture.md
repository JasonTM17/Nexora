# Information architecture

All product nodes are **Planned**. The organization switcher changes visible
context only; every protected operation remains subject to server-derived
membership and permission checks.

## Route families and landmarks

```text
Public
├── Published page
├── Not found
└── Access-limited member entry

Authentication
├── Sign in
├── Authentication callback / recovery
└── Organization selection

Studio (authenticated, permission-aware)
├── Overview
├── Content
│   ├── Pages
│   ├── Page editor / builder
│   ├── Preview
│   └── Review and history
├── Experience
│   └── Themes
├── Knowledge
│   ├── Knowledge bases
│   ├── Documents and processing state
│   ├── Member chat
│   └── Retrieval quality (authorized inspection)
├── Organization
│   ├── Profile
│   ├── Members
│   ├── Roles and permissions
│   └── Tenant settings
└── Help / command entry
```

The final visual direction may reorganize controls, but it must preserve this
meaningful hierarchy and the task destinations listed in the route inventory.

## Navigation contract

| Context | Primary navigation | Semantic requirements |
| --- | --- | --- |
| Public | Brand/home, page-local links, member entry | `header`, one `nav` with a descriptive label, `main`, `footer`; content hierarchy is not hidden behind icon-only controls. |
| Authentication | Back/continue and recovery support | `main` contains one clear task; return destination is announced and safe. No Studio navigation is exposed before a session. |
| Studio desktop | Persistent primary navigation plus contextual tabs | A “Skip to content” link is first in keyboard order; selected route uses `aria-current="page"`; collapsed labels still have accessible names. |
| Studio 375px | Menu button opens a labelled modal/drawer navigation | Focus moves into the drawer, remains trapped while open, returns to the trigger on close and announces the current section. |
| Builder | Page hierarchy breadcrumb, outline and contextual command menu | Breadcrumbs express location (`Content / Pages / {page} / Builder`), never browser history or authorization. Canvas commands have a keyboard route. |
| Themes | Experience section, theme editor and preview context | Preview is named as non-published. Unsafe token values remain validation failures, not CSS that reaches a browser. |
| Knowledge/chat | Knowledge scope switcher, conversation/history list and source panel | The scope is named before the composer; source panel opening/closing has a clear return focus target. |

## Access and disclosure rules

- Hidden navigation is not authorization. A manually entered protected route
  receives the same planned server-side check as a visible link.
- When a route is denied, use a neutral title such as “You do not have access”;
  do not reveal the page title, tenant name, document name, role assignment or
  whether the hidden resource exists.
- Contextual help is permission-aware. It may describe an available task but
  must not enumerate actions or object names outside the current authority.
- A route with a changed/removed membership clears stale visible tenant data
  before it offers another selection. Do not retain prior content during a
  switch merely to make the interface look fast.

## Responsive content priority

| Surface | 375px behavior | Desktop behavior |
| --- | --- | --- |
| Public page | Single content column; navigation drawer; no horizontal scroll | Content-led layout with adaptable reading measure. |
| Lists (pages, members, documents) | Essential identity/status/action first; filters in a disclosure or drawer; table becomes labelled cards where needed | Dense sortable/filterable table or list with persistent controls. |
| Forms/profile/settings | One column, full-width controls and sticky-safe primary action | Grouped fields; summary and contextual help may sit alongside. |
| Review/history | Timeline before secondary metadata; action bar remains reachable without overlaying content | Candidate, history and decision context can be side-by-side. |
| Builder | Outline, inspector and preview; precision canvas is explicitly desktop-oriented | Library, outline/canvas and inspector coordinate without requiring horizontal viewport scrolling. |
| Chat | Composer, answer and citation controls remain in reading order; source panel is a modal/drawer | Persistent source/context rail is allowed. |

## Keyboard and announcement baseline

Each planned route provides these shared behaviors before product-specific
commands are added:

1. Focus order: skip link → header/context switcher → primary navigation →
   breadcrumb/context → page heading → route actions → route content.
2. One `h1` identifies the route; tabs use proper tab semantics only where the
   content changes in place. A navigation link remains a link.
3. Route changes, save-state changes and content refreshes use restrained
   `aria-live` announcements. They do not announce every streaming token or
   autosave timer.
4. Dialogs name their purpose, describe irreversible effects, offer an explicit
   cancel control and restore focus to the initiating control. Destructive
   actions never depend on color alone.
5. Respect `prefers-reduced-motion`; motion is not the only indicator of saving,
   processing, selection, status or new content.
