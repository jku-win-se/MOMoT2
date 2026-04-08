#!/usr/bin/env bash
set -euo pipefail

IMAGE_NAME="${IMAGE_NAME:-momot-rest-test}"
CONTAINER_NAME="${CONTAINER_NAME:-momot-rest-minimal-test}"
PORT="${PORT:-8081}"
SKIP_BUILD="${SKIP_BUILD:-0}"
KEEP_CONTAINER="${KEEP_CONTAINER:-0}"
KEEP_ARTIFACTS="${KEEP_ARTIFACTS:-0}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "$REPO_ROOT"

for required in \
  "stack-example-minimal/src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot" \
  "stack-example-minimal/model/stack.ecore" \
  "stack-example-minimal/model/stack.henshin" \
  "stack-example-minimal/model/input/model/model_five_stacks.xmi"; do
  if [[ ! -f "$required" ]]; then
    echo "Required file is missing: $required" >&2
    exit 1
  fi
done

command -v docker >/dev/null || { echo "docker command not found" >&2; exit 1; }
command -v jar >/dev/null || { echo "jar command not found (JDK required)" >&2; exit 1; }
command -v curl >/dev/null || { echo "curl command not found" >&2; exit 1; }
command -v unzip >/dev/null || { echo "unzip command not found" >&2; exit 1; }

if [[ "$SKIP_BUILD" != "1" ]]; then
  echo "[1/8] Building Docker image ${IMAGE_NAME}..."
  docker build -t "$IMAGE_NAME" -f Dockerfile .
fi

echo "[2/8] Resetting test container ${CONTAINER_NAME}..."
docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true

echo "[3/8] Starting container on localhost:${PORT} -> 8080..."
docker run -d --name "$CONTAINER_NAME" -p "${PORT}:8080" "$IMAGE_NAME" >/dev/null

HEALTH_URL="http://localhost:${PORT}/health"
READY=0
for _ in $(seq 1 30); do
  if curl -fsS "$HEALTH_URL" >/dev/null 2>&1; then
    READY=1
    break
  fi
  sleep 1
done
if [[ "$READY" != "1" ]]; then
  echo "Container did not become healthy at ${HEALTH_URL}" >&2
  exit 1
fi

JOB_ROOT="headless-example/job-minimal"
JOB_ZIP="headless-example/job-minimal.zip"
RESPONSE_ZIP="headless-example/response-minimal.zip"
RESPONSE_DIR="headless-example/response-minimal"

rm -rf "$JOB_ROOT" "$JOB_ZIP" "$RESPONSE_ZIP" "$RESPONSE_DIR"

echo "[4/8] Creating deterministic minimal payload..."
mkdir -p "$JOB_ROOT/src/at/ac/tuwien/big/momot/examples/stack"
mkdir -p "$JOB_ROOT/model/input/model"
cp "stack-example-minimal/src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot" "$JOB_ROOT/src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot"
cp "stack-example-minimal/model/stack.ecore" "$JOB_ROOT/model/stack.ecore"
cp "stack-example-minimal/model/stack.henshin" "$JOB_ROOT/model/stack.henshin"
cp "stack-example-minimal/model/input/model/model_five_stacks.xmi" "$JOB_ROOT/model/input/model/model_five_stacks.xmi"

echo "[5/8] Building payload zip with stable entry names..."
(
  cd "$JOB_ROOT"
  jar --create --file ../job-minimal.zip \
    model/stack.ecore \
    model/stack.henshin \
    model/input/model/model_five_stacks.xmi \
    src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot
)

echo "[6/8] Executing /run request..."
RUN_URL="http://localhost:${PORT}/run?script=src/at/ac/tuwien/big/momot/examples/stack/StackSearchExample.momot"
curl -fsS -X POST "$RUN_URL" \
  -H "Content-Type: application/zip" \
  --data-binary "@${JOB_ZIP}" \
  --output "$RESPONSE_ZIP"

echo "[7/8] Extracting and validating response..."
mkdir -p "$RESPONSE_DIR"
unzip -q "$RESPONSE_ZIP" -d "$RESPONSE_DIR"

EXIT_CODE_PATH="$RESPONSE_DIR/runner/exit_code.txt"
RUNNER_LOG_PATH="$RESPONSE_DIR/runner/runner.log"
REQUEST_PATH="$RESPONSE_DIR/runner/request.json"

if [[ ! -f "$EXIT_CODE_PATH" ]]; then
  echo "Missing response artifact: $EXIT_CODE_PATH" >&2
  exit 1
fi

EXIT_CODE="$(cat "$EXIT_CODE_PATH")"
echo "Exit code: $EXIT_CODE"
[[ -f "$REQUEST_PATH" ]] && { echo "Request metadata:"; cat "$REQUEST_PATH"; }
[[ -f "$RUNNER_LOG_PATH" ]] && { echo "Runner log tail:"; tail -n 40 "$RUNNER_LOG_PATH"; }

if [[ "$EXIT_CODE" != "0" ]]; then
  echo "Minimal REST test failed with exit_code=${EXIT_CODE}" >&2
  exit 1
fi

echo "[8/8] SUCCESS: Minimal REST test passed (exit_code=0)."
echo "Swagger UI: http://localhost:${PORT}/docs"
echo "OpenAPI:    http://localhost:${PORT}/openapi.json"

if [[ "$KEEP_CONTAINER" != "1" ]]; then
  docker rm -f "$CONTAINER_NAME" >/dev/null
fi
if [[ "$KEEP_ARTIFACTS" != "1" ]]; then
  rm -rf "$JOB_ROOT" "$JOB_ZIP" "$RESPONSE_ZIP" "$RESPONSE_DIR"
fi
