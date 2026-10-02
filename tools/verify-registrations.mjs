#!/usr/bin/env node
/**
 * Static consistency checks for the 1.7.10 port.
 *
 * A dedicated server loads classes, registries and NBT but never textures or lang files, so a
 * green `runServer` says nothing about whether `setBlockTextureName("vinery:dark_cherry_planks")`
 * points at a file that exists. Minecraft 1.7.10 also fails silently in both cases: a missing
 * texture renders as the missing-texture placeholder with no log line, and a missing translation
 * key renders as the raw key. This script is the substitute for that feedback loop.
 *
 * Checks, for every block/item registered through VineryRegistry:
 *   1. the `vinery:<path>` texture name passed to setBlockTextureName / setTextureName resolves to
 *      an actual PNG under assets/vinery/textures/
 *   2. a matching translation key exists in en_US.json (tile.* for blocks, item.* for items)
 *   3. registry names are free of colons (FML rejects namespaced names on 1.7.10)
 *   4. the texture file is readable and non-empty
 *
 * Usage: node tools/verify-registrations.mjs
 * Exits non-zero if anything fails, so it can be wired into CI later.
 */
import { readdirSync, readFileSync, statSync, existsSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const SRC = join(ROOT, 'src', 'main', 'java');
const ASSETS = join(ROOT, 'src', 'main', 'resources', 'assets', 'vinery');
const LANG = join(ASSETS, 'lang', 'en_US.json');

const lang = JSON.parse(readFileSync(LANG, 'utf8'));
const problems = [];
const seen = { blocks: 0, items: 0, itemBlockItems: 0 };

function javaFilesIn(dir, acc = []) {
    for (const entry of readdirSync(dir, { withFileTypes: true })) {
        const path = join(dir, entry.name);
        if (entry.isDirectory()) javaFilesIn(path, acc);
        else if (entry.name.endsWith('.java')) acc.push(path);
    }
    return acc;
}

/**
 * Collects every texture reference a class makes.
 *
 * 1.7.10 offers three ways to name a texture, and all three have to be understood:
 *   - Block.setBlockTextureName("vinery:x")  — the block-icon path
 *   - IIconRegister.registerIcon("vinery:x") — used by pillar-style blocks with distinct faces
 *   - Item.setTextureName("vinery:x")        — item icons
 * An item extending ItemBlock (including ItemSlab) has no texture of its own: it borrows the
 * block's icon, so those classes legitimately declare nothing.
 */
function collectTextureNames() {
    const map = new Map();
    for (const file of javaFilesIn(SRC)) {
        const text = readFileSync(file, 'utf8');
        const className = (text.match(/class\s+(\w+)/) || [])[1];
        if (!className) continue;
        const icons = [
            ...[...text.matchAll(/set(?:Block)?TextureName\(\s*"([^"]+)"/g)].map((m) => m[1]),
            ...[...text.matchAll(/registerIcon\(\s*"([^"]+)"/g)].map((m) => m[1]),
        ];
        map.set(className, { icons, extendsItemBlock: /extends\s+(?:\w+\.)?Item(?:Block|Slab)\b/.test(text) });
    }
    return map;
}

/** Registry calls look like VineryRegistry.block(new FooBlock(...), "name") / .item(new X(), "name"). */
function collectRegistrations() {
    const blocks = [];
    const items = [];
    const blockFiles = javaFilesIn(join(SRC, 'com', 'mrfuzzihead', 'vinery'));

    // A `null` ItemBlock class means the block has no item form (slabs), so it never needs a key.
    const blockNoItem = /VineryRegistry\.block\(\s*new\s+(\w+)\([^;]*?\)[^;]*?,\s*null,\s*"([\w_]+)"/gs;
    const blockWithItem = /VineryRegistry\.block\(\s*new\s+(\w+)\([^;]*?\),\s*"([\w_]+)"/gs;
    const itemPattern = /VineryRegistry\.item\(\s*new\s+(\w+)\([^;]*?\),\s*"([\w_]+)"/gs;

    for (const file of blockFiles) {
        const text = readFileSync(file, 'utf8');
        if (!text.includes('VineryRegistry.')) continue;
        for (const m of text.matchAll(blockNoItem)) blocks.push({ className: m[1], name: m[2], hasItem: false });
        for (const m of text.matchAll(blockWithItem)) blocks.push({ className: m[1], name: m[2], hasItem: true });
        for (const m of text.matchAll(itemPattern)) items.push({ className: m[1], name: m[2], file });
    }
    return { blocks, items };
}

const textureNames = collectTextureNames();
const { blocks, items } = collectRegistrations();

/**
 * Vanilla item types that are ItemBlock subclasses and therefore borrow their icon and translation
 * key from the block they were registered against. They have no source file of their own here.
 */
const VANILLA_ITEM_BLOCK_TYPES = new Set(['ItemBlock', 'ItemSlab', 'ItemCloth', 'ItemMultiTexture']);

function checkTexture(className, icon) {
    if (!icon.includes(':')) {
        problems.push(`${className}: texture name "${icon}" has no domain — 1.7.10 needs "vinery:<path>"`);
        return;
    }
    const [, path] = icon.split(':');
    const png = join(ASSETS, 'textures', 'blocks', `${path}.png`);
    if (!existsSync(png)) {
        problems.push(`${className}: texture "${icon}" -> ${png.replace(ROOT, '.')} DOES NOT EXIST`);
    } else if (statSync(png).size === 0) {
        problems.push(`${className}: texture "${icon}" is empty`);
    }
}

function checkLang(key, what, name) {
    if (!Object.prototype.hasOwnProperty.call(lang, key)) {
        problems.push(`${what} "${name}": missing translation key ${key}`);
    }
}

for (const { className, name, hasItem } of blocks) {
    seen.blocks++;
    if (name.includes(':')) problems.push(`block "${name}" contains a colon — illegal on 1.7.10`);
    const info = textureNames.get(className);
    if (!info || info.icons.length === 0) {
        problems.push(`block "${name}" (${className}) declares no texture name`);
    } else {
        for (const icon of info.icons) checkTexture(className, icon);
    }
    // A block with no item form (the double slab) can never show up in an inventory, so 1.7.10
    // gives it no translation key either — vanilla's double_stone_slab has none.
    if (hasItem) checkLang(`tile.vinery.${name}.name`, 'block', name);
}

for (const { className, name } of items) {
    seen.items++;
    if (name.includes(':')) problems.push(`item "${name}" contains a colon — illegal on 1.7.10`);
    const info = textureNames.get(className);
    if (!info) {
        if (VANILLA_ITEM_BLOCK_TYPES.has(className)) {
            // Borrows the block's icon and the tile.* key already verified against that block.
            seen.itemBlockItems++;
            continue;
        }
        problems.push(`item "${name}" (${className}) not found in sources`);
        continue;
    }
    if (info.extendsItemBlock) {
        // Same thing, but for a Vinery class that extends ItemBlock/ItemSlab.
        seen.itemBlockItems++;
        continue;
    }
    if (info.icons.length === 0) {
        problems.push(`item "${name}" (${className}) declares no texture name`);
    } else {
        for (const icon of info.icons) checkTexture(className, icon);
    }
    checkLang(`item.vinery.${name}.name`, 'item', name);
}

console.log(
    `checked ${seen.blocks} block(s), ${seen.items} explicit item(s)` +
    `${seen.itemBlockItems ? ` and ${seen.itemBlockItems} ItemBlock-derived item(s) (icon from block)` : ''}` +
    ` against ${LANG.replace(ROOT, '.')}`);
if (problems.length === 0) {
    console.log('all registrations have a resolvable texture and translation key  OK');
} else {
    console.error(`\n${problems.length} problem(s):`);
    for (const problem of problems) console.error(`  - ${problem}`);
    process.exitCode = 1;
}