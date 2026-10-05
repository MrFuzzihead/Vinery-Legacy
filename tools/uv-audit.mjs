// For each face of the chair's boxes, work out which texels it samples and how many are opaque.
// Answers "is the alpha test discarding these faces, or are the UVs wrong", without an in-game test.
import fs from 'fs';
import zlib from 'zlib';

function loadTexture(file) {
  const buf = fs.readFileSync(file);
  let off = 8, w = 0, h = 0, depth = 0, ct = 0, il = 0, plte = null, trns = null;
  const idat = [];
  while (off < buf.length) {
    const len = buf.readUInt32BE(off);
    const type = buf.toString('ascii', off + 4, off + 8);
    const d = buf.subarray(off + 8, off + 8 + len);
    if (type === 'IHDR') { w = d.readUInt32BE(0); h = d.readUInt32BE(4); depth = d[8]; ct = d[9]; il = d[12]; }
    else if (type === 'PLTE') plte = d;
    else if (type === 'tRNS') trns = d;
    else if (type === 'IDAT') idat.push(d);
    else if (type === 'IEND') break;
    off += 12 + len;
  }
  if (il) throw new Error('interlaced');
  const ch = { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }[ct];
  const perByte = 8 / depth;
  const bpr = depth === 8 ? w * ch : Math.ceil((w * depth) / 8);
  const unit = depth === 8 ? ch : 1;
  const raw = zlib.inflateSync(Buffer.concat(idat));
  const rows = Buffer.alloc(h * bpr);
  let pos = 0;
  for (let y = 0; y < h; y++) {
    const f = raw[pos++];
    const line = raw.subarray(pos, pos + bpr); pos += bpr;
    const cur = rows.subarray(y * bpr, (y + 1) * bpr);
    const prev = y > 0 ? rows.subarray((y - 1) * bpr, y * bpr) : Buffer.alloc(bpr);
    for (let i = 0; i < bpr; i++) {
      const a = i >= unit ? cur[i - unit] : 0, b = prev[i], c = i >= unit ? prev[i - unit] : 0;
      let v = line[i];
      if (f === 1) v += a; else if (f === 2) v += b; else if (f === 3) v += (a + b) >> 1;
      else if (f === 4) { const p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c); v += (pa <= pb && pa <= pc) ? a : (pb <= pc ? b : c); }
      cur[i] = v & 255;
    }
  }
  const sample = (x, y) => depth === 8 ? rows[y * bpr + x * unit]
    : (rows[y * bpr + Math.floor((x * depth) / 8)] >> (8 - depth - (x % perByte) * depth)) & ((1 << depth) - 1);
  return {
    w, h,
    alpha(x, y) {
      if (ct === 3) { const k = sample(x, y); return trns && k < trns.length ? trns[k] : 255; }
      if (ct === 4) return rows[y * bpr + x * 2 + 1];
      if (ct === 6) return rows[y * bpr + x * 4 + 3];
      return 255;
    },
  };
}

const DIR = 'src/main/resources/assets/vinery/textures/blocks/';
const TEX = { 0: loadTexture(DIR + 'dark_cherry_chair_base.png'), 2: loadTexture(DIR + 'dark_cherry_chair_top.png') };
const NAME = { 0: 'chair_base', 2: 'chair_top' };
const FACES = ['down', 'up', 'north', 'south', 'west', 'east'];

const src = fs.readFileSync('src/main/java/com/mrfuzzihead/vinery/core/block/ChairGeometry.java', 'utf8');
const nums = s => (s.match(/-?\d+\.?\d*/g) || []).map(Number);

let totalOpaque = 0, totalFaces = 0, discarded = 0;

for (const shape of ['LOWER', 'UPPER']) {
  const start = src.indexOf('Box[] ' + shape + ' = {');
  const body = src.slice(start, src.indexOf('\n};', start) === -1 ? src.length : src.indexOf('\n};', start));
  console.log(`\n=== ${shape} ===`);
  // Each box contributes 6 coordinates, a texture index, 6 UV rects and 6 texture indices.
  const chunks = body.split('new Box(').slice(1);
  chunks.forEach((chunk, bi) => {
    const uvBlock = chunk.slice(chunk.indexOf('new double[][] {'));
    const rectMatches = [...uvBlock.matchAll(/new double\[\] \{ ([^}]*) \}/g)].map(m => nums(m[1]));
    const texMatch = chunk.slice(chunk.indexOf('new int[] {')).match(/-?\d+/g).map(Number);
    rectMatches.forEach((rect, f) => {
      if (!rect || rect.length < 4 || !texMatch || texMatch[f] < 0) return;
      const t = TEX[texMatch[f]];
      if (!t) return;
      const x0 = Math.round(rect[0] * t.w), x1 = Math.round(rect[2] * t.w);
      const y0 = Math.round(rect[1] * t.h), y1 = Math.round(rect[3] * t.h);
      const lo = Math.min(x0, x1), hi = Math.max(x0, x1);
      const lo2 = Math.min(y0, y1), hi2 = Math.max(y0, y1);
      let opaque = 0, total = 0;
      for (let yy = lo2; yy < hi2; yy++) {
        for (let xx = lo; xx < hi; xx++) {
          if (xx < 0 || yy < 0 || xx >= t.w || yy >= t.h) continue;
          total++;
          if (t.alpha(xx, yy) >= 26) opaque++;
        }
      }
      if (!total) return;
      const frac = opaque / total;
      totalFaces++;
      totalOpaque += frac;
      if (frac === 0) discarded++;
      console.log(`  box${bi} ${FACES[f].padEnd(6)} ${NAME[texMatch[f]].padEnd(11)}`
        + ` px x${lo}..${hi} y${lo2}..${hi2}  opaque=${(frac * 100).toFixed(0)}%`
        + (frac === 0 ? '   <-- fully discarded by alpha test' : ''));
    });
  });
}

console.log(`\nmean opaque fraction: ${(totalOpaque / totalFaces * 100).toFixed(1)}% across ${totalFaces} faces`);
console.log(`faces entirely discarded by the alpha test: ${discarded}`);