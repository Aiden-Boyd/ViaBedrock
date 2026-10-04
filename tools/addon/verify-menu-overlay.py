import json
import zipfile
from pathlib import Path

jar = Path("build/libs/viafabricplus-bedrock-1.1.1-SNAPSHOT.jar")
required = [
    "core.integration.MixinBedrockServerSelectionList",
    "core.integration.MixinBedrockFriendsInMultiplayer",
    "core.integration.MixinBedrockRealmsInRealms",
    "core.integration.MixinBedrockRealmSelectionList",
]
with zipfile.ZipFile(jar) as archive:
    config = json.loads(archive.read("viafabricplus-bedrock.mixins.json"))
    for mixin in required:
        assert mixin in config["client"], mixin
        assert (config["package"] + "." + mixin).replace(".", "/") + ".class" in archive.namelist(), mixin
    for name in ["BedrockFriendEntry", "BedrockRealmEntry", "BedrockMenuJoiner", "AccountWorldList"]:
        assert "com/viaversion/viafabricplus/bedrock/integration/" + name + ".class" in archive.namelist(), name
print("Verified native friends and Realms menu integration in the addon")
