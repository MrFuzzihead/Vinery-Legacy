// Regenerates the leaf texture pair, following the pattern used by Et Futurum 2.6.48 (a real
// 1.7.10 mod that adds cherry/mangrove/modern leaves):
//
//     <wood>_leaves_N.png         cut-out art, used when the player has Fancy Graphics on
//     <wood>_leaves_N_opaque.png  generated opaque variant, used when Fast Graphics is on
//
// BaseLeaves.registerBlockIcons registers both, and BaseLeaves.getIcon picks between them with
// `isOpaqueCube() ? 1 : 0`.
//
// Why both are needed: 1.7.10 has NO alpha testing anywhere (grepping every patched Minecraft class
// for "alphaFunc"/"AlphaTest" returns zero hits), so a cut-out texture on a cube is rendered with
// GL blending and you see straight through it. Vanilla leaves avoid this by shipping an opaque
// texture. Fast graphics is the 1.7.10 default, so the opaque variant is what almost everyone sees;
// the cut-out art is kept intact for Fancy Graphics, where the gaps read as sky between leaves.
//
// The 1.21 originals are the source of truth and live in src/port-holding/textures-1.21/leaves,
// outside both the source set and the released jar.

import fs from 'fs';
import path from 'path';
import zlib from 'zlib';

const SRC_DIR = 'src/port-holding/textures-1.21/leaves';
const OUT_DIR = 'src/main/resources/assets/vinery/textures/blocks';
const SHADE = 0.5;      // how far a filled gap is darkened from its local leaf colour
const OPAQUE_THRESHOLD = 250;

// ---------------------------------------------------------------- PNG decode (filters undone)
function crc32(buf) {
  let c, table = crc32.table;
  if (!table) {
    table = crc32.table = new Int32Array(256);
    for (let n = 0; n < 256; n++) {
      c = n;
      for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
      table[n] = c;
    }
  }
  c = -1;
  for (let i = 0; i < buf.length; i++) c = table[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  return (c ^ -1) >>> 0;
}

function decode(file) {
  const buf = fs.readFileSync(file);
  let off = 8, w = 0, h = 0, depth = 0, colorType = 0, interlace = 0, plte = null, trns = null;
  const idat = [];
  while (off < buf.length) {
    const len = buf.readUInt32BE(off);
    const type = buf.toString('ascii', off + 4, off + 8);
    const d = buf.subarray(off + 8, off + 8 + len);
    if (type === 'IHDR') { w = d.readUInt32BE(0); h = d.readUInt32BE(4); depth = d[8]; colorType = d[9]; interlace = d[12]; }
    else if (type === 'PLTE') plte = d;
    else if (type === 'tRNS') trns = d;
    else if (type === 'IDAT') idat.push(d);
    else if (type === 'IEND') break;
    off += 12 + len;
  }
  if (depth !== 8) throw new Error(`${file}: bit depth ${depth}`);
  if (interlace) throw new Error(`${file}: interlaced`);

  const srcCh = { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }[colorType];
  const raw = zlib.inflateSync(Buffer.concat(idat));
  const stride = w * srcCh, lines = Buffer.alloc(h * stride);
  let pos = 0;
  for (let y = 0; y < h; y++) {
    const f = raw[pos++];
    const line = raw.subarray(pos, pos + stride); pos += stride;
    const cur = lines.subarray(y * stride, (y + 1) * stride);
    const prev = y > 0 ? lines.subarray((y - 1) * stride, y * stride) : Buffer.alloc(stride);
    for (let i = 0; i < stride; i++) {
      const a = i >= srcCh ? cur[i - srcCh] : 0, b = prev[i], c = i >= srcCh ? prev[i - srcCh] : 0;
      let v = line[i];
      if (f === 1) v += a; else if (f === 2) v += b; else if (f === 3) v += (a + b) >> 1;
      else if (f === 4) { const p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c); v += (pa <= pb && pa <= pc) ? a : (pb <= pc ? b : c); }
      cur[i] = v & 255;
    }
  }

  const rgba = Buffer.alloc(w * h * 4);
  for (let i = 0; i < w * h; i++) {
    const s = i * srcCh, d = i * 4;
    if (colorType === 3) {
      const k = lines[s];
      rgba[d] = plte[k * 3]; rgba[d + 1] = plte[k * 3 + 1]; rgba[d + 2] = plte[k * 3 + 2];
      rgba[d + 3] = trns && k < trns.length ? trns[k] : 255;
    } else if (colorType === 4) { rgba[d] = rgba[d + 1] = rgba[d + 2] = lines[s]; rgba[d + 3] = lines[s + 1]; }
    else if (colorType === 0) { rgba[d] = rgba[d + 1] = rgba[d + 2] = lines[s]; rgba[d + 3] = 255; }
    else if (colorType === 2) { rgba[d] = lines[s]; rgba[d + 1] = lines[s + 1]; rgba[d + 2] = lines[s + 2]; rgba[d + 3] = 255; }
    else { rgba[d] = lines[s]; rgba[d + 1] = lines[s + 1]; rgba[d + 2] = lines[s + 2]; rgba[d + 3] = lines[s + 3]; }
  }
  return { w, h, rgba };
}

// ---------------------------------------------------------------- PNG encode (RGBA8)
function chunk(type, data) {
  const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
  const td = Buffer.concat([Buffer.from(type, 'ascii'), data]);
  const crc = Buffer.alloc(4); crc.writeUInt32BE(crc32(td));
  return Buffer.concat([len, td, crc]);
}

function encode(w, h, rgba) {
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4);
  ihdr[8] = 8; ihdr[9] = 6;
  const stride = w * 4;
  const raw = Buffer.alloc(h * (stride + 1));
  for (let y = 0; y < h; y++) {
    raw[y * (stride + 1)] = 0;
    rgba.copy(raw, y * (stride + 1) + 1, y * stride, (y + 1) * stride);
  }
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk('IHDR', ihdr),
    chunk('IDAT', zlib.deflateSync(raw, { level: 9 })),
    chunk('IEND', Buffer.alloc(0)),
  ]);
}

// ---------------------------------------------------------------- generate the opaque variant
function makeOpaque(w, h, rgba) {
  const n = w * h;
  const filled = new Uint8Array(n * 3);
  const q = [];
  for (let i = 0; i < n; i++) {
    if (rgba[i * 4 + 3] >= OPAQUE_THRESHOLD) {
      filled[i * 3] = rgba[i * 4]; filled[i * 3 + 1] = rgba[i * 4 + 1]; filled[i * 3 + 2] = rgba[i * 4 + 2];
      q.push(i);
    }
  }
  let head = 0;
  while (head < q.length) {
    const i = q[head++], x = i % w, y = (i / w) | 0;
    const nb = [];
    if (x > 0) nb.push(i - 1);
    if (x < w - 1) nb.push(i + 1);
    if (y > 0) nb.push(i - w);
    if (y < h - 1) nb.push(i + w);
    for (const j of nb) {
      if (rgba[j * 4 + 3] >= OPAQUE_THRESHOLD) continue;
      if (filled[j * 3] || filled[j * 3 + 1] || filled[j * 3 + 2]) continue;
      filled[j * 3] = filled[i * 3]; filled[j * 3 + 1] = filled[i * 3 + 1]; filled[j * 3 + 2] = filled[i * 3 + 2];
      q.push(j);
    }
  }
  const out = Buffer.alloc(n * 4);
  let gaps = 0;
  for (let i = 0; i < n; i++) {
    const a = rgba[i * 4 + 3];
    if (a < OPAQUE_THRESHOLD) gaps++;
    const t = a / 255;
    out[i * 4]     = Math.round(rgba[i * 4] * t + filled[i * 3] * SHADE * (1 - t));
    out[i * 4 + 1] = Math.round(rgba[i * 4 + 1] * t + filled[i * 3 + 1] * SHADE * (1 - t));
    out[i * 4 + 2] = Math.round(rgba[i * 4 + 2] * t + filled[i * 3 + 2] * SHADE * (1 - t));
    out[i * 4 + 3] = 255;
  }
  return { out, gaps };
}

// ---------------------------------------------------------------- run
if (!fs.existsSync(SRC_DIR)) { console.error('missing originals: ' + SRC_DIR); process.exit(1); }

const names = fs.readdirSync(SRC_DIR).filter(n => /leaves_\d+\.png$/.test(n)).sort();
if (!names.length) { console.error('no originals in ' + SRC_DIR); process.exit(1); }

for (const name of names) {
  const src = path.join(SRC_DIR, name);

  // 1. ship the 1.21 cut-out art untouched
  fs.copyFileSync(src, path.join(OUT_DIR, name));

  // 2. generate the opaque sibling
  const img = decode(src);
  const { out, gaps } = makeOpaque(img.w, img.h, img.rgba);
  const opaqueName = name.replace(/\.png$/, '_opaque.png');
  fs.writeFileSync(path.join(OUT_DIR, opaqueName), encode(img.w, img.h, out));
  console.log(`${name.padEnd(28)} + ${opaqueName.padEnd(34)} ${img.w}x${img.h}, filled ${gaps} gap px`);
}
console.log('\ncut-out art shipped as-is; opaque variants generated for Fast Graphics.');
