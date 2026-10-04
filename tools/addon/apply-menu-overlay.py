from pathlib import Path
import json
import shutil

source = Path(__file__).resolve().parents[2] / "addon-overlay"
target = Path.cwd()
for path in source.rglob("*"):
    if path.is_file():
        destination = target / path.relative_to(source)
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, destination)
lang = target / "src/main/resources/assets/viafabricplus-bedrock/lang/en_us.json"
translations = json.loads(lang.read_text())
translations.update(json.loads((source.parent / "tools/addon/menu-translations.json").read_text()))
lang.write_text(json.dumps(translations, indent=2, ensure_ascii=False) + "\n")


import runpy
runpy.run_path(str(Path(__file__).with_name("apply-client-components.py")), run_name="__main__")
