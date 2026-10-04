import os
import subprocess
import zipfile
from pathlib import Path

root = Path(os.environ.get("GRADLE_USER_HOME", str(Path.home() / ".gradle")))
target = "net/minecraft/client/gui/screens/multiplayer/ServerSelectionList.class"
for jar in root.rglob("*.jar"):
    try:
        with zipfile.ZipFile(jar) as archive:
            names = archive.namelist()
            if target not in names:
                continue
            selected = [name[:-6].replace("/", ".") for name in names if name.endswith(".class") and (
                name.startswith("com/mojang/realmsclient/gui/screens/RealmsMainScreen")
                or name.startswith("com/mojang/realmsclient/dto/RealmsServer")
                or name.startswith("net/minecraft/client/gui/screens/multiplayer/ServerSelectionList")
                or name in [
                    "net/minecraft/client/gui/screens/multiplayer/JoinMultiplayerScreen.class",
                    "net/minecraft/client/multiplayer/ServerData.class",
                    "net/minecraft/client/multiplayer/ServerList.class",
                ])]
        print("Minecraft menu jar:", jar, flush=True)
        subprocess.run(["javap", "-classpath", str(jar), "-private", *selected], check=True)
        raise SystemExit(0)
    except zipfile.BadZipFile:
        continue
raise SystemExit("Minecraft menu classes not found")
