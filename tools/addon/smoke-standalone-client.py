"""Launch the shipped jar without development mod outputs or external engines."""
import io
import json
import os
from pathlib import Path
import subprocess
import zipfile

root = Path.cwd()
jar = root / "build/libs/viafabricplus-bedrock-1.1.1-SNAPSHOT.jar"
owned_classes = set()
mod_ids = set()

def inspect(data):
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        owned_classes.update(n for n in archive.namelist() if n.endswith(".class"))
        metadata = json.loads(archive.read("fabric.mod.json"))
        mod_ids.add(metadata["id"])
        for nested in metadata.get("jars", []):
            inspect(archive.read(nested["file"]))

inspect(jar.read_bytes())
required = {
    "com/viaversion/viaversion/api/Via.class",
    "com/viaversion/viafabricplus/bedrock/client/BedrockClient.class",
    "net/raphimc/viabedrock/protocol/BedrockProtocol.class",
    "net/lenni0451/commons/httpclient/HttpClient.class",
    "io/jsonwebtoken/Jwts.class",
    "io/jsonwebtoken/impl/DefaultJwtParserBuilder.class",
    "io/jsonwebtoken/gson/io/GsonSerializer.class",
}
assert required <= owned_classes, f"Missing bundled translation classes: {required - owned_classes}"
assert "viafabricplus-bedrock" in mod_ids
assert not {"viafabricplus", "viafabricplus-api"} & mod_ids
for name in owned_classes:
    assert not name.startswith(("com/viaversion/viafabricplus/screen/", "com/viaversion/viafabricplus/api/", "com/viaversion/viabackwards/", "net/raphimc/vialegacy/", "com/viaversion/viaaprilfools/")), name
print("Bundled modules:", ", ".join(sorted(mod_ids)), flush=True)

export = subprocess.run(
    ["./gradlew", "runClient", "--stacktrace"], capture_output=True, text=True,
    env={**os.environ, "VIA_BEDROCK_STANDALONE_SMOKE": "true"},
)
launch_file = root / "build/standalone-launch.json"
if not launch_file.exists() or "STANDALONE_LAUNCH_EXPORTED" not in export.stdout + export.stderr:
    print(export.stdout[-12000:] + export.stderr[-12000:])
    raise SystemExit("Could not export the Minecraft launch configuration")
launch = json.loads(launch_file.read_text())
# Use an allowlist: unbundled mod dependencies must never leak from Gradle.
vanilla = {str(Path(p).resolve()) for p in launch["vanillaLibraries"]}
classpath = []
removed = []
for entry in launch["classpath"]:
    path = Path(entry)
    allowed = str(path.resolve()) in vanilla
    if zipfile.is_zipfile(path):
        with zipfile.ZipFile(path) as archive:
            names = set(archive.namelist())
            allowed |= "net/minecraft/client/main/Main.class" in names
            allowed |= "net/fabricmc/loader/impl/launch/knot/KnotClient.class" in names
    (classpath if allowed else removed).append(entry)
assert not any("viafabricplus-5.1.0" in Path(p).name for p in launch["classpath"]), "ViaFabricPlus leaked into development dependencies"
print("Removed all non-vanilla development libraries and outputs:", len(removed), flush=True)
print("Production classpath:", [Path(p).name for p in classpath], flush=True)

# Deliberately avoid Loom's dev launcher and its extra mod/classpath properties.
jvm = [arg for arg in launch["jvmArgs"] if not arg.startswith(("-Dfabric.", "-Dloader."))]
jvm += [
    "-Dfabric.development=false",
    "-Dfabric.addMods=" + str(jar),
    "-DviaBedrock.menuSmoke=true",
]
game_dir = root / "run-standalone"
game_dir.mkdir(exist_ok=True)
assert not (game_dir / "mods").exists() or not list((game_dir / "mods").iterdir())
(game_dir / "options.txt").write_text(
    "onboardAccessibility:false\nskipMultiplayerWarning:true\ntutorialStep:none\nmaxFps:30\n"
)
args = launch["args"]
for index, value in enumerate(args[:-1]):
    if value == "--gameDir":
        args[index + 1] = str(game_dir)
if "--gameDir" not in args:
    args += ["--gameDir", str(game_dir)]
command = [launch["java"], *jvm, "-cp", os.pathsep.join(classpath),
           "net.fabricmc.loader.impl.launch.knot.KnotClient", *args]
process = subprocess.run(command, capture_output=True, text=True, timeout=240)
output = process.stdout + process.stderr
print(output[-24000:])
if process.returncode or any(marker not in output for marker in (
    "BEDROCK_NATIVE_MENU_SMOKE_PASSED", "BEDROCK_AUTH_RUNTIME_SMOKE_PASSED",
)):
    raise SystemExit("Packaged standalone mod did not pass the Minecraft menu smoke test")
print("BEDROCK_STANDALONE_PACKAGE_SMOKE_PASSED")


assert "BEDROCK_COMPONENT_PIPELINE_SMOKE_PASSED" in output, "Standalone protocol pipeline test did not run"
