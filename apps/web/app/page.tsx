import Link from "next/link";
import { AppShell, PageGrid } from "../../../packages/ui-core/src/app-shell";
import { InformationCard } from "../../../packages/ui-core/src/information-card";
import { StatusLabel } from "../../../packages/ui-core/src/status-label";

export const revalidate = 3600;

export default function HomePage() {
  return (
    <AppShell>
      <PageGrid>
        <header className="nx-site-header"><Link className="nx-wordmark" href="/">Nexora</Link><nav className="nx-nav" aria-label="Surface navigation"><Link aria-current="page" href="/">Overview</Link><Link href="/knowledge">Knowledge</Link><Link href="/studio">Studio</Link><Link href="/ai">AI</Link><Link href="/builder">Builder</Link><Link href="/account">Account</Link></nav></header>
        <main id="main-content">
          <section className="nx-hero nx-hero--home">
            <div className="nx-hero-copy">
              <p className="nx-eyebrow">Tenant-aware content workspace</p>
              <h1>Make publishing, knowledge, and governance easier to trust.</h1>
              <p className="nx-lede">Nexora brings publishing, retrieval, and operational boundaries into one deliberate workspace. The visible product remains honest about what is connected and what is represented by local fixture evidence.</p>
              <div className="nx-hero-actions"><Link className="nx-action-button nx-action-button--primary" href="/knowledge">Explore knowledge workspace</Link><Link className="nx-action-button nx-action-button--secondary" href="/studio">Open Studio</Link></div>
              <ul className="nx-hero-proof" aria-label="Product principles"><li><span>01</span> Server-derived membership and permission checks.</li><li><span>02</span> PostgreSQL is the durable source of tenant truth.</li><li><span>03</span> Realtime signals always converge through a durable refetch.</li></ul>
              <p className="nx-provenance">Local integration preview. It does not claim a hosted tenant, connected provider, or live production telemetry.</p>
            </div>
            <aside className="nx-hero-panel" aria-label="Workspace foundations">
              <p className="nx-eyebrow">Designed for explicit boundaries</p>
              <div className="nx-hero-signal"><strong>Identity</strong><span>Current membership, not browser assertions.</span></div>
              <div className="nx-hero-signal"><strong>Content</strong><span>Immutable publishing history and safe workflow states.</span></div>
              <div className="nx-hero-signal"><strong>Knowledge</strong><span>Authorize before retrieval, context, and citation.</span></div>
              <div className="nx-hero-panel-footer"><StatusLabel kind="fixture" /> Local evidence and deterministic states</div>
            </aside>
          </section>
          <section className="nx-home-section" aria-labelledby="workspace-capabilities"><div className="nx-section-heading"><p className="nx-eyebrow">Explore the workspace</p><h2 id="workspace-capabilities">Clear paths for each kind of work.</h2><p>Every surface keeps its authority boundary visible instead of implying connected data.</p></div><div className="nx-card-grid nx-home-capabilities"><InformationCard title="Knowledge workspace" action={<StatusLabel kind="planned" />}><p>Ingest, organize, and query tenant-scoped knowledge with explicit job and citation states.</p><Link className="nx-card-link" href="/knowledge">Open knowledge workspace <span aria-hidden="true">→</span></Link></InformationCard><InformationCard title="Publishing studio" action={<StatusLabel kind="fixture" />}><p>Move through content and page workflows with visible ownership, validation, and recovery states.</p><Link className="nx-card-link" href="/studio">Open Studio <span aria-hidden="true">→</span></Link></InformationCard><InformationCard title="Safe exploration" action={<StatusLabel kind="offline" />}><p>Use AI and Builder surfaces that preserve no-answer, denied, and source-loss states instead of inventing results.</p><Link className="nx-card-link" href="/ai">Open AI workspace <span aria-hidden="true">→</span></Link></InformationCard></div></section>
          <section className="nx-trust-rail" aria-labelledby="trust-rail-heading"><div className="nx-section-heading"><p className="nx-eyebrow">Trust rail</p><h2 id="trust-rail-heading">A focused path from browser action to durable evidence.</h2></div><ol><li><span className="nx-rail-step">1</span><div><strong>Same-origin BFF</strong><p>Browser requests stay behind a narrowly-scoped server boundary.</p></div></li><li><span className="nx-rail-step">2</span><div><strong>Spring and RLS</strong><p>Fresh authorization precedes sensitive tenant work.</p></div></li><li><span className="nx-rail-step">3</span><div><strong>Durable state and events</strong><p>PostgreSQL truth and bounded event delivery remain the recovery point.</p></div></li></ol></section>
        </main>
      </PageGrid>
    </AppShell>
  );
}
