import Link from "next/link";
import { ActionButton } from "../../../packages/ui-core/src/action-button";
import { AppShell, PageGrid } from "../../../packages/ui-core/src/app-shell";
import { InformationCard } from "../../../packages/ui-core/src/information-card";
import { StatusLabel } from "../../../packages/ui-core/src/status-label";

export const revalidate = 3600;

export default function HomePage() {
  return (
    <AppShell>
      <PageGrid>
        <header className="nx-site-header"><Link className="nx-wordmark" href="/">Nexora</Link><nav className="nx-nav" aria-label="Surface navigation"><Link aria-current="page" href="/">Public</Link><Link href="/studio">Studio</Link><Link href="/ai">AI</Link><Link href="/builder">Builder</Link></nav></header>
        <main id="main-content">
          <section className="nx-hero"><div><p className="nx-eyebrow">Product foundation / M1</p><h1>A reliable surface for work that matters.</h1><p className="nx-lede">Nexora’s interface foundation uses shared semantic tokens, clear ownership boundaries, and honest deterministic fixtures while product connections are not configured.</p><div className="nx-hero-actions"><Link className="nx-action-button nx-action-button--primary" href="/studio">Open Studio foundation</Link><ActionButton tone="secondary" disabled>Connection required</ActionButton></div><p className="nx-provenance">Static foundation preview. No tenant, repository, provider, or live metric is connected.</p></div><InformationCard title="Foundation status"><ul className="nx-state-list"><li><StatusLabel kind="fixture" /> Deterministic content only</li><li><StatusLabel kind="planned" /> Live product wiring is planned</li><li><StatusLabel kind="offline" /> Network-independent preview</li></ul></InformationCard></section>
          <section className="nx-card-grid" aria-label="Surface overview"><InformationCard title="Public"><p>Reading-first content with a clear, responsive grid.</p></InformationCard><InformationCard title="Studio"><p>Owned Ant Design wrapper boundary for dense workflows.</p></InformationCard><InformationCard title="AI and Builder"><p>Explicit source states and selection geometry, without live claims.</p></InformationCard></section>
        </main>
      </PageGrid>
    </AppShell>
  );
}
