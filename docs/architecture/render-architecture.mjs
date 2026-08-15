// R00A architecture evidence renderer.
// Extracts every fenced mermaid block from docs/architecture/**/*.md,
// renders each to docs/architecture/renders/<doc>-<n>.svg with mmdc and
// records renderer version plus SHA-256 digests of source block and output.
// Run: node docs/architecture/render-architecture.mjs
// Output: docs/architecture/renders/ + docs/architecture/architecture-evidence.json

import { createHash } from "node:crypto";
import { mkdirSync, readFileSync, readdirSync, writeFileSync } from "node:fs";
import { execFileSync } from "node:child_process";
import { basename, dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const archDir = resolve(here, ".");
const outDir = join(archDir, "renders");
mkdirSync(outDir, { recursive: true });

const files = readdirSync(archDir).filter((f) => f.endsWith(".md"));

/** @type {Array<{source: string, block: number, digest: string, output: string, outputDigest: string}>} */
const diagrams = [];

for (const file of files) {
  const text = readFileSync(join(archDir, file), "utf8");
  const fence = /```mermaid\r?\n([\s\S]*?)```/g;
  let match;
  let block = 0;
  while ((match = fence.exec(text)) !== null) {
    block += 1;
    const source = match[1].trim();
    const slug = `${basename(file, ".md")}-${String(block).padStart(2, "0")}`;
    const mmdPath = join(outDir, `${slug}.mmd`);
    const svgPath = join(outDir, `${slug}.svg`);
    writeFileSync(mmdPath, source, "utf8");
    try {
      execFileSync("npx.cmd", ["-y", "@mermaid-js/mermaid-cli@11.16.0", "-i", mmdPath, "-o", svgPath, "-b", "#ffffff"], {
        stdio: "pipe",
        shell: true,
      });
    } catch (error) {
      console.error(`FAILED ${file}#block-${block}: ${String(error.stderr ?? error).split("\n").slice(0, 4).join(" | ")}`);
      continue;
    }
    diagrams.push({
      source: `${file}#block-${block}`,
      block,
      digest: createHash("sha256").update(source).digest("hex"),
      output: `renders/${slug}.svg`,
      outputDigest: createHash("sha256").update(readFileSync(svgPath)).digest("hex"),
    });
    console.log(`rendered ${slug}.svg (${file}#block-${block})`);
  }
}

const evidence = {
  task: "R00A",
  productSha: process.argv[2] ?? "unset",
  renderer: { tool: "@mermaid-js/mermaid-cli", version: "11.16.0" },
  generatedAt: new Date().toISOString(),
  diagrams,
};

writeFileSync(join(archDir, "architecture-evidence.json"), JSON.stringify(evidence, null, 2) + "\n");
console.log(`recorded ${diagrams.length} diagrams to architecture-evidence.json`);
