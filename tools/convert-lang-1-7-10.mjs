#!/usr/bin/env node
/**
 * Converts the 1.21 lang files to the format 1.7.10 actually reads.
 *
 * Two things are wrong with a straight copy of the modern files, and both fail silently:
 *
 *   1. File extension. Forge 1.7.10 scans jar entries with
 *      `assets/(.*)/lang/([\w_-]+).lang`, so only **.lang** files are loaded. An en_US.json
 *      sitting in the jar is ignored and every name renders as its raw key.
 *   2. Key naming. 1.7.10 resolves block names as `tile.<unlocalizedName>.name`, item names as
 *      `item.<unlocalizedName>.name`, whereas 1.21 uses `block.`/`item.` without the suffix.
 *      Locale codes are also matched case-sensitively: en_us.json -> en_US.lang.
 *
 * Keys whose feature does not exist on 1.7.10 are dropped rather than translated: advancements,
 * subtitles and REI have no counterpart. Re-running is safe — .lang input is parsed back in.
 *
 * Usage: node tools/convert-lang-1-7-10.mjs [--dry-run]
 */
import { readdirSync, readFileSync, writeFileSync, renameSync } from 'node:fs';
import { join, dirname, basename, extname } from 'node:path';
import { fileURLToPath } from 'node:url';

const LANG_DIR = join(dirname(fileURLToPath(import.meta.url)), '..', 'src', 'main', 'resources',
    'assets', 'vinery', 'lang');

const DRY_RUN = process.argv.includes('--dry-run');

/** Prefixes that have no 1.7.10 counterpart: advancements do not exist, no REI, subtitles are per-sound. */
const DROP_PREFIXES = ['advancement.', 'subtitles.', 'rei.', 'biome.'];

function targetLocale(fileName) {
    const base = basename(fileName, extname(fileName));
    const [lang, region = ''] = base.split('_');
    return region ? `${lang}_${region.toUpperCase()}` : lang;
}

function convertKey(key) {
    if (DROP_PREFIXES.some((prefix) => key.startsWith(prefix))) return null;
    if (key.startsWith('block.')) return `tile.${key.slice('block.'.length)}.name`;
    if (key.startsWith('item.') && !key.endsWith('.name')) return `${key}.name`;
    return key;
}

function parseLang(text) {
    const entries = [];
    for (const line of text.split('\n')) {
        const trimmed = line.trim();
        if (!trimmed || trimmed.startsWith('#')) continue;
        const index = trimmed.indexOf('=');
        if (index === -1) continue;
        entries.push([trimmed.slice(0, index), trimmed.slice(index + 1)]);
    }
    return Object.fromEntries(entries);
}

function toLangFile(obj) {
    return Object.keys(obj)
        .sort()
        .map((key) => `${key}=${String(obj[key]).replace(/\r?\n/g, '\\n')}`)
        .join('\n') + '\n';
}

let filesChanged = 0;
let keysConverted = 0;
let keysDropped = 0;

for (const fileName of readdirSync(LANG_DIR).filter((f) => f.endsWith('.json') || f.endsWith('.lang'))) {
    const path = join(LANG_DIR, fileName);
    const raw = readFileSync(path, 'utf8');
    const original = fileName.endsWith('.lang') ? parseLang(raw) : JSON.parse(raw);

    const converted = {};
    for (const [key, value] of Object.entries(original)) {
        const newKey = convertKey(key);
        if (newKey === null) {
            keysDropped++;
            continue;
        }
        if (newKey !== key) keysConverted++;
        converted[newKey] = value;
    }

    const newFileName = `${targetLocale(fileName)}.lang`;
    const target = join(LANG_DIR, newFileName);

    // Compare against a real listing rather than existsSync(): on case-insensitive filesystems
    // existsSync('en_US.lang') is already true for 'en_us.json', which would look like a collision.
    if (newFileName !== fileName && readdirSync(LANG_DIR).includes(newFileName)) {
        console.warn(`! ${newFileName} already exists, leaving ${fileName} untouched`);
        continue;
    }

    if (DRY_RUN) {
        console.log(`[dry-run] ${fileName} -> ${newFileName} (${Object.keys(converted).length} keys)`);
        continue;
    }

    if (target !== path) {
        renameSync(path, target);
    }
    writeFileSync(target, toLangFile(converted));
    filesChanged++;
}

console.log(`${DRY_RUN ? '[dry-run] ' : ''}${filesChanged} lang file(s) written as .lang, ` +
    `${keysConverted} key(s) converted, ${keysDropped} key(s) dropped`);