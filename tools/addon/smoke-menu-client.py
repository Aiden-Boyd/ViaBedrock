import os
import signal
import subprocess
import sys

process = subprocess.Popen(
    ["./gradlew", "runClient", "--stacktrace"],
    stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, start_new_session=True,
    env={**os.environ, "VIA_BEDROCK_MENU_SMOKE": "true"},
)
try:
    output, _ = process.communicate(timeout=180)
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
    raise SystemExit("Minecraft did not pass the native menu smoke test")
print("Opened both native menus and verified Bedrock rows without Xbox sign-in")
