// Prints a texture's alpha channel and luminance as ASCII, so the art can be inspected without
// seeing the render.
import fs from 'fs';
import zlib from 'zlib';

function load(file) {
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
  const sample = (x, y) => depth === 8
    ? rows[y * bpr + x * unit]
    : (rows[y * bpr + Math.floor((x * depth) / 8)] >> (8 - depth - (x % perByte) * depth)) & ((1 << depth) - 1);

  const px = [];
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      let r, g, b, a = 255;
      if (ct === 3) { const k = sample(x, y); r = plte[k * 3]; g = plte[k * 3 + 1]; b = plte[k * 3 + 2]; a = trns && k < trns.length ? trns[k] : 255; }
      else if (ct === 0) { r = g = b = sample(x, y); }
      else if (ct === 4) { r = g = b = sample(x, y); a = rows[y * bpr + x * 2 + 1]; }
      else if (ct === 2) { r = rows[y * bpr + x * 3]; g = rows[y * bpr + x * 3 + 1]; b = rows[y * bpr + x * 3 + 2]; }
      else { r = rows[y * bpr + x * 4]; g = rows[y * bpr + x * 4 + 1]; b = rows[y * bpr + x * 4 + 2]; a = rows[y * bpr + x * 4 + 3]; }
      px.push({ r, g, b, a });
    }
  }
  return { w, h, px };
}

const RAMP = ' .:-=+*#%@';
for (const f of process.argv.slice(2)) {
  const { w, h, px } = load(f);
  console.log(`\n=== ${f.split(/[\\/]/).pop()}  ${w}x${h} ===`);
  console.log('  ALPHA (space = fully transparent, @ = opaque):');
  for (let y = 0; y < h; y++) {
    let row = '  ';
    for (let x = 0; x < w; x++) row += RAMP[Math.round((px[y * w + x].a / 255) * (RAMP.length - 1))];
    console.log(row + '   ' + y);
  }
  console.log('  COLOUR (luminance):');
  for (let y = 0; y < h; y++) {
    let row = '  ';
    for (let x = 0; x < w; x++) {
      const p = px[y * w + x];
      const lum = (0.299 * p.r + 0.587 * p.g + 0.114 * p.b) * (p.a / 255);
      row += RAMP[Math.round((lum / 255) * (RAMP.length - 1))];
    }
    console.log(row);
  }
}
