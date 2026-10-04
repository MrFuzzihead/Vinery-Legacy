// Detects two same-facing faces on the same plane that also overlap on both remaining axes —
// the condition that causes z-fighting.
//
// Boxes are stored as [x0, y0, z0, x1, y1, z1], so lo[k] = b[k] and hi[k] = b[k + 3].
import fs from 'fs';

const src = fs.readFileSync('src/main/java/com/mrfuzzihead/vinery/core/block/WineRackGeometry.java', 'utf8');

function boxesOf(name) {
  const start = src.indexOf(name + ' = {');
  if (start === -1) throw new Error('no geometry named ' + name);
  const body = src.slice(start, src.indexOf('};', start));
  const re = /new Box\(\s*([-\d.]+),\s*([-\d.]+),\s*([-\d.]+),\s*([-\d.]+),\s*([-\d.]+),\s*([-\d.]+)/g;
  const out = [];
  let m;
  while ((m = re.exec(body))) out.push(m.slice(1, 7).map(Number));
  return out;
}

// axis 0/1/2 = x/y/z; `at` is the array index holding the plane coordinate.
const FACES = [
  { axis: 0, at: 0, sign: -1, name: '-X' },
  { axis: 0, at: 3, sign: +1, name: '+X' },
  { axis: 1, at: 1, sign: -1, name: '-Y' },
  { axis: 1, at: 4, sign: +1, name: '+Y' },
  { axis: 2, at: 2, sign: -1, name: '-Z' },
  { axis: 2, at: 5, sign: +1, name: '+Z' },
];

const lo = (b, k) => b[k];
const hi = (b, k) => b[k + 3];
const overlap = (a, b, k) => Math.min(hi(a, k), hi(b, k)) - Math.max(lo(a, k), lo(b, k)) > 1e-9;

let problems = 0;
for (const shape of ['BIG', 'SMALL', 'CABINET']) {
  const boxes = boxesOf(shape);
  const hits = [];
  for (let i = 0; i < boxes.length; i++) {
    for (let j = i + 1; j < boxes.length; j++) {
      for (const a of FACES) {
        for (const b of FACES) {
          if (a.axis !== b.axis || a.sign !== b.sign) continue;
          if (Math.abs(boxes[i][a.at] - boxes[j][b.at]) > 1e-9) continue;
          const others = [0, 1, 2].filter(k => k !== a.axis);
          if (overlap(boxes[i], boxes[j], others[0]) && overlap(boxes[i], boxes[j], others[1])) {
            hits.push(`box ${i} vs box ${j}: both ${a.name} at ${boxes[i][a.at]}`);
          }
        }
      }
    }
  }
  console.log(`${shape.padEnd(8)} ${String(boxes.length).padStart(2)} boxes -> ${hits.length ? hits.length + ' coplanar same-facing overlap(s)' : 'clean'}`);
  for (const h of hits) console.log('    ' + h);
  problems += hits.length;
}
console.log(problems ? '\nFAIL' : '\nPASS: no coplanar same-facing faces in any shape');
process.exit(problems ? 1 : 0);
