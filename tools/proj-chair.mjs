// Projects a chair's boxes orthographically as ASCII, so the silhouette can be checked without
// seeing it. Reads the geometry straight out of ChairGeometry.java.
import fs from 'fs';

const src = fs.readFileSync('src/main/java/com/mrfuzzihead/vinery/core/block/ChairGeometry.java', 'utf8');

function boxesOf(name) {
  const start = src.indexOf(name + ' = {');
  const body = src.slice(start, src.indexOf('};', start));
  const re = /new Box\(\s*([-\d.]+),\s*([-\d.]+),\s*([-\d.]+),\s*([-\d.]+),\s*([-\d.]+),\s*([-\d.]+)/g;
  const out = [];
  let m;
  while ((m = re.exec(body))) out.push(m.slice(1, 7).map(Number));
  return out;
}

const lower = boxesOf('LOWER');
const upperRaw = boxesOf('UPPER');


// The chair spans two blocks: the upper half is drawn at y 0..0.3125 of the block ABOVE, so shift it.
// ChairGeometry stores 0-1 block space; scale back to the 0-16 grid this projection uses.
const scale = Math.max(...[...lower, ...upperRaw].flat()) <= 1.5 ? 16 : 1;
const scaled = b => b.map(v => v * scale);
const upper = upperRaw.map(scaled);
const lowerScaled = lower.map(scaled);
const shiftedUpper = upper.map(b => [b[0], b[1] + 16, b[2], b[3], b[4] + 16, b[5]]);
const all = [...lowerScaled, ...shiftedUpper];

const X = 16, Y = 32, Z = 16;
const grid = () => Array.from({ length: Y }, () => new Array(Z).fill(0));
const mark = (g, b, v) => {
  for (let x = Math.round(b[0]); x < Math.round(b[3]); x++) {
    for (let y = Math.round(b[1]); y < Math.round(b[4]); y++) {
      for (let z = Math.round(b[2]); z < Math.round(b[5]); z++) {
        if (x >= 0 && x < X && y >= 0 && y < Y && z >= 0 && z < Z) g[y][z] = v;
      }
    }
  }
};

const g = grid();
lowerScaled.forEach((b, i) => mark(g, b, i + 1));
shiftedUpper.forEach((b, i) => mark(g, b, 100 + i));

console.log('TOP VIEW  (looking down; rows = Y decreasing upward is irrelevant here, x across, z down)');
console.log('     ' + Array.from({ length: Z }, (_, z) => (z === 0 ? '' : String(z % 10))).join(''));
const top = grid();
lower.forEach((b, i) => mark(top, b, i + 1));
shiftedUpper.forEach((b, i) => mark(top, b, 100 + i));
for (let z = 0; z < Z; z++) {
  console.log(String(z).padStart(3) + ' |' + top.map((_, x) => {
    const v = top[0][x];
    return v ? '#' : '.';  // placeholder, replaced below
  }).join(''));
}

// Proper projections: a cell is filled if ANY box covers it at that (x,z) for top view.
function project(mode) {
  const lines = [];
  const N = 16;
  for (let a = 0; a < N; a++) {
    let row = '';
    for (let b = 0; b < N; b++) {
      let hit = 0;
      for (const box of all) {
        let inFirst, inSecond;
        if (mode === 'top') { inFirst = a >= box[0] && a < box[3]; inSecond = b >= box[2] && b < box[5]; }
        else { inFirst = a >= box[0] && a < box[3]; inSecond = b >= box[1] && b < box[4]; }
        if (inFirst && inSecond) { hit++; break; }
      }
      row += hit ? '#' : '.';
    }
    lines.push(row);
  }
  return lines;
}

console.log('\nTOP VIEW   (x across = left->right, z down = north->south; backrest should be at the far end)');
project('top').forEach((r, i) => console.log(String(i).padStart(3) + ' ' + r));

console.log('\nSIDE VIEW  (x across, y up from the floor; the seat is the low slab, backrest rises at one end)');
for (let y = Y - 1; y >= 0; y--) {
  let row = '';
  for (let x = 0; x < 16; x++) {
    let hit = false;
    for (const box of all) {
      if (x >= box[0] && x < box[3] && y >= box[1] && y < box[4]) { hit = true; break; }
    }
    row += hit ? '#' : '.';
  }
  if (row.includes('#')) console.log(String(y).padStart(3) + ' ' + row);
}
