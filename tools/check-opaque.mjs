// Report PNG header fields and whether the image has any transparency.
//
// Handles every PNG variant Minecraft accepts, including 4-bit palette images: a large share of
// Vinery's textures are depth-4 PALETTE, and an earlier version of this tool only read depth 8,
// which silently skipped 47 of 144 files and made a cut-out audit look clean when it was not.
import fs from 'fs';
import zlib from 'zlib';

const CT = { 0: 'GREY', 2: 'RGB', 3: 'PALETTE', 4: 'GREY+ALPHA', 6: 'RGBA' };

function chunks(file) {
  const buf = fs.readFileSync(file);
  let off = 8;
  const out = {};
  const idat = [];
  while (off < buf.length) {
    const len = buf.readUInt32BE(off);
    const type = buf.toString('ascii', off + 4, off + 8);
    const d = buf.subarray(off + 8, off + 8 + len);
    if (type === 'IHDR') {
      out.w = d.readUInt32BE(0); out.h = d.readUInt32BE(4);
      out.depth = d[8]; out.colorType = d[9]; out.interlace = d[12];
    } else if (type === 'PLTE') out.plte = d;
    else if (type === 'tRNS') out.trns = d;
    else if (type === 'IDAT') idat.push(d);
    else if (type === 'IEND') break;
    off += 12 + len;
  }
  out.idat = idat;
  return out;
}

// Alpha per pixel, or null if the format cannot express transparency at all.
function alphas(h) {
  const { w, h: height, depth, colorType, interlace, plte, trns } = h;
  if (interlace) throw new Error('interlaced');
  if (depth !== 1 && depth !== 2 && depth !== 4 && depth !== 8) throw new Error('depth ' + depth);

  const samplesPerByte = 8 / depth;
  const bytesPerRow = depth === 8 ? w * ({ 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }[colorType]) : Math.ceil((w * depth) / 8);
  const filterUnit = depth === 8 ? { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }[colorType] : 1;

  const raw = zlib.inflateSync(Buffer.concat(h.idat));
  const rows = Buffer.alloc(height * bytesPerRow);
  let pos = 0;
  for (let y = 0; y < height; y++) {
    const f = raw[pos++];
    const line = raw.subarray(pos, pos + bytesPerRow); pos += bytesPerRow;
    const cur = rows.subarray(y * bytesPerRow, (y + 1) * bytesPerRow);
    const prev = y > 0 ? rows.subarray((y - 1) * bytesPerRow, y * bytesPerRow) : Buffer.alloc(bytesPerRow);
    for (let i = 0; i < bytesPerRow; i++) {
      const a = i >= filterUnit ? cur[i - filterUnit] : 0;
      const b = prev[i];
      const c = i >= filterUnit ? prev[i - filterUnit] : 0;
      let v = line[i];
      if (f === 1) v += a;
      else if (f === 2) v += b;
      else if (f === 3) v += (a + b) >> 1;
      else if (f === 4) {
        const p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
        v += (pa <= pb && pa <= pc) ? a : (pb <= pc ? b : c);
      }
      cur[i] = v & 255;
    }
  }

  // Returns the palette index (or grey/alpha value) for each pixel.
  const sample = (x, y) => {
    if (depth === 8) return rows[y * bytesPerRow + x * filterUnit];
    const byte = rows[y * bytesPerRow + Math.floor((x * depth) / 8)];
    const shift = 8 - depth - (x % samplesPerByte) * depth;
    return (byte >> shift) & ((1 << depth) - 1);
  };

  const out = [];
  for (let y = 0; y < height; y++) {
    for (let x = 0; x < w; x++) {
      if (colorType === 3) { const k = sample(x, y); out.push(trns && k < trns.length ? trns[k] : 255); }
      else if (colorType === 0) out.push(255);
      else if (colorType === 2) out.push(255);
      else if (colorType === 4) out.push(rows[y * bytesPerRow + x * 2 + 1]);
      else out.push(rows[y * bytesPerRow + x * 4 + 3]);
    }
  }
  return out;
}

let cutout = 0, skipped = 0, opaque = 0;
const rows = [];
for (const f of process.argv.slice(2)) {
  const h = chunks(f);
  let a;
  try { a = alphas(h); }
  catch (e) { skipped++; rows.push([f, `${h.w}x${h.h} depth=${h.depth} ${CT[h.colorType]}`, 'SKIPPED ' + e.message]); continue; }
  const transparent = a.filter(v => v === 0).length;
  const partial = a.filter(v => v > 0 && v < 255).length;
  const canExpressAlpha = h.colorType === 3 ? !!h.trns : (h.colorType === 4 || h.colorType === 6);
  const ok = !canExpressAlpha || (transparent === 0 && partial === 0);
  ok ? opaque++ : cutout++;
  rows.push([f, `${h.w}x${h.h} depth=${h.depth} ${CT[h.colorType]}${h.plte ? ' PLTE' : ''}${h.trns ? ' tRNS' : ''}`,
    `transparent=${transparent} partial=${partial}`, ok]);
}

const filter = process.env.FILTER;
for (const [f, info, res, ok] of rows) {
  if (filter === 'cutout' && ok !== false) continue;
  const verdict = ok === false ? 'CUT-OUT' : ok === 'SKIPPED' ? 'SKIPPED' : 'opaque';
  console.log(`${f.split(/[\\/]/).pop().padEnd(34)} ${info.padEnd(34)} ${res.padEnd(28)} ${verdict}`);
}
console.log(`\n${cutout} cut-out, ${opaque} fully opaque, ${skipped} unreadable, ${rows.length} total`);
process.exit(cutout || skipped ? 1 : 0);
