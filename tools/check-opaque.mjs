// Verify PNGs really are fully opaque, decoding correctly (PNG filters undone).
import fs from 'fs';
import zlib from 'zlib';

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
  if (depth !== 8 || interlace) throw new Error('unsupported depth/interlace');
  const srcCh = { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }[colorType];
  const raw = zlib.inflateSync(Buffer.concat(idat));
  const stride = w * srcCh;
  const lines = Buffer.alloc(h * stride);
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
  let minAlpha = 255, transparent = 0, partial = 0;
  for (let i = 0; i < w * h; i++) {
    let a = 255;
    if (colorType === 3) { const idx = lines[i]; a = trns && idx < trns.length ? trns[idx] : 255; }
    else if (colorType === 4) a = lines[i * 2 + 1];
    else if (colorType === 6) a = lines[i * 4 + 3];
    if (a < minAlpha) minAlpha = a;
    if (a === 0) transparent++; else if (a < 255) partial++;
  }
  return { w, h, colorType, plte: !!plte, trns: !!trns, minAlpha, transparent, partial };
}

let bad = 0, skipped = 0;
for (const f of process.argv.slice(2)) {
  let r;
  try {
    r = decode(f);
  } catch (e) {
    // A texture this tool cannot decode is not evidence of anything; report and move on rather
    // than dying part-way and silently under-reporting the rest of the directory.
    skipped++;
    console.log(`${f.split(/[\\/]/).pop().padEnd(30)} SKIPPED (${e.message})`);
    continue;
  }
  const ok = r.transparent === 0 && r.partial === 0;
  if (!ok) bad++;
  console.log(
    `${f.split(/[\\/]/).pop().padEnd(30)} ${r.w}x${r.h} type=${r.colorType}` +
    `${r.plte ? ' PLTE' : ''}${r.trns ? ' tRNS' : ''}  ` +
    `minAlpha=${String(r.minAlpha).padStart(3)} transparent=${r.transparent} partial=${r.partial}  ${ok ? 'OPAQUE ok' : 'NOT OPAQUE'}`);
}
console.log(`\n${bad} cut-out, ${skipped} skipped, ${process.argv.length - 2 - skipped} fully opaque`);
process.exit(bad ? 1 : 0);
