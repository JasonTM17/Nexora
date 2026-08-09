# Acceptance and Evidence Contract

## Verdicts

| Verdict | Meaning | Allowed next action |
|---|---|---|
| PASS | Exact artifact and exact HEAD independently verified; all required gates passed | Integrate or advance dependency |
| HOLD | Useful work exists, but evidence, scope, validation, or review is incomplete | Preserve branch; repair or gather evidence |
| STOP | Security, tenant, secret, destructive, data-loss, provenance, cost, or authority invariant failed | Freeze affected scope and escalate |
| DEFER | User deliberately removes capability from the active Goal, while master roadmap retains it | Remove from current claims; record decision |

## Universal Phase Receipt

Every accepted phase records:

```yaml
phase_id: <execution and prompt phase>
milestone: <M0-M8>
goal_scope: <v0.1-M0-M4 or later Goal>
semantic_digest_algorithm: NEXORA-SEMANTIC-DIGEST-1
plan_semantic_digest: <approved digest>
plan_file_list_digest: <approved digest>
master_source_sha256: <approved digest>
parent_requirement_catalog_digest: <approved digest>
child_requirement_catalog_digest: <approved digest>
requirements: [<stable parent/child and UREQ ids>]
accepted_base_sha: <sha>
branch: <intent-based branch>
worktree: <absolute path>
owner: <agent/role>
resolved_route:
  agent: <observed runtime role>
  model: <actual model>
  reasoning_effort: <actual effort>
  tools: [<material tools>]
writer_lease_generation: <generation or null>
allowed_paths: [<paths>]
head_sha: <sha>
commits: [<sha>]
working_tree_clean: true
checks:
  - command: <redacted command>
    exit_code: 0
    environment: <local/container/live>
artifacts: [<paths/digests>]
security_evidence: [<paths>]
diff_digest: <digest of reviewed base..head patch>
review_verdict: PASS
reviewed_head_sha: <sha>
material_gate:
  required: <true|false>
  candidate_identity: <sha/digest/deployment or not-applicable>
  advisor_receipt: <FIT receipt or not-applicable>
  kongming_receipt: <PASS receipt or not-applicable>
  controller_disposition: <linked disposition or not-applicable>
merge_method: <approved method>
main_before_sha: <sha>
merge_sha: <sha>
combined_main_checks: [<evidence>]
remote_sha: <sha or not-applicable for a non-release phase>
limitations: [<truthful unverified boundaries>]
```

A worker branch may be `IMPLEMENTED` and a milestone branch may be `INTEGRATED`, but the task is `ACCEPTED/DONE` only after the approved main merge and combined-main checks. For the requested v0.1 and later public releases, remote SHA equality is mandatory after explicit R3 authorization; a non-release phase may record `not-applicable`.

Every C3/R3 or otherwise material candidate requires both Advisor and Kongming receipts for the same identity. Missing, stale, `HOLD` or `STOP` counsel blocks advancement; routine C1/C2 work may use `not-applicable` only while it remains within an already dual-approved contract.

Squash, cherry-pick, rebase, conflict resolution or generated-file drift that changes the reviewed patch invalidates the receipt. Recompute the diff digest and repeat exact-head review.

## Evidence Classes

Keep these classes separate:

1. Static/source evidence.
2. Unit/deterministic evidence.
3. Containerized integration evidence.
4. Browser evidence.
5. Live external-service evidence.
6. Load/benchmark evidence.
7. Deployment evidence.
8. Recovery evidence.
9. Distribution/media evidence.

Lower classes never silently substitute for higher ones.

## Global Feature Gate

Every feature, proportionally, addresses:

- Business behavior and explicit failure behavior.
- API input/output, stable errors, pagination/idempotency where relevant.
- Authentication, authorization, tenant isolation, rate/abuse limits.
- Database key, tenant key, FK, constraints, indexes, timestamps, deletion, audit, RLS review.
- UI loading, empty, error, denied, conflict, reconnect, mobile, keyboard, reduced-motion and destructive-confirmation states.
- Logs, metrics, traces, health and safe metadata.
- Unit, integration, security and E2E tests appropriate to risk.
- Docs and exact limitations.

## Critical STOP Conditions

- Any secret-shaped value enters tracked content, staged diff, Git history, report, screenshot, or log.
- Worker pushes/merges `main`, force-pushes shared history, or accepts its own work.
- Concurrent writers overlap migrations, lockfiles, shared contracts, generated outputs, or root configuration.
- Reviewer evidence targets a different HEAD from the proposed merge.
- Unaccounted dirty changes remain at handoff.
- Mandatory build/test/lint/typecheck/migration/security command fails or did not run.
- Any cross-tenant page, document, object, realtime event, vector, chat, audit, analytics, flag, or admin access succeeds.
- Unauthorized document content enters retrieval candidates or LLM context.
- Citation cannot resolve to authorized source/chunk evidence.
- Destructive or paid external action lacks explicit approval, ceiling, and recovery plan.
- Deployment has no tested rollback or incompatible migration path.
- Same failure repeats twice without a changed hypothesis or approach.
- Screenshot/GIF/release/package/production claim lacks exact source/artifact identity or presents a concept/fixture as observed live behavior.

## UI Evidence

- Browser screenshots at 375px and agreed desktop width; record dimensions and route/state.
- No horizontal overflow.
- Keyboard-complete critical flows, including non-pointer alternative to drag/drop.
- Visible focus, correct focus restoration, labels, dialog semantics, error association.
- Reduced motion and contrast checks.
- Automated accessibility scan with zero serious/critical findings on critical routes.
- Manual keyboard and screen-reader smoke test.
- Sanitization/CSP test for rich content and citations.
- Per-surface CSP/cache evidence: production build, exact headers, browser violation log, hydration console, first-screen FOUC capture and public cache hit/miss/control behavior; dynamic Studio/auth and cacheable public pages are evaluated separately.
- Stitch quarantine receipt inventories scripts/import maps/event handlers/URLs/fonts/images/dependency instructions, proves offline/no-network inspection and shows that no generated code or unapproved remote asset entered production.
- Build bundle/performance budget from actual output.

## RAG Evidence

- Versioned evaluation corpus and checksum.
- Provider/model/embedding identifiers and parameters.
- Deterministic CI provider results separated from live provider smoke.
- Retrieval metrics such as Recall@K and citation precision with predeclared thresholds.
- Permission-filter and leakage fixtures at retrieval and context stages.
- No-answer/low-confidence behavior.
- Prompt-injection and XSS rendering fixtures.
- Source deletion/tombstone removes future retrieval eligibility.

## Performance Evidence

Every benchmark records commit SHA, hardware, services, versions, dataset, warmup, duration, concurrency, raw output, baseline, changed variable, result, and limitations. Unsupported round numbers are forbidden.

## Deployment and Recovery Evidence

- Artifact/image digest tied to commit, GHCR identity, SBOM/scan/provenance and attestation verification.
- Rendered manifests and environment configuration without secrets.
- Migration compatibility and rollback trigger/command.
- Vercel preview/staging/production identity, promotion/rollback and post-deploy observation trace.
- Backup scope, RPO, RTO.
- Restore into isolated environment and application-level verification.

## Documentation, Media and GitHub Evidence

- Screenshot/GIF manifest records SHA-256, `product_sha`, route, state, viewport, data class, fixture/provider identity, migration/event/schema revision, capture tool/version and date; it never contains its own `evidence_sha` or a future `release_sha`.
- The external R00I-B control/CI receipt records `evidence_sha`, manifest digest and a verified `product_sha..evidence_sha` diff limited to approved docs/media. R01 proves `evidence_sha..release_sha` is release-metadata-only; the signed tag/GitHub Release/Controller receipt maps final `release_sha` back to `evidence_sha` plus manifest digest. Any material UI/runtime change requires recapture.
- Hosted captures also record Vercel project/deployment ID and source SHA; backend/event captures record exact API and consumer image digests where those services contribute.
- Diagram source, renderer version, rendered SVG/PNG digest and architecture review head are retained together.
- Alt text, transcript and reduced-motion/static alternative accompany the walkthrough.
- Live-observed GitHub About/topics/social preview, Release tag/assets and repository-linked Package state are stage-matched: M4 alpha proves M0-M4 media and release identity; M7 proves GHCR/deployment/rollback; M8 proves final product media and all links.
- Release tag, public remote main SHA, Vercel deployment ID/source SHA, OCI subjects and deployed backend digests reconcile. The portable web OCI subject is never claimed to be the artifact Vercel serves.
- Image pull, checksum, SBOM/provenance subject and attestation verification commands succeed, while attestation is not misrepresented as vulnerability-free or runtime-safe proof.
- README/quick start is rehearsed from a clean checkout and all badges/links resolve.

## Whole-Plan Consistency Gate

After any accepted validation or red-team change:

1. Re-read `plan.md`, all supplemental contracts, and every `phase-*.md`.
2. List decision deltas.
3. Search for stale names, rejected assumptions, superseded APIs/files/routes, duplicate contracts, and changed dependencies.
4. Reconcile every affected occurrence.
5. Record files read, deltas checked, stale references fixed, and unresolved contradictions.

Implementation remains blocked when unresolved contradictions are greater than zero.
