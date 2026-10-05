// One-off: shade each face independently, the way renderStandardBlock does.
//
// The quad path set one colour for the whole box, so all six faces received identical light and the
// chair rendered as a flat silhouette. Vanilla varies brightness per face -- top brightest, north and
// south intermediate, east and west darkest, bottom darkest of all -- which is most of what gives a
// block its sense of solidity. The factors below are the ones RenderBlocks applies.
const fs = require('fs');
const p = 'src/main/java/com/mrfuzzihead/vinery/client/render/QuadRenderer.java';
let s = fs.readFileSync(p, 'utf8');

if (s.includes('FACE_SHADE')) { console.log('already present'); process.exit(0); }

const oldBrightness = '        int packedBrightness = block.getMixedBrightnessForBlock(world, x, y, z);\n'
  + '        float ao = block.getAmbientOcclusionLightValue();';
const newBrightness = '        int packedBrightness = block.getMixedBrightnessForBlock(world, x, y, z);\n'
  + '        float ao = block.getAmbientOcclusionLightValue();\n'
  + '\n'
  + '        // Per-face brightness, in the order down, up, north, south, west, east. RenderBlocks\n'
  + '        // shades a cube\'s faces differently and that variation is most of what makes it read as a\n'
  + '        // solid object rather than a flat cut-out; setting one colour for the whole box flattened\n'
  + '        // the chair into a silhouette.\n'
  + '        final float[] shade = { 0.5F, 1.0F, 0.8F, 0.8F, 0.6F, 0.6F };';

if (!s.includes(oldBrightness)) { console.error('ANCHOR MISSING: brightness'); process.exit(1); }
s = s.replace(oldBrightness, newBrightness);

const oldSet = '            tess.setBrightness(packedBrightness);\n'
  + '            // White, not getAmbientOcclusionLightValue(). That value is the per-vertex smoothing\n'
  + '            // factor -- 0.2 for a non-opaque block -- and using it as the colour multiplier drew\n'
  + '            // every quad at 20% brightness. The equivalent of vanilla\'s non-AO path.\n'
  + '            tess.setColorOpaque_F(1.0F, 1.0F, 1.0F);';

const newSet = '            // setBrightness takes the packed sky/block value and unpacks and scales it itself.\n'
  + '            // White times this face\'s shade, NOT getAmbientOcclusionLightValue(): that value is the\n'
  + '            // per-vertex smoothing factor -- 0.2 for a non-opaque block -- and using it here drew\n'
  + '            // every quad at 20% brightness.\n'
  + '            tess.setBrightness(packedBrightness);\n'
  + '            float faceShade = shade[face];\n'
  + '            tess.setColorOpaque_F(faceShade, faceShade, faceShade);';

if (!s.includes(oldSet)) { console.error('ANCHOR MISSING: set'); process.exit(1); }
s = s.replace(oldSet, newSet);

// Drop the now-duplicated prose above the old setBrightness call.
s = s.replace(/            \/\/ Shading is delegated to the Tessellator exactly as RenderBlocks does it:\n            \/\/ setBrightness takes the packed sky\/block value and unpacks and scales it itself,\n            \/\/ and the colour applied is the ambient-occlusion factor on all three channels\.\n            \/\/\n            \/\/ Hand-rolling this is what made the chair invisible: an earlier version read sky\n            \/\/ light from the wrong bit of the packed value and never scaled 0-15 up to 0-255, so\n            \/\/ every face came out at RGB 15, which is effectively black\.\n/, '');

fs.writeFileSync(p, s);
console.log('per-face shading added');