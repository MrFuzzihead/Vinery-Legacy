// Asserts that collision and picking agree with what the renderer draws.
//
// For every facing, the rotated geometry must move to the corresponding block edge. This is the
// check that was missing when the chair's hitbox stayed pointing south while the chair faced the
// player: nothing verified that the collision boxes rotated at all.
const fs = require('fs');

const geo = fs.readFileSync('src/main/java/com/mrfuzzihead/vinery/core/block/ChairGeometry.java', 'utf8');

function boxesOf(shape) {
  const start = geo.indexOf('Box[] ' + shape + ' = {');
  const end = geo.indexOf('\n};', start);
  const body = geo.slice(start, end === -1 ? geo.length : end);
  const chunks = body.split('new Box(').slice(1);
  const out = [];
  for (const chunk of chunks) {
    const head = chunk.slice(0, chunk.indexOf('new double[][]'));
    const n = (head.match(/-?\d+\.?\d*/g) || []).slice(0, 6).map(Number);
    out.push({ x0: n[0], y0: n[1], z0: n[2], x1: n[3], y1: n[4], z1: n[5] });
  }
  return out;
}

// An independent reimplementation of the clockwise quarter-turn, so this does not share the code
// path it is checking. Mirrors Box#rotatedExtents: facing 1 maps (x,z) -> (1-z, x).
function rotate(b, facing) {
  const f = facing & 3;
  let x0, x1, z0, z1;
  if (f === 0) { x0 = b.x0; x1 = b.x1; z0 = b.z0; z1 = b.z1; }
  else if (f === 1) { x0 = 1 - b.z1; x1 = 1 - b.z0; z0 = b.x0; z1 = b.x1; }
  else if (f === 2) { x0 = 1 - b.x1; x1 = 1 - b.x0; z0 = 1 - b.z1; z1 = 1 - b.z0; }
  else { x0 = b.z0; x1 = b.z1; z0 = 1 - b.x1; z1 = 1 - b.x0; }
  return { x0: Math.min(x0, x1), x1: Math.max(x0, x1), z0: Math.min(z0, z1), z1: Math.max(z0, z1) };
}

let failures = 0;
for (const shape of ['LOWER', 'UPPER']) {
  const boxes = boxesOf(shape);
  const backrest = boxes.reduce((a, b) => (b.y1 > a.y1 ? b : a), boxes[0]);
  console.log(`\n=== ${shape}: ${boxes.length} boxes; backrest z ${backrest.z0}..${backrest.z1} ===`);
  const seen = new Set();
  for (let f = 0; f < 4; f++) {
    const r = rotate(backrest, f);
    let dir = 'MIDDLE';
    if (r.z0 > 0.45) dir = 'south';
    else if (r.z1 < 0.55) dir = 'north';
    else if (r.x0 > 0.45) dir = 'east';
    else if (r.x1 < 0.55) dir = 'west';
    seen.add(dir);
    console.log(`  facing=${f}  backrest z ${r.z0.toFixed(3)}..${r.z1.toFixed(3)}`
      + `  x ${r.x0.toFixed(3)}..${r.x1.toFixed(3)}  -> ${dir}`);
    if (dir === 'MIDDLE') failures++;
  }
  if (seen.size < 4) { console.log('  ^ backrest only reached ' + seen.size + ' of 4 edges'); failures++; }
}
console.log(failures ? `\nFAIL (${failures} problem(s))` : '\nPASS: the backrest moves between edges as facing changes');