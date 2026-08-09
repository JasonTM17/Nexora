# Gói quyết định chốt trước Goal — Nexora

## Trạng thái

Tài liệu này đã được người dùng chấp thuận đầy đủ vào 2026-08-09, gồm `DEC-011` và `DEC-016`. C0-01D ghi nhận ratification vào Decision Log; hiệu lực dispatch vẫn cần exact-head Advisor/Kongming review, Controller disposition và mechanical integration. Đây chưa phải formal Goal.

## Đã chốt

| ID | Quyết định đã chốt |
|---|---|
| DEC-001 | Phương án A: Goal đầu M0-M4/Prompt 0-21, các Goal sau giữ M5-M8 |
| DEC-012 | Stitch tạo 3 hướng có bằng chứng; người dùng chọn; production UI được hand-build |
| DEC-015 | Apache-2.0 cho repository/public release, third-party provenance tách riêng |
| DEC-022 | Ảnh/GIF/sơ đồ thật, GitHub About/Releases, GHCR, SBOM/provenance bắt buộc |
| DEC-025 | Ant Design 6.x cho Studio; Ant Design X đánh giá qua adapter; public dùng Tailwind/custom; không có hai design system đầy đủ |

## Bộ mặc định đã được người dùng chấp thuận cho Goal M0-M4

| ID | Mặc định khuyến nghị | Tác động/rào chắn |
|---|---|---|
| DEC-002 | Repository duy nhất là `D:\Nexora`; `engineer/`, `.worktrees/`, env/secrets và runtime ledger không track | Không lẫn AgentKit/local state vào public repo |
| DEC-003 | Protected `main` -> `integration/<milestone>` -> một branch/worktree/lease cho mỗi writer task | PM đánh `MERGE_READY`; Git Manager merge cơ học; Controller chỉ `ACCEPTED` sau combined-main |
| DEC-004 | PM được cho phép integration nội bộ theo contract; merge milestone/main cần combined PASS + dual receipt; first push/release/deploy vẫn xin quyền R3 cụ thể | Chấp thuận policy không đồng nghĩa tự động push ngay |
| DEC-006 | Supabase Auth phát identity; Spring xác thực JWT và sở hữu domain authorization | Không tự xây auth server, không tin UI/session payload để phân quyền |
| DEC-007 | Browser domain data đi qua same-origin Next.js BFF -> Spring; direct Supabase chỉ Auth, signed private Storage, private Realtime | Không domain PostgREST/browser-to-Spring tùy tiện |
| DEC-008 | Shared schema/database với tenant keys + composite constraints + Spring checks + forced RLS defense-in-depth | Bất kỳ cross-tenant success là STOP |
| DEC-009 | Chỉ context tối thiểu đã authorize đi tới AI; raw prompt/source không log mặc định; CI có deterministic provider | Live và fixture evidence tách riêng |
| DEC-010 | DeepSeek adapter env-only, runtime verify base URL/model; key đã dán phải rotate | Không bao giờ ghi key vào plan/Git/chat receipt |
| DEC-011 | DeepSeek tối đa USD 5/25 calls, concurrency 1, một bounded retry + kill switch; Stitch so sánh đúng 3 hướng trên cùng 4 anchor, mỗi anchor gồm 1 lượt tạo ban đầu + tối đa 2 lượt edit, tức tối đa 36 generation/edit operations tổng. Bốn phần inventory còn lại của hướng được chọn mặc định được design/hand-build từ hệ token đã duyệt; muốn gọi Stitch thêm phải có amendment số lượng riêng được chấp nhận. Cloud mới USD 0 trong M0-M4 nếu chưa có R3 riêng | R3 boundary: không mua credit/upgrade, không provision paid project, không gọi provider bằng credential và không phát sinh recurring spend; ngoại lệ phải ghi provider/project/region, hard cap, TTL và owner teardown |
| DEC-013 | 5 block đầu: Hero, RichText, FeatureGrid, CTA, FAQ | Có schema/version/a11y/visibility, không raw executable content |
| DEC-014 | PostgreSQL durable jobs/outbox trước; Go/NATS chỉ giữ khi real consumer + benchmark/failure evidence chứng minh | Không microservice vì “trông production” |
| DEC-016 | Chấp thuận data-lifecycle matrix và các v0.1 defaults trong `data-lifecycle-and-privacy-contract.md` | Execution boundary: account/tenant delete, export, chat/document purge, anonymization và backup resurrection vẫn cần implementation/evidence; không phải compliance claim |
| DEC-018 | Sau duyệt plan, bootstrap public-safe local Git/control-plane; Goal pin Git SHA + semantic/source/catalog digests | Giải quyết vòng lặp Goal cần SHA nhưng Git chưa tồn tại |
| DEC-019 | Release đầu là `v0.1.0-alpha.1` production-shaped developer preview | Không claim production-certified trước M6/M7 |
| DEC-020 | Baseline embedding: local pinned TEI + `Qwen/Qwen3-Embedding-0.6B`, 1024 dimensions; M0 benchmark hardware/corpus có quyền HOLD/đề xuất đổi | DeepSeek chat không bị giả định là embedding API |
| DEC-021 | v0.1 public routing theo path qua site resolver; giữ abstraction host/subdomain cho Goal sau | Tránh buộc DNS/multi-domain trước evidence |
| DEC-027 | CSP theo bề mặt: Studio/auth có thể strict nonce; public cacheable pages dùng static/external/hash-compatible strategy đã test; không blanket nonce/`unsafe-inline` | Security không âm thầm phá ISR/PPR/CDN |
| DEC-028 | Domain schema không expose Data API; grants tường minh; chỉ policy/trigger Supabase hỗ trợ mới chạm managed schema; extension version được quan sát/test chứ không SQL-pin | Hấp thụ breaking changes Supabase hiện hành |

## Quyết định đề xuất không làm phình Goal đầu

| ID | Khuyến nghị |
|---|---|
| DEC-023 | Chấp nhận kiến trúc đích Vercel + managed Supabase + Spring boundary; M0-M4 chỉ local/CI hoặc preview không-production được cấp quyền riêng, chưa provision/spend hay claim production cho tới R3 và budget |
| DEC-026 | Chấp nhận innovation boundary nhưng giữ `accepted_innovation_hooks: []`; INN-* không tự vào Goal |

## Quyết định để Goal sau

| ID | Khi nào chốt |
|---|---|
| DEC-005 | Chọn staging/production target thực khi vào M7 |
| DEC-017 | Chốt SLO/RPO/RTO sau M6 measurement và cost model |
| DEC-024 | Chọn managed Kubernetes provider/region/capacity khi vào M7 |

## Receipt đã nhận

Người dùng đã trả lời: “Tôi chấp nhận toàn bộ mặc định trong gói quyết định, gồm DEC-011 và DEC-016; cho phép thực hiện C0-01 đến C0-07 trong D:\Nexora để tạo Goal M0-M4. Chưa cho phép first push, paid provision, release hoặc deploy.” Controller phải cập nhật từng DEC, chạy consistency/digest lại và lấy Advisor/Kongming cùng exact revision; câu trả lời không tự cấp quyền first push, paid provision, release hay deploy.
