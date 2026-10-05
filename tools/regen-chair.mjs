// Regenerates ChairGeometry.java from the 1.21 chair models.
//
// Run from the repo root:  node tools/regen-chair.mjs
// Exists because embedding Java in a shell heredoc loses every backtick it contains.

import fs from 'fs';
import { execFileSync } from 'child_process';

const ARGS = ['dark_cherry_chair_base=0,dark_cherry_chair_top=2'];

function generate(model, skip) {
  const argv = ['tools/gen-box-geometry.mjs', model, ARGS[0]];
  if (skip !== undefined) argv.push(String(skip));
  const out = execFileSync(process.execPath, argv, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] });
  // Drop only the generator's two header lines (the file banner and the 'Elements omitted'
  // note). Stripping every // line also removed the per-element notes, which is what hid the
  // fact that a skipped element had still been emitted.
  const lines = out.split('\n').filter(l => !l.startsWith('// Generated from') && !l.startsWith('// Elements omitted'));
  return lines.join('\n').trim().replace('GENERATED', '@');
}

const lower = generate('dark_cherry_chair_bottom').replace('@', 'LOWER');
const upper = generate('dark_cherry_chair_top').replace('@', 'UPPER');

const doc = [
  '/**',
  ' * Geometry generated from the 1.21 chair models by tools/gen-box-geometry.mjs — do not hand-edit.',
  ' *',
  ' * <p>',
  ' * The chair occupies two blocks: <code>dark_cherry_chair_bottom</code> is the seat and legs and fills',
  ' * the lower block; <code>dark_cherry_chair_top</code> is the backrest and sits in the block above at',
  ' * y 0..0.3125 of its own block, making the chair 1.3125 blocks tall.',
  ' *',
  ' * <p>',
  ' * The 1.21 models address sub-rectangles of packed 16x16 sheets, so every box carries per-face UV',
  ' * rectangles and per-face textures and is drawn by the quad path rather than by',
  ' * {@code renderStandardBlock}, which can only map a whole icon onto a face.',
  ' *',
  ' * <p>',
  ' * Regenerating:',
  ' *',
  ' * <pre>',
  ' * node tools/regen-chair.mjs',
  ' * </pre>',
  ' *',
  ' * <p>',
  ' * Element 7 of the lower half is skipped. It reads <code>[11,4,4] -&gt; [3,6,12]</code> — bounds',
  ' * inverted, x running from 11 down to 3 — and normalises to element 6&apos;s cushion. The 1.21',
  ' * {@code ChairBlock} confirms it independently: SHAPE_LOWER holds exactly seven boxes, matching',
  ' * elements 0 through 6, with no eighth.',
  ' */',
].join('\n');

const path = 'src/main/java/com/mrfuzzihead/vinery/core/block/ChairGeometry.java';
const current = fs.readFileSync(path, 'utf8');
const head = current.slice(0, current.indexOf('public final class ChairGeometry'));
fs.writeFileSync(path, head + 'public final class ChairGeometry {\n\n    private ChairGeometry() {}\n\n'
  + doc + '\n\n    ' + lower + '\n\n    ' + upper + '\n}\n');
console.log('regenerated ' + path);
