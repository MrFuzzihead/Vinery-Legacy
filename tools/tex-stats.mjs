// Luminance statistics for a texture: how much dark/light contrast survives, and whether the
// gaps read as shadow rather than as smeared mid-tones.
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
  if (depth !== 8 || interlace) throw new Error('unsupported');
  const ch = { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }[colorType];
  const raw = zlib.inflateSync(Buffer.concat(idat));
  const stride = w * ch, lines = Buffer.alloc(h * stride);
  let pos = 0;
  for (let y = 0; y < h; y++) {
    const f = raw[pos++];
    const line = raw.subarray(pos, pos + stride); pos += stride;
    const cur = lines.subarray(y * stride, (y + 1) * stride);
    const prev = y > 0 ? lines.subarray((y - 1) * stride, y * stride) : Buffer.alloc(stride);
    for (let i = 0; i < stride; i++) {
      const a = i >= ch ? cur[i - ch] : 0, b = prev[i], c = i >= ch ? prev[i - ch] : 0;
      let v = line[i];
      if (f === 1) v += a; else if (f === 2) v += b; else if (f === 3) v += (a + b) >> 1;
      else if (f === 4) { const p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c); v += (pa <= pb && pa <= pc) ? a : (pb <= pc ? b : c); }
      cur[i] = v & 255;
    }
  }
  const px = [];
  for (let i = 0; i < w * h; i++) {
    const s = i * ch;
    let r, g, b, a = 255;
    if (colorType === 3) { const k = lines[s]; r = plte[k * 3]; g = plte[k * 3 + 1]; b = plte[k * 3 + 2]; a = trns && k < trns.length ? trns[k] : 255; }
    else if (colorType === 2) { r = lines[s]; g = lines[s + 1]; b = lines[s + 2]; }
    else { r = lines[s]; g = lines[s + 1]; b = lines[s + 2]; a = colorType === 6 ? lines[s + 3] : (colorType === 4 ? lines[s + 1] : 255); }
    px.push({ r, g, b, a, lum: 0.299 * r + 0.587 * g + 0.114 * b });
  }
  return { w, h, px };
}

for (const f of process.argv.slice(2)) {
  const { w, h, px } = decode(f);
  const lum = px.map(p => p.lum);
  const mean = lum.reduce((a, b) => a + b, 0) / lum.length;
  const sd = Math.sqrt(lum.reduce((a, b) => a + (b - mean) ** 2, 0) / lum.length);
  const sorted = [...lum].sort((a, b) => a - b);
  const p10 = sorted[Math.floor(lum.length * 0.1)];
  const p90 = sorted[Math.floor(lum.length * 0.9)];
  const opaque = px.filter(p => p.a >= 250);
  const gaps = px.filter(p => p.a < 250);
  const gapLum = gaps.length ? gaps.reduce((a, p) => a + p.lum, 0) / gaps.length : NaN;
  const opaqueLum = opaque.length ? opaque.reduce((a, p) => a + p.lum, 0) / opaque.length : NaN;
  console.log(
    `${f.split(/[\\/]/).pop().padEnd(30)} mean=${mean.toFixed(1).padStart(5)} sd=${sd.toFixed(1).padStart(5)} ` +
    `p10=${p10.toFixed(0).padStart(3)} p90=${p90.toFixed(0).padStart(3)} ` +
    `leaf=${isFinite(opaqueLum) ? opaqueLum.toFixed(1).padStart(5) : '   - '} ` +
    `gap=${isFinite(gapLum) ? gapLum.toFixed(1).padStart(5) : '   - '} ` +
    `${isFinite(gapLum) ? 'gap/leaf=' + (gapLum / opaqueLum).toFixed(2) : ''}`);
}
