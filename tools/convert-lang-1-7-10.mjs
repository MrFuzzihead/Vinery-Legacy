#!/usr/bin/env node
/**
 * One-shot converter for the 1.21 lang files to 1.7.10 conventions.
 *
 * Minecraft 1.7.10 resolves translation keys differently and locates lang files by an uppercase
 * locale code, so a straight copy of the modern files is silently ignored at runtime:
 *
 *   - file names   en_us.json -> en_US.json   (1.7.10 matches the code case-sensitively)
 *   - block keys   block.vinery.x    -> tile.vinery.x.name
 *   - item keys    item.vinery.x     -> item.vinery.x.name
 *   - entity keys  entity.vinery.x   -> entity.vinery.x.name
 *   - dropped      advancement.*, subtitles.*, rei.* (1.21-only features with no 1.7.10
 *                  equivalent: advancements do not exist, subtitles are per-sound JSON, and there
 *                  is no REI on 1.7.10)
 *
 * Keys already in 1.7.10 form pass through untouched, so the script is safe to re-run.
 *
 * Usage: node tools/convert-lang-1-7-10.mjs [--dry-run]
 */
import { readdirSync, readFileSync, writeFileSync, renameSync } from 'node:fs';
import { join, dirname, basename } from 'node:path';
import { fileURLToPath } from 'node:url';

const LANG_DIR = join(dirname(fileURLToPath(import.meta.url)), '..', 'src', 'main', 'resources',
    'assets', 'vinery', 'lang');

const DRY_RUN = process.argv.includes('--dry-run');

/** Prefixes that have no 1.7.10 counterpart and are dropped rather than translated. */
const DROP_PREFIXES = ['advancement.', 'subtitles.', 'rei.', 'biome.'];

function toUppercaseLocale(fileName) {
    const ext = '.json';
    const base = basename(fileName, ext);
    const [lang, region] = base.split('_');
    return `${lang}_${region.toUpperCase()}${ext}`;
}

function convertKey(key) {
    if (DROP_PREFIXES.some((prefix) => key.startsWith(prefix))) return null;
    // block.<ns>.<path> -> tile.<ns>.<path>.name
    if (key.startsWith('block.')) return `tile.${key.slice('block.'.length)}.name`;
    // item.<ns>.<path> -> item.<ns>.<path>.name, but only when the key has no .name suffix yet
    if (key.startsWith('item.') && !key.endsWith('.name')) {
        return `${key}.name`;
    }
    return key;
}

function sortKeys(obj) {
    return Object.keys(obj).sort().reduce((acc, key) => ({ ...acc, [key]: obj[key] }), {});
}

let filesChanged = 0;
let keysConverted = 0;
let keysDropped = 0;
let renamed = 0;

for (const fileName of readdirSync(LANG_DIR).filter((f) => f.endsWith('.json'))) {
    const path = join(LANG_DIR, fileName);
    const original = JSON.parse(readFileSync(path, 'utf8'));

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

    const newFileName = toUppercaseLocale(fileName);
    const target = join(LANG_DIR, newFileName);

    // Compare against a real directory listing rather than existsSync(): on case-insensitive
    // filesystems existsSync('en_US.json') is already true for 'en_us.json', which would make
    // every rename look like a collision with itself.
    const collision = readdirSync(LANG_DIR).includes(newFileName);
    if (newFileName !== fileName && collision) {
        console.warn(`! ${newFileName} already exists, leaving ${fileName} untouched`);
        continue;
    }

    if (DRY_RUN) {
        console.log(`[dry-run] ${fileName} -> ${newFileName} (${Object.keys(converted).length} keys)`);
        continue;
    }

    // Rename before writing so the old lowercase name does not linger.
    if (target !== path) {
        renameSync(path, target);
        renamed++;
    }
    writeFileSync(target, JSON.stringify(sortKeys(converted), null, 2) + '\n');
    filesChanged++;
}

console.log(
    `${DRY_RUN ? '[dry-run] ' : ''}${filesChanged} lang file(s) written, ${renamed} renamed, ` +
    `${keysConverted} key(s) converted, ${keysDropped} key(s) dropped`);