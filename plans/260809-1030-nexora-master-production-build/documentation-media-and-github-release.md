# Nexora Documentation, Media and GitHub Distribution Contract

## Binding Requirement

The user requires the completed Nexora repository to be presentation-ready as well as technically reproducible. This contract is accepted through `DEC-022`. It covers repository documentation, real visual evidence, GitHub presentation, Releases, Packages/GHCR and container distribution. It does not authorize publication before the release and remote-write gates pass.

## Truth and Provenance Rule

Every screenshot, GIF, diagram, benchmark, badge, package and release claim must identify the exact source SHA and evidence class that produced it. Generated concepts may guide design, but only captures from a running reviewed build may be labeled as the product. Fixture data must be visibly labeled; secrets, private paths, personal data and unsupported live metrics are forbidden.

## Repository Documentation Architecture

| Artifact | Required content | Evidence gate |
|---|---|---|
| `README.md` | Product value, verified capabilities, real hero capture, quick start, architecture summary, status, links and honest limitations | Commands rerun from clean checkout; capability links resolve to receipts |
| `docs/architecture/**` | Context, container, component, deployment, trust/tenant, publish/outbox and secure-RAG diagrams | Diagram source and rendered SVG/PNG correspond to accepted architecture |
| `docs/product/**` | Core journeys, UI/state inventory, accessibility notes, screenshot gallery and walkthrough | Captured at named route/state/viewport on exact SHA |
| `docs/development/**` | Prerequisites, setup, profiles, testing, migrations, contracts and contribution flow | A new-engineer rehearsal succeeds |
| `docs/operations/**` | Deploy, observe, alert, incident, rollback, backup, restore, scaling and provider-outage runbooks | Staging drill receipts and last-tested dates |
| `docs/security/**` | Threat model, auth/RBAC/tenant/RLS, upload/RAG/provider policy, retention and residual risks | Security review links to tests and findings |
| `docs/evidence/**` | Release index, checks, benchmarks, RAG evaluations, restore drills and media manifest | Digests, timestamps, environment and limitations recorded |

Documentation uses one canonical contract per subject. Generated OpenAPI, schema and diagram artifacts are linked or reproduced deterministically; prose copies may not drift into a second source of truth.

## Required Architecture Visuals

1. C4 system context: users, Nexora, Vercel, backend, Supabase, DeepSeek and operations boundary.
2. Container/service view: Next.js web, Spring domain API, Go ingress, NATS JetStream, PostgreSQL/pgvector, Storage, Realtime and embedding service.
3. Tenant trust boundary: browser, Supabase JWT, Spring authorization, database roles/RLS and object/vector isolation.
4. Publishing sequence: draft, review, immutable version, transaction/outbox, invalidation, Realtime notification and public refetch.
5. Secure-RAG sequence: upload, extract, chunk, embed, tenant/permission filter before context, generation and citation resolution.
6. Deployment and recovery: preview/staging/production promotion, immutable image digests, rollback and data restore dependencies.

Mermaid or diagram-as-code sources remain reviewable in Git. Final docs also contain rendered SVG for sharp browser display and PNG fallbacks/social use. Diagram source, renderer version and output digest are recorded.

## Screenshot and GIF Capture Contract

### Minimum final capture set

- Desktop and 375px public experience.
- Organization switch and permission-denied state.
- Page list, builder, preview, review, publish and rollback.
- Theme editor and approved adaptive page behavior.
- Knowledge upload/progress/failure recovery.
- Secure chat with resolvable citations and a no-answer/denied path.
- Loading, empty, error, offline/reconnect and degraded states where material.
- Operations view only when real telemetry is connected; otherwise use an explicitly labeled deterministic demo.

### Walkthrough

Create a concise GIF showing one continuous verified story: sign in -> edit/build -> review/publish -> public update -> upload knowledge -> ask cited question. A higher-quality MP4 may accompany it, but the requested GIF remains in the repository documentation. Provide alt text, a text transcript and a reduced-motion/static alternative.

### Capture pipeline

1. Freeze `product_sha`, release/main ancestry, web deployment ID, API/consumer image digests, migration version, event/schema revisions, seed/fixture/corpus checksums and provider mode/model.
2. Start the documented capture environment and scenario ID; record browser/OS/tool versions, redaction/fault-injection profile and run pre-capture E2E smoke checks.
3. Capture named routes/states at specified viewports with sensitive values redacted at source.
4. Optimize media without making behavior appear faster or more capable than observed.
5. Generate `docs/media-manifest.json` containing schema version, path, media SHA-256, raw-recording digest, edit/optimization recipe, `product_sha`, web deployment ID, API/container digest, migration version, event/schema revisions, seed/fixture/corpus checksum, provider/model or deterministic-provider label, capture environment/scenario, route, state, viewport, data class, command/tool/browser versions and date. The tracked manifest never attempts to contain its own `evidence_sha` or a future `release_sha`.
6. Review accessibility, truthfulness, cropping, filesize and broken links.
7. Commit and integrate docs/media. The final R00I-B Controller/CI receipt outside the candidate tree records the resulting `evidence_sha`, manifest digest and proof that `product_sha..evidence_sha` changes only approved docs/media paths; rerun build/smoke at `evidence_sha`.
8. R01 creates the later release-metadata-only candidate. Prove `evidence_sha..release_sha` is limited to approved release metadata, then map final `release_sha -> evidence_sha + media-manifest digest` in the external signed tag/GitHub Release/Controller receipt. No tracked file is required to contain its own final commit SHA.
9. Re-capture if any product path, API/schema/migration, fixture/corpus/provider mode or observable behavior differs; a release-metadata-only delta may reuse the proven `product_sha` capture through the explicit external SHA mapping.

## GitHub Repository Presentation

The release manager configures and verifies:

- About description, production URL when live, and focused topics such as `headless-cms`, `rag`, `nextjs`, `spring-boot`, `supabase`, `pgvector`, `nats`, `go` and `multi-tenant`.
- A real-product social preview derived from the approved visual system, with no fake metric or credential.
- License, security policy, contributing guide, code of conduct if public collaboration is enabled, issue/PR templates and CODEOWNERS.
- Branch rules/rulesets, required checks, secret scanning/dependency policy, Discussions only if an owner/moderation path exists.
- Repository homepage, README badges and package links validated after publication.

GitHub About and settings are external state. Their values and verification screenshots/CLI output are recorded in the release receipt; checked-in docs alone do not prove configuration.

## Versioning and Release Train

| Stage | Recommended identity | Required distribution |
|---|---|---|
| M4 developer preview | `v0.1.0-alpha.1` prerelease | Exact-SHA notes, initial architecture/quick start, truthful M0-M4 captures/GIF and known limitations |
| M7 release candidate | `v1.0.0-rc.1` prerelease | GHCR images, SBOM/provenance/attestations, deploy/rollback/restore evidence and checksums |
| M8 stable | `v1.0.0` | Final docs/media, immutable release record, production evidence index and residual-risk statement |

Release notes list user-visible changes, migrations, compatibility, security fixes, upgrade/rollback instructions, contributors, artifact digests and known limitations. Draft first; publish only from accepted `main`. Mutable `latest` is assigned only to a stable release and is never used for deployment pinning.

### Artifact identity matrix

| Runtime/distribution | Canonical identity | Must not be conflated with |
|---|---|---|
| GitHub source release | Signed/annotated tag and accepted remote `main` SHA | Local branch or unpushed commit |
| Vercel web | Team/project plus staged Production deployment ID, source SHA and configuration fingerprint | Portable `nexora-web` OCI image |
| Spring API | GHCR repository plus immutable OCI digest and attestation subject | SemVer tag alone |
| Go ingress/consumer | GHCR repository plus immutable OCI digest and attestation subject | Another service digest or `latest` |
| Database | Flyway schema/migration version and restore watermark | Application tag alone |
| Media | Tracked manifest pins `product_sha`, deployment/digest/schema/fixture/provider tuple and media digest; R00I-B receipt adds `evidence_sha`; external release attestation maps final `release_sha` to `evidence_sha` plus manifest digest | Screenshot filename/date alone or a tracked file claiming its own final SHA |

Artifact attestations establish origin/provenance and must be verified against the OCI digest. They do not, by themselves, prove security, correctness or production health; scan/test/deployment evidence remains separate.

## Packages, Images and Supply Chain

Publish service images to GHCR from GitHub Actions with the repository-scoped `GITHUB_TOKEN`, least-privilege permissions and OCI source/description/license labels. Candidate image set:

- `ghcr.io/jasontm17/nexora-api` for Spring.
- `ghcr.io/jasontm17/nexora-ingest` for Go when the M3 boundary is retained.
- `ghcr.io/jasontm17/nexora-web` as the portable/local/Kubernetes image even when the primary web target is Vercel.

Each image is multi-stage, non-root, health-aware and tagged with SemVer plus `sha-<shortsha>`. Release and deployment records pin immutable SHA-256 digests. Buildx publishes tested architectures only, produces SPDX SBOM plus provenance, runs vulnerability/license/secret checks and creates a verifiable GitHub artifact attestation where the repository plan supports it. Build arguments never carry secrets.

## Team Ownership and Supervision

| Work | Writer | Required reviewers |
|---|---|---|
| README/docs navigation and claim index | Docs/release owner | Domain owner + Advisor + Kongming |
| Architecture sources/renders | Architecture owner | Security/data owner + Advisor + Kongming |
| Product captures/GIF | UI evidence owner | Accessibility reviewer + Advisor + Kongming |
| GHCR/workflows/attestations | Supply-chain owner | Security reviewer + Advisor + Kongming |
| GitHub settings/About/Release | Release manager | Controller + Advisor + Kongming; user approval for first public mutation |

Advisor checks clarity, UX fidelity, architectural coherence and operator usefulness. Kongming independently attacks unsupported claims, stale media, secret leakage, mutable artifacts, unsafe workflow permissions and SHA/digest mismatches. Neither reviewer accepts its own authored artifact.

### M4 evidence branch train

| Order | Task/branch | Sole writer boundary | Dependency and merge gate |
|---:|---|---|---|
| 1A | R00A `docs/m4-alpha-architecture` | `docs/architecture/**` | Starts from accepted `product_sha`; exact-head review yields `MERGE_READY` |
| 1B | R00B `docs/m4-alpha-media` | `docs/product/media/**`, `docs/product/walkthrough.*`, `docs/media-manifest.json` | May run beside R00A as writer two; captures the same product head; exact-head accessibility/truth/provenance review yields `MERGE_READY` |
| 2A | R00I-A `integration/v0.1-m4-evidence` | No semantic writer; Git Manager mechanically integrates A/B `MERGE_READY` heads | Sequential merge and bounded link/provenance/path checks produce one exact `INTEGRATED` evidence-base head |
| 2B | R00C `docs/m4-alpha-docs` | `README.md`, `docs/product/index.md`, `docs/evidence/v0.1.0-alpha.1/**` | Branches from R00I-A so A/B artifacts exist; exact-head review yields `MERGE_READY` |
| 3 | R00I-B `integration/v0.1-m4-evidence` | No semantic writer; Git Manager mechanically integrates C `MERGE_READY` head | Combined links/build/smoke, secret scan and docs-only `product_sha..evidence_sha` proof; changed conflict output is re-reviewed before main acceptance |
| 4 | R01 `release/v0.1.0-alpha.1` | Release notes and approved release metadata only | Branches from accepted R00I-B/main evidence head; one Release Manager writer; final same-candidate dual review and user R3 authority |

This train mirrors the subject ownership table instead of asking one broad docs/media agent to cross architecture, UI evidence and release claims. Maximum two writers remains enforced, branch/path intersections are empty, and the release branch is never shared by concurrent writers. `MERGE_READY`, `INTEGRATED` and `ACCEPTED` retain their canonical meanings throughout.

## Milestone Placement

- M1 establishes truthful README/docs skeleton, policies and capture tooling contract.
- M4 produces the first real product captures, GIF, M0-M4 architecture and required prerelease after publication authority; without that R3 authority the Goal remains `NEEDS_USER`.
- M6 supplies complete security/observability/performance evidence.
- M7 publishes verified packages/images and exercises deployment/rollback/restore.
- M8 reconciles every claim and publishes the stable final documentation corpus.

## Stage-Specific Acceptance

### M4 — `v0.1.0-alpha.1` source/media prerelease

- [ ] M0-M4 architecture sources/renders, real product screenshots and requested GIF pin `product_sha` plus the expanded capture tuple in the tracked manifest; R00I-B externally records `evidence_sha`, and the final tag/Release receipt maps `release_sha -> evidence_sha + manifest digest` without self-reference.
- [ ] Alt text, transcript and reduced-motion/static alternatives exist; fixtures/provider mode and M6/M7 limitations are visible.
- [ ] Clean-checkout local setup and documented M0-M4 critical flow succeed at the release head.
- [ ] Accepted remote `main`, prerelease tag/notes/assets and live GitHub About state reconcile after explicit publication approval.
- [ ] Advisor reviews UX/architecture/media truth; Kongming passes the alpha claim/secret/provenance review. GHCR/deployed-digest evidence is explicitly not an M4 blocker.

### M7 — release candidate distribution and continuity

- [ ] Repository-linked Spring/Go/web-portability GHCR packages pull by immutable digest and expose correct OCI metadata.
- [ ] Source/tag SHA, Vercel deployment identity, API/Go OCI digests, migration version, SBOM/provenance/attestation subjects and deployed digests reconcile in the artifact matrix.
- [ ] Vulnerability/license/secret gates, exact deployment checks, rollback eligibility and isolated DB/object/JetStream restore receipts pass.
- [ ] Advisor reviews operability; Kongming passes supply-chain, continuity and release-authority review.

### M8 — stable `v1.0.0`

- [ ] Final architecture/media corpus matches the stable system and all external GitHub About/topics/social preview/Release/Package links are live-observed.
- [ ] A new engineer reproduces setup, critical flow, pull/run, deployment and recovery guides within documented limitations.
- [ ] Final Advisor and Kongming exact-candidate verdicts, Staff review and residual-risk statement are published in the evidence index.

## Stop Conditions

Invented live data, stale or concept-only media labeled production, secret/private path in an artifact, release built from an unaccepted head, mutable image deployed, missing digest/attestation verification, broken quick start, or GitHub state claimed from files without live observation.
