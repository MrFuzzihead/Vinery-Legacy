plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

// The 1.21 model/blockstate/data JSONs are kept in the repo as the reference for the
// port (see BACKPORT_PLAN.md §5) but must not ship in the 1.7.10 jar: 1.7.10 has no
// JSON model system, no blockstates and no datapacks, so they are dead weight (~2.5 MB).
// Textures, lang and sounds ARE shipped - 1.7.10 uses all three.
tasks.jar {
    exclude("assets/vinery/blockstates/**")
    exclude("assets/vinery/models/**")
    exclude("data/**")
}