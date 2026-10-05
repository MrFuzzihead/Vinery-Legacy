// One-off: forward -D flags from the Gradle invocation into the dev client, so diagnostics like
// -Dvinery.vertexDiag can be switched on without editing code.
const fs = require('fs');
const p = 'build.gradle.kts';
let s = fs.readFileSync(p, 'utf8');
if (s.includes('vinery.forwardClientProps')) { console.log('already present'); process.exit(0); }

const anchor = 'usesMixins = true';
const block = `usesMixins = true

// Forward -Dvinery.* flags into the dev client, for the render diagnostics in
// tools/ (for example -Dvinery.noCull=true or -Dvinery.vertexDiag=true).
tasks.withType<JavaExec>().configureEach {
    val forwarded = providers.systemPropertiesPrefixedBy("vinery.").get()
    if (forwarded.isNotEmpty()) {
        jvmArgs(forwarded.map { "-D$it" })
    }
}
`;
if (!s.includes(anchor)) { console.error('ANCHOR MISSING'); process.exit(1); }
s = s.replace(anchor, block);
fs.writeFileSync(p, s);
console.log('gradle forwarding added');