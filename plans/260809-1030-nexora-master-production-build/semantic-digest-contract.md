# Nexora Semantic Candidate Digest Contract

## Purpose

Advisor, Kongming, Controller and the future Goal must identify the same semantic plan candidate reproducibly. A prose phrase such as “all plan files except evidence” is insufficient because file ordering, line endings and exclusions can change the hash.

Canonical algorithm ID: `NEXORA-SEMANTIC-DIGEST-1`.

## Semantic File Set

The digest root is the absolute directory containing `plan.md`. The included set is every `*.md` file directly under that root, except `validation-log.md`.

Excluded evidence paths are:

- `validation-log.md`;
- the entire `reports/**` subtree, including candidate manifests and Advisor/Kongming receipts;
- generated local runtime/control-ledger state outside the plan root, including machine-path receipts under the Git common directory.

Phase files, Decision Log, Outcome/Goal/Release contracts, requirements catalogs, workflow/runbook, technology/UI/hosting/distribution contracts and the Vietnamese approval summary are semantic and therefore included. A new root Markdown file enters the set automatically. Nested semantic Markdown is forbidden unless this contract is revised first; validation fails rather than silently excluding it.

## Canonicalization

For every included file:

1. Resolve a plan-root-relative path and replace `\` with `/`.
2. Reject absolute paths, `..`, symlinks escaping the root, duplicate normalized paths and case-colliding paths.
3. Sort paths by ordinal UTF-8 byte order; do not use locale-aware or case-folded sorting.
4. Require valid UTF-8. Remove one UTF-8 BOM if present.
5. Normalize `CRLF` and lone `CR` to `LF`.
6. Preserve all other bytes and whitespace, but normalize the end to exactly one terminal `LF`.
7. Compute `content_sha256` and byte length over that normalized UTF-8 content.

The digest stream concatenates this binary frame for each sorted file:

```text
UTF8("NEXORA-FILE\0")
UTF8(relative_posix_path)
NUL
UTF8(decimal_normalized_content_byte_length)
NUL
normalized_content_bytes
NUL
```

The candidate digest is lowercase hexadecimal SHA-256 over the complete concatenated stream. The file-list digest is lowercase SHA-256 over `UTF8(path + "\n")` for the same ordered path list.

No timestamps, absolute root, Git SHA, validation result or review verdict enter the semantic stream. This avoids machine-specific hashes and self-reference while ensuring every semantic byte is covered.

## Evidence Manifest

The computed manifest is append-only evidence under `reports/candidate-manifests/<candidate-digest>.json` and is excluded from the semantic set. A manifest eligible for the public repository is machine-portable and public-safe. It records:

- `algorithm_id`;
- logical `plan_root_id: NEXORA_PLAN_ROOT` plus the repository-relative plan directory; never an absolute workstation or attachment path;
- UTC generation time and tool implementation/version;
- included file count, ordered relative paths, byte lengths and content hashes;
- excluded path rules and observed excluded files;
- file-list digest and semantic candidate digest;
- logical master-source ID, master-prompt SHA-256, parent requirement-catalog content hash and expanded child-catalog digest;
- Decision Log revision/hash and numeric decision counts;
- optional Git baseline SHA once it exists.

The manifest never claims that its own bytes were reviewed. Each review receipt points to the manifest digest and independently recomputes the candidate digest from semantic files. Tool diagnostics that require absolute paths are written only to the untracked Git-common-dir control/evidence area and are not copied into public manifests, docs, logs or media metadata. Historical local manifests containing private paths are ineligible for staging or publication.

## Independent Reproduction Gate

Before plan dual approval and again in C0-06:

1. Two independent implementations—proposed Node.js and PowerShell—compute the digest from a clean read-only snapshot.
2. Both must report identical included path list, per-file content hashes, file-list digest and candidate digest.
3. Advisor and Kongming each independently record `algorithm_id`, file count, file-list digest, candidate digest and manifest digest in their receipt.
4. The Controller rejects a receipt that names only a Git branch, chat turn, timestamp or unversioned “latest plan.”
5. Any included-file change invalidates both receipts. Evidence-only appends do not change the candidate but may not rewrite an existing receipt.

## Goal and Git Binding

The future Goal pins all of:

- accepted plan Git SHA after the public-safe control-plane baseline;
- `NEXORA-SEMANTIC-DIGEST-1` candidate digest and file-list digest;
- master-prompt source SHA-256;
- parent and expanded child requirement-catalog digests;
- Decision Log revision;
- canonical control-ledger genesis receipt.

The Git tree at the pinned SHA must reproduce the same semantic digest. A matching digest without the accepted Git tree, or a matching Git SHA with a different digest implementation/result, is `STOP`.

## Change Control and Stop Conditions

A change to included/excluded file rules, canonicalization, framing, hash algorithm or catalog binding creates a new algorithm ID and requires fresh dual review. Missing root semantic files, hidden nested semantics, locale-dependent ordering, platform-native line-ending hashing, a self-referential tracked manifest, mismatched independent implementations, stale receipts or manual digest transcription without reproduction are `STOP`.
