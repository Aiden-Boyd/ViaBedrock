import os
import signal
import subprocess
import sys
from pathlib import Path

# Bypass vanilla first-launch onboarding only in this disposable CI game directory.
game_dir = Path("run")
game_dir.mkdir(exist_ok=True)
(game_dir / "options.txt").write_text(
    "onboardAccessibility:false\nskipMultiplayerWarning:true\ntutorialStep:none\nmaxFps:30\n"
)

process = subprocess.Popen(
    ["./gradlew", "runClient", "--stacktrace"],
    stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, start_new_session=True,
    env={**os.environ, "VIA_BEDROCK_MENU_SMOKE": "true"},
)
try:
    output, _ = process.communicate(timeout=240)
except subprocess.TimeoutExpired:
    os.killpg(process.pid, signal.SIGTERM)
    try:
        output, _ = process.communicate(timeout=10)
    except subprocess.TimeoutExpired:
        os.killpg(process.pid, signal.SIGKILL)
        output, _ = process.communicate()
    print(output[-24000:])
    raise SystemExit("Minecraft menu smoke test timed out")
print(output[-24000:])
if process.returncode != 0 or "BEDROCK_NATIVE_MENU_SMOKE_PASSED" not in output:
    log = game_dir / "logs/latest.log"
    if log.exists():
        print("Minecraft runtime log on smoke failure:")
        print(log.read_text(errors="replace")[-40000:])
    for index, line in enumerate(output.splitlines()):
        if any(marker in line for marker in ("Caused by:", "AssertionError", "Mixin apply failed", "InjectionError")):
            print("\\n".join(output.splitlines()[index:index + 18]))
    raise SystemExit("Minecraft did not pass the native menu smoke test")
print("Opened both native menus and verified Bedrock rows without Xbox sign-in")
