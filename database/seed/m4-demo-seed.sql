-- M4-S01 deterministic RAG demo seed: Nexora University synthetic documents
-- and retrieval expectations. Fixture-only; never applies to production
-- tenants and never commits a credential. Idempotent through ON CONFLICT.

BEGIN;
SET LOCAL ROLE nexora_migrator;

INSERT INTO nexora.knowledge_bases (id, organization_id, name, description, state, created_by_subject_id) VALUES
  ('11000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001',
   'Nexora University Knowledge', 'Synthetic demo knowledge for RAG evaluation.', 'ACTIVE',
   '20000000-0000-4000-8000-000000000001')
ON CONFLICT (id) DO NOTHING;

INSERT INTO nexora.documents (id, knowledge_base_id, organization_id, original_name, stored_object_key, content_type, byte_size, sha256, state, uploaded_by_subject_id) VALUES
  ('30000000-0000-4000-8000-000000000001', '11000000-0000-4000-8000-000000000001',
   '10000000-0000-4000-8000-000000000001', 'student-handbook-2026.txt',
   'organizations/10000000-0000-4000-8000-000000000001/knowledge/11000000-0000-4000-8000-000000000001/documents/30000000-0000-4000-8000-000000000001',
   'text/plain', 1024,
   'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'READY',
   '20000000-0000-4000-8000-000000000001'),
  ('30000000-0000-4000-8000-000000000002', '11000000-0000-4000-8000-000000000001',
   '10000000-0000-4000-8000-000000000001', 'academic-regulations.md',
   'organizations/10000000-0000-4000-8000-000000000001/knowledge/11000000-0000-4000-8000-000000000001/documents/30000000-0000-4000-8000-000000000002',
   'text/markdown', 2048,
   'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb', 'READY',
   '20000000-0000-4000-8000-000000000001'),
  ('30000000-0000-4000-8000-000000000003', '11000000-0000-4000-8000-000000000001',
   '10000000-0000-4000-8000-000000000001', 'ai-lab-guide.pdf',
   'organizations/10000000-0000-4000-8000-000000000001/knowledge/11000000-0000-4000-8000-000000000001/documents/30000000-0000-4000-8000-000000000003',
   'application/pdf', 4096,
   'cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc', 'READY',
   '20000000-0000-4000-8000-000000000001')
ON CONFLICT (id) DO NOTHING;

INSERT INTO nexora.chunks (id, document_id, organization_id, knowledge_base_id, chunk_index, text, token_count, sha256, chunk_strategy_version, state) VALUES
  ('40000000-0000-4000-8000-000000000001', '30000000-0000-4000-8000-000000000002',
   '10000000-0000-4000-8000-000000000001', '11000000-0000-4000-8000-000000000001', 0,
   'Nexora University publishes immutable page versions through a review workflow. Rollback creates a new version and preserves the immutable publication history.',
   26, 'dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd', 'nexora-chunk-v1', 'ACTIVE'),
  ('40000000-0000-4000-8000-000000000002', '30000000-0000-4000-8000-000000000001',
   '10000000-0000-4000-8000-000000000001', '11000000-0000-4000-8000-000000000001', 0,
   'The Student Handbook covers enrollment, academic integrity and campus resources for the 2026 intake.',
   18, 'eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee', 'nexora-chunk-v1', 'ACTIVE'),
  ('40000000-0000-4000-8000-000000000003', '30000000-0000-4000-8000-000000000001',
   '10000000-0000-4000-8000-000000000001', '11000000-0000-4000-8000-000000000001', 1,
   'Enrollment requires a verified identity, an active organization membership and a reviewed application.',
   16, 'ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', 'nexora-chunk-v1', 'ACTIVE'),
  ('40000000-0000-4000-8000-000000000004', '30000000-0000-4000-8000-000000000001',
   '10000000-0000-4000-8000-000000000001', '11000000-0000-4000-8000-000000000001', 2,
   'Campus resources include the library, the AI lab and the cloud computing cluster with booked access windows.',
   20, '9999999999999999999999999999999999999999999999999999999999999999', 'nexora-chunk-v1', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

COMMIT;
