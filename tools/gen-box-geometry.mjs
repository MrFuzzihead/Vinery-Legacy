// Generates Java Box arrays from a 1.21 block model, carrying per-face textures and UV rectangles.
//
// Why this exists: the 1.21 models address sub-rectangles of packed 16x16 sheets -- the chair samples
// [3,3,13,13] for the seat and [0,8,2,16] for a leg -- and 1.7.10's RenderBlocks#renderStandardBlock
// can only map a whole icon onto a face. Hand-transcribing those UVs is exactly the kind of work
// that produces confident wrong results, so the model file is the input and the Java is the output.
//
// Usage:
//   node tools/gen-box-geometry.mjs <model-name> <texName=index>[,<texName=index>...]
//     <model-name> is a block model JSON name without the .json, e.g. dark_cherry_chair_bottom
//     the index arguments are the icon indices in the block's faceIcons array, and every texture
//     the model references must be listed
//
// Face order emitted is BoxRenderer's: down, up, north, south, west, east.
//
// V axis: Blockbench writes uv as [u1, v1, u2, v2] with v measured from the TOP of the image.
// IIcon#getInterpolatedV is 0 at the icon's minV, which is likewise the icon's TOP row -- so v is
// used as-is, with NO inversion. An earlier version emitted `1 - v` on the assumption that GL's V ran
// the other way; it does not, and that single flip sent every face to the opposite end of the
// sprite. On a sheet with transparent padding at the bottom, most faces then sampled only padding,
// were discarded by the alpha test, and the chair rendered as a few stray lines.

import fs from 'fs';

const MODELS = 'src/main/resources/assets/vinery/models/block/';
const FACES = ['down', 'up', 'north', 'south', 'west', 'east'];

function resolve(modelName, seen = new Set()) {
  const file = MODELS + modelName + '.json';
  if (!fs.existsSync(file)) { console.error('no model: ' + file); process.exit(1); }
  if (seen.has(modelName)) { console.error('parent cycle at ' + modelName); process.exit(1); }
  seen.add(modelName);
  const model = JSON.parse(fs.readFileSync(file, 'utf8'));

  let textures = model.textures || {};
  let elements = model.elements || null;

  if (model.parent) {
    const parent = resolve(model.parent.replace(/^vinery:block\//, ''), seen);
    // A child may add to or override its parent's texture bindings.
    textures = { ...parent.textures, ...textures };
    if (!elements) elements = parent.elements;
  }
  return { textures, elements };
}

const [, , modelName, indexArg, skipArg] = process.argv;
if (!modelName || !indexArg) {
  console.error('usage: gen-box-geometry.mjs <model> <tex=index,...> [skipElementIndices]');
  process.exit(1);
}
const indices = {};
for (const pair of indexArg.split(',')) {
  const [name, i] = pair.split('=');
  indices[name] = Number(i);
}
const skip = new Set((skipArg || '').split(',').filter(Boolean).map(Number));

const { textures, elements } = resolve(modelName);
if (!elements) { console.error('model has no elements (and none inherited)'); process.exit(1); }

const nameFor = slot => {
  // Faces reference slots as "#0"; the textures map keys them as "0".
  let v = textures[String(slot).replace(/^#/, '')];
  while (typeof v === 'string' && v.startsWith('#')) v = textures[v.slice(1)];
  return typeof v === 'string' ? v.replace(/^vinery:block\//, '') : null;
};

const used = new Set();
const lines = [];

elements.forEach((el, i) => {
  // Omitted outright. Emitting a placeholder comment here did nothing once the caller
  // stripped comments, and the box it named still got drawn.
  if (skip.has(i)) { return; }

  // Normalise, and flag the inverted-export case loudly rather than silently transcribing it.
  const from = el.from, to = el.to;
  const inverted = from.some((v, k) => v > to[k]);
  const lo = from.map((v, k) => Math.min(v, to[k]));
  const hi = from.map((v, k) => Math.max(v, to[k]));
  const sameAsPrev = elements.findIndex((o, j) => j !== i
    && Math.min(o.from[0], o.to[0]) === lo[0] && Math.min(o.from[1], o.to[1]) === lo[1]
    && Math.min(o.from[2], o.to[2]) === lo[2] && Math.max(o.from[0], o.to[0]) === hi[0]
    && Math.max(o.from[1], o.to[1]) === hi[1] && Math.max(o.from[2], o.to[2]) === hi[2]);

  const uvs = FACES.map(face => {
    const f = (el.faces || {})[face];
    if (!f) return 'null';
    const tex = nameFor(f.texture);
    if (tex === null) return 'null';
    used.add(tex);
    if (!(tex in indices)) { console.error(`texture ${tex} has no index; pass ${tex}=<n>`); process.exit(1); }
    const [u1, v1, u2, v2] = f.uv;
    const a = (u1 / 16).toFixed(4), b = (v1 / 16).toFixed(4);
    const c = (u2 / 16).toFixed(4), d = (v2 / 16).toFixed(4);
    return `new double[] { ${a}, ${b}, ${c}, ${d} }`;
  });
  const texIdx = FACES.map(face => {
    const f = (el.faces || {})[face];
    if (!f) return -1;
    const tex = nameFor(f.texture);
    return tex === null ? -1 : indices[tex];
  });


  // The model uses 0-16 space; Box takes 0-1 block space. The old seven-argument Box constructor
  // divided for you behind a fromModelSpace flag; the nine-argument one used for per-face UVs does
  // not, so the conversion has to happen here or every box is drawn and collided 16x too large.
  const unit = n => (Number(n) / 16).toFixed(5);
  const comment = inverted
    ? `element ${i} (inverted bounds, normalised; duplicates element ${sameAsPrev})`
    : `element ${i}`;
  lines.push(`        // ${comment}`);
  lines.push(`        new Box(${lo.map(unit).join(', ')}, ${hi.map(unit).join(', ')}, -1,`);
  lines.push(`            new double[][] { ${uvs.join(', ')} },`);
  lines.push(`            new int[] { ${texIdx.join(', ')} }),`);
});

console.log(`// Generated from ${modelName}.json by tools/gen-box-geometry.mjs — do not hand-edit.`);
if (skip.size) console.log(`// Elements omitted: ${[...skip].join(', ')} (see the class javadoc).`);
console.log(`public static final Box[] GENERATED = {`);
console.log(lines.join('\n'));
console.log('};');
console.error('\ntextures referenced: ' + [...used].sort().join(', '));
