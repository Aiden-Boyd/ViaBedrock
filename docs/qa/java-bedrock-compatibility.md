# Java–Bedrock compatibility audit

Target: Minecraft Java 26.3 and the Bedrock protocol supported by this branch.
This is an engineering coverage audit, not a statement of complete gameplay parity.
Offline regression tests and packaged-client smoke tests cannot replace authenticated
Bedrock Dedicated Server, Realm and peer-hosted gameplay testing.

## Changes in this pass

- Handle server input permission updates; filter movement, directional movement,
  jumping, sneaking, sprinting and dismounting. Apply key and camera restrictions
  in the standalone client. Revoke held inputs when a permission changes.
- Reconcile item stack response names and durability even when stack counts remain
  unchanged. Preserve other metadata and use the server's filtered name.
- Serialize anvil text updates with inventory operations. Implement rename-only
  stacked items and shift-click destination planning through the shared craft path.
  Repairing and combining enchantments are separate, incomplete workflows.
- Acknowledge accepted Java use-item prediction sequences and send mining stop
  actions with their actual block position and face.
- Apply movement correction velocity instead of preserving the client's previous
  velocity. Accept zero-tick and older-than-history corrections, reject duplicate,
  stale and future numbered corrections, and reject nonfinite values.
- Translate leaving a bed to the Bedrock stop-sleeping action. Horse commands no
  longer throw an exception; horse jumping and inventory remain unsupported.

## Coverage and remaining work

| Mechanic | Client and server boundary to verify | Current status / release QA |
| --- | --- | --- |
| Inventory clicks | Cursor, slot mappings, stack IDs, request ordering, rollback | Existing translation; regression coverage for reconciliation. Live test pickup, split, shift-click, double-click, drag, creative and rejection under latency. |
| 2×2 and crafting table | Recipe selection, consumption, output destination, repeated crafts | Existing craft planner; live test mixed ingredients, full inventory, repeated shift-crafts and recipe changes while a request is pending. |
| Workstations | Input acceptance, server recipe, output, experience and metadata | Existing furnace, stonecutter, enchanting and smithing paths need live matrices. Anvil rename improved; repair/enchantment combining incomplete. Grindstone transaction workflow needs implementation. |
| Loom, cartography, trading | Window type, slot maps, selected recipe, authoritative costs | Confirmed unsupported container types; cannot claim parity. |
| Offhand and item use | Hand, use duration, stop-use, food, shields, bows, charging | Offhand handling incomplete; start-use auth input has a TODO. Live test food, bows, crossbows, shields, potions, buckets and held-item changes. |
| Mining and placement | Prediction sequence, permissions, break progress, cancellation, drops | Stop coordinates and accepted use acknowledgements corrected. Live test instant break, tools, water, fire, block replacement, denied interactions and latency. |
| Doors and double chests | Linked block states, container size, updates after close | Existing translations; live test orientation, powered doors, obstruction, joins and chunk boundaries. |
| Permissions | Visitor, custom abilities, input locks and live revocation | Server authority retained; new input locks implemented. Live test forbidden placement, breaking, interaction, attacks, camera, movement and dismount, then restore permissions. |
| Player movement | Server corrections, velocity, teleport acknowledgements and auth input | Focused regressions; full Bedrock rewind/replay is absent. Special-block friction, fluid physics and effect interactions remain approximate. |
| Swimming, crawling, gliding | Pose, dimensions, legal transitions and movement speed | Existing partial adapter logic; verify water entry/exit, low ceilings, elytra durability, flight revocation and collision under latency. |
| Boats | Driver/passenger roles, paddles, correction and dismount | Existing boat translation and correction tests; live test two passengers, seat switching, collisions, ice, water, destruction and reconnect. |
| Other mounts | Jump charge, steering, inventory, seats and movement authority | Horse jumping/inventory unsupported; ordinary commands now safely ignored rather than throwing. Test pigs, striders, camels, horses and other rideable entities separately. |
| Combat | Attack permissions, cooldown differences, use poses, knockback and equipment | Bedrock server decides damage; live test melee, projectiles, shield response, equipment changes and mob bow animations. |
| Death and dimensions | Respawn ordering, loading acknowledgements, passengers and inventory | Existing paths; live test death with pending clicks, portals, end return, teleport during mounting and reconnect. |
| Block entities and redstone | Server-owned simulation plus Java block/entity display | Server owns simulation. Verify state/rendering for piston motion, waterlogging, signs, lecterns, containers and custom states. Java-exclusive redstone behavior cannot be imposed on the server. |
| Recipes and item metadata | Names, damage, enchantments, trim, repair cost and recipe IDs | Metadata reconciliation improved; MULTI recipes are discarded and recipe-book integration has TODOs. Verify metadata survives every transaction. |
| Resource packs and custom content | Pack negotiation, item/block mappings and render assets | Partial translation; no general guarantee for arbitrary server packs, custom models or scripts. |
| Cameras, fog, HUD and emotes | Bedrock presentation packets to client behavior | Several packet families are explicitly cancelled. Camera input lock implemented; full camera/presentation support absent. |
| Commands and forms | Suggestions, typed arguments, responses and cancellation | Partial translation; command templates and some form layouts need dedicated work. |
| Friends, servers and Realms | Authentication, discovery, expiry, permissions, join and invite codes | Native listing integration passes offline menu smoke tests. Authenticated joins and lifecycle changes require live QA. |

## Validation policy

Run the core build (compilation, JUnit, codec fixtures and Checkstyle) and the standalone
build (development menus, packaged launch without external ViaVersion/ViaFabricPlus,
native integration checks and embedded-core verification) for the final commit.
Inspect the delivered jar, not only development classes. Record live gameplay separately;
never count a missing test server or account as a passing test.

Upstream changes remain relevant: this fork must be rebased and retested against protocol,
Minecraft, authentication and resource-pack changes. This audit is intentionally finite
and does not assert that every possible edition difference has been enumerated.
