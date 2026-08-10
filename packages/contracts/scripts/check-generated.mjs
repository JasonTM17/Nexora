import { readFile } from "node:fs/promises";
import { outputPath, renderClient, specPath } from "./generate-client.mjs";

const spec = JSON.parse(await readFile(specPath, "utf8"));
const expected = renderClient(spec);
const actual = await readFile(outputPath, "utf8");

if (actual !== expected) {
  process.stderr.write("Generated client drift detected. Run: node packages/contracts/scripts/generate-client.mjs\n");
  process.exitCode = 1;
} else {
  process.stdout.write("Generated client matches the OpenAPI source.\n");
}
