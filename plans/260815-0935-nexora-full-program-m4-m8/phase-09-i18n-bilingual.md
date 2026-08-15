---
phase: 9
title: "Bilingual i18n (VI/EN) + Comprehensive Permissions"
status: pending
priority: P1
effort: "5-7 days"
dependencies: [2]
---

# Phase 9: Bilingual i18n + Comprehensive Permissions

## Overview
Make the Nexora web surface fully bilingual (Vietnamese + English) and implement
comprehensive permission-based access control on all routes and UI elements.

## Requirements
- [ ] i18n infrastructure (next-intl or custom)
- [ ] Vietnamese + English translations for all UI text
- [ ] Language switcher in navbar
- [ ] Language preference persisted (cookie + profile)
- [ ] All BFF routes enforce permission checks
- [ ] All UI elements respect permissions (show/hide/disable)
- [ ] Permission matrix displayed in admin
- [ ] Role-based access control (RBAC) enforcement

## Implementation Steps

### 1. i18n Infrastructure
- Install `next-intl` or create custom i18n with React Context
- Create `apps/web/messages/en.json` and `apps/web/messages/vi.json`
- Create `apps/web/lib/i18n.ts` with translation helpers
- Add middleware for locale detection (cookie → browser → default)

### 2. Translation Files
- Extract all hardcoded English text from components
- Create comprehensive translation keys:
  - Common (buttons, states, errors)
  - Navigation (menu items, breadcrumbs)
  - Surfaces (home, studio, AI, builder, knowledge, search)
  - Admin (flags, analytics, notifications, experiments)
  - Forms (labels, placeholders, validation)
- Professional Vietnamese translations

### 3. Language Switcher
- Add toggle in navbar (VI | EN)
- Persist preference in cookie + user profile
- Auto-detect from browser `Accept-Language`

### 4. Permission System Enhancement
- **Backend**: Permission middleware on all BFF routes
- **Frontend**: `usePermission()` hook + `<PermissionGate>` component
- **Matrix**: Display current user's permissions in admin

### 5. Permission Keys
| Key | Description | Roles |
|---|---|---|
| `organization.read` | View organization | All members |
| `organization.manage` | Edit org settings | OWNER, ADMIN |
| `members.read` | View members | All members |
| `members.manage` | Invite/remove members | OWNER, ADMIN |
| `pages.read` | View pages | All members |
| `pages.create` | Create pages | OWNER, ADMIN, EDITOR |
| `pages.publish` | Publish pages | OWNER, ADMIN |
| `knowledge.read` | View knowledge | All members |
| `knowledge.manage` | Manage knowledge bases | OWNER, ADMIN, EDITOR |
| `rag.query` | Ask AI questions | All members |
| `analytics.read` | View analytics | OWNER, ADMIN |
| `flags.read` | View feature flags | OWNER, ADMIN |
| `flags.manage` | Manage feature flags | OWNER, ADMIN |
| `experiments.read` | View experiments | OWNER, ADMIN |
| `experiments.manage` | Manage experiments | OWNER, ADMIN |
| `notifications.read` | View notifications | All members |

## Todo
- [ ] i18n infrastructure
- [ ] Translation files (EN + VI)
- [ ] Language switcher
- [ ] Permission middleware
- [ ] Permission hooks + components
- [ ] Translate all surfaces
- [ ] Permission matrix admin page

## Success Criteria
- All UI text displayed in VI or EN based on preference
- Language switcher works and persists
- All BFF routes enforce permissions
- UI elements hidden/disabled based on permissions
- Permission matrix visible in admin

## Commit Plan
```
feat(i18n): add bilingual infrastructure with VI/EN translations
feat(permissions): add comprehensive RBAC enforcement on all routes
feat(web): translate all surfaces to Vietnamese + English
feat(admin): add permission matrix display
```
