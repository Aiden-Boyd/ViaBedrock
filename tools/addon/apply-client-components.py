from pathlib import Path
import json
root = Path("src/main/java/com/viaversion/viafabricplus/bedrock")
removed = ['injection/mixin/core/integration/MixinLimitationsImpl.java', 'injection/mixin/core/integration/MixinProtocolCategory.java', 'injection/mixin/core/integration/MixinProtocolVersionMetadata.java', 'injection/mixin/core/integration/MixinSettingsScreen.java', 'injection/mixin/core/integration/MixinVFPScreen.java', 'settings/ActionSetting.java', 'screen/settings/ActionListEntry.java', 'screen/BedrockRealmsScreen.java', 'screen/AcceptInvitationCodeScreen.java']
for name in removed:
    (root / name).unlink(missing_ok=True)
# All remaining sources are overlay-owned. Refuse accidental framework dependencies.
for source in root.rglob("*.java"):
    for line in source.read_text().splitlines():
        if line.startswith("import com.viaversion.viafabricplus.") and not line.startswith("import com.viaversion.viafabricplus.bedrock."):
            raise RuntimeError(f"Unexpected ViaFabricPlus framework dependency: {source}: {line}")
print("Applied Bedrock client components without the ViaFabricPlus mod or UI")
