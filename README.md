# ViaBedrock

ViaVersion addon to add support for Minecraft: Bedrock Edition servers.

ViaBedrock aims to be as compatible and accurate as possible with the Minecraft: Bedrock Edition protocol.

## Usage

**ViaBedrock is in very early stages of development and NOT intended for regular use yet.**

**Do not report any bugs yet. There are still a lot of things which are not implemented yet.**

If you want to talk about ViaBedrock or learn more about it you can join my [Discord](https://raphimc.net/discord).

### Standalone proxy (Serverside / Clientside)

To use ViaBedrock independently of any server or client software, you can download the latest [ViaProxy dev build](https://build.lenni0451.net/job/ViaProxy/) (Click on the **ViaProxy-x.x.x.jar**
file).

### Fabric mod (Clientside)

For this fork, install Fabric Loader for Minecraft **26.3** with Java **25**, then put the `viabedrock-standalone-*.jar` from the [Build Standalone Bedrock Fabric Mod workflow](https://github.com/Aiden-Boyd/ViaBedrock/actions/workflows/build-vfp-bedrock-test-addon.yml) into your mods folder. No separate ViaVersion, ViaFabricPlus, or Bedrock addon jar is required. Replace the previous custom Bedrock addon when updating.

The jar includes the ViaVersion protocol engine, this fork's ViaBedrock code, required Bedrock authentication/transports, and selected client bridge components. It does not include the ViaFabricPlus mod, API, menus, ViaBackwards, ViaLegacy, or ViaAprilFools. Java servers use Minecraft's native pipeline. Choose Java or Bedrock on the Add Server and Direct Connect screens; the Bedrock button on Multiplayer opens Microsoft account settings. Friends and Realms appear in the native lists. Visitor and custom interaction permissions follow the server's abilities.

CI checks the embedded translation classes and launches the packaged mod in an empty game directory with external engine libraries and development mod outputs removed. Authenticated server/Realm gameplay still needs live QA.

## Features

Here is an overview of the current and planned features in ViaBedrock.

- [x] Pinging
- [x] Joining
- [x] Xbox Live Auth
- [x] Chat / Commands
- [x] Chunks
- [x] Chunk caching
- [x] Block updates
- [x] Block entities
- [x] Biomes
- [x] Player spawning
- [x] Entity spawning
- [x] Entity interactions
- [x] Basic entity metadata translation
- [x] Entity attributes
- [x] Basic entity mounting
- [x] Player abilities
- [x] Movement
- [ ] Client-Authoritative Inventory
- [ ] Server-Authoritative Inventory
- [x] Basic item data translation
- [ ] Block breaking
- [x] Basic block placing
- [x] Basic item use
- [x] Respawning and dimension switching
- [x] Form GUIs
- [x] Scoreboard
- [x] Titles
- [x] Bossbar
- [x] Player list
- [x] Command suggestions
- [x] Sounds (No mob sounds yet)
- [x] Particles
- [x] Basic resource pack conversion (Contributions are welcome)

## Optional clientside mods

Below is a list of mods which can be used in combination with ViaBedrock to enhance certain aspects, which would not be possible without client modification:

- [ViaBedrockUtility](https://github.com/Oryxel/ViaBedrockUtility): Adds support for some custom player skins and improves custom entity rendering
- [BedrockSkinUtility](https://github.com/Camotoy/BedrockSkinUtility): Adds support for some custom player skins

## Useful resources

ViaBedrock would not have been possible without the following projects:

- [ViaVersion](https://github.com/ViaVersion/ViaVersion): Provides the base for translating packets
- [CloudburstMC Protocol](https://github.com/CloudburstMC/Protocol): Documentation of the Bedrock Edition protocol
- [PMMP BedrockProtocol](https://github.com/pmmp/BedrockProtocol): Documentation of the Bedrock Edition protocol
- [Mojang Protocol Docs](https://github.com/Mojang/bedrock-protocol-docs): Documentation of the Bedrock Edition protocol
- [CloudburstMC Protocol Docs](https://github.com/CloudburstMC/protocol-docs): Documentation of the Bedrock Edition protocol
- [wiki.vg](https://minecraft.wiki/w/Minecraft_Wiki:Projects/wiki.vg_merge/Bedrock_Protocol): Documentation of the Bedrock Edition protocol
- [mcrputil](https://github.com/valaphee/mcrputil): Documentation of Bedrock Edition resource pack encryption
- [wiki.bedrock.dev](https://wiki.bedrock.dev): Documentation of various technical aspects of Bedrock Edition

Additionally ViaBedrock uses assets and data dumps from other projects: See the `Data Asset Sources.md` file for more information.


### Building the client adapter

The complete client sources are in `addon-overlay`; selected bridge components retain their original GPL notices. See `THIRD_PARTY_NOTICES.md` for provenance. The workflow publishes this core to Maven Local, checks out the pinned upstream addon build scaffolding, and runs `tools/addon/apply-menu-overlay.py` from that checkout before `./gradlew clean build`. It then tests both a development client and the actual packaged jar in an empty game directory. No ViaFabricPlus mod is required for the build or runtime.
