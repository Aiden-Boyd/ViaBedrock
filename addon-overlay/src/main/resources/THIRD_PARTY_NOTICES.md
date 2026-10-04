# Included components and source

This fork is licensed under GPL-3.0-or-later. It includes modified client components from ViaFabricPlus (GPL-3.0-or-later), revision 9913d32266be9243ee31fc84539913ae79ae6aa2, and Bedrock integration sources/build scaffolding from ViaVersionAddons/viafabricplus-bedrock (GPL-3.0-or-later), revision 1268aa42bf0f96c63058906e07a9cd05534565af. Original author and license headers are retained in each source file.

Changes remove the ViaFabricPlus mod/API/UI and legacy engines, retain the modern connection bridge, restyle the shared widgets, and add Bedrock account settings and per-server Java/Bedrock selection. The full modified component sources are in addon-overlay/src/main/java. The pinned scaffolding and exact build commands are in .github/workflows/build-vfp-bedrock-test-addon.yml.

The internal protocol engine is com.viaversion:viaversion-common:5.12.0 (GPL-3.0-or-later), from https://github.com/ViaVersion/ViaVersion/tree/5.12.0 . Its source and build instructions are available there. Authentication and transport libraries retain their upstream notices in the nested jars; dependency coordinates and exclusions are specified in addon-overlay/build.gradle.kts and the pinned scaffolding's version catalog.

This is an independent fork; the upstream authors do not endorse its changes. Corresponding fork source and build instructions: https://github.com/Aiden-Boyd/ViaBedrock/tree/integration/inventory-current . The GPL text is in LICENSE.
