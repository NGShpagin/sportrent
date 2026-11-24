#!/usr/bin/env bash
# Shared helpers for the end-to-end scripts. Source this file, do not run it.
#
# Environment:
#   E2E_NO_START=1   do not start/stop the stack, use one that is already running on :8080
#   E2E_WORK=<dir>   where logs and temp files go (default: <repo>/.e2e)
#
# Requirements: built jars (mvn -DskipTests package), the databases up (docker compose up -d),
# a filled-in .env in the repo root, curl and python3.

E2E_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
E2E_WORK="${E2E_WORK:-$E2E_ROOT/.e2e}"
G="localhost:8080"
J="Content-Type: application/json"
E2E_PASS=0
E2E_FAIL=0
E2E_PIDS=()
BODY="$E2E_WORK/body"
CODE=""

mkdir -p "$E2E_WORK"

env_val() { # env_val NAME -> value from .env
  grep -E "^$1=" "$E2E_ROOT/.env" | head -1 | cut -d= -f2-
}

jget() { # jget 'python expr over d' < json
  python3 -c "import sys,json; d=json.load(sys.stdin); print(eval(sys.argv[1]))" "$1"
}

# ---- stack lifecycle -------------------------------------------------------

start_service() { # start_service <module>
  local jar="$E2E_ROOT/$1/target/$1-0.0.1-SNAPSHOT.jar"
  [ -f "$jar" ] || { echo "missing $jar, run: mvn -DskipTests package" >&2; exit 2; }
  # cd first: "cd x && cmd &" would background a subshell and $! would not be java's PID
  cd "$E2E_ROOT" || exit 2
  nohup java -jar "$jar" > "$E2E_WORK/$1.log" 2>&1 &
  echo $! >> "$E2E_WORK/pids"
}

stop_stack() {
  [ "${E2E_NO_START:-}" = "1" ] && return 0
  if [ -f "$E2E_WORK/pids" ]; then
    local pid i alive
    while read -r pid; do kill "$pid" 2>/dev/null; done < "$E2E_WORK/pids"
    # graceful shutdown takes a while; wait so the next run does not find :8080 still taken
    for i in $(seq 1 60); do
      alive=0
      while read -r pid; do kill -0 "$pid" 2>/dev/null && alive=1; done < "$E2E_WORK/pids"
      [ "$alive" = 0 ] && break
      sleep 1
    done
    rm -f "$E2E_WORK/pids"
  fi
}

start_stack() {
  [ "${E2E_NO_START:-}" = "1" ] && { wait_for_gateway; return 0; }
  [ -f "$E2E_ROOT/.env" ] || { echo ".env not found, copy .env.example" >&2; exit 2; }
  if curl -s -m 2 -o /dev/null "$G/actuator/health"; then
    echo "something already listens on :8080, stop it or use E2E_NO_START=1" >&2; exit 2
  fi
  rm -f "$E2E_WORK"/*.log "$E2E_WORK/pids"
  trap stop_stack EXIT
  start_service eureka-server
  sleep 14
  local m
  for m in user-service auth-service authz-service api-gateway; do start_service "$m"; done
  # authz-service assigns the admin role at startup, wait until it is done (or gave up)
  local i
  for i in $(seq 1 40); do
    grep -qE "Admin role assigned|Could not assign admin" "$E2E_WORK/authz-service.log" 2>/dev/null && break
    sleep 3
  done
  sleep 8   # let the gateway pick up the registry
  wait_for_gateway
  wait_authz_ready
}

# While user-service was still starting, authz-service's startup calls to it failed. Those failures
# sit in the circuit breaker's sliding window (size 10, see resilience4j config) and would trip it
# as soon as the window fills up. Poll an idempotent call (admin <- ROLE_ADMIN) until 10 succeed in
# a row, which flushes the window, before the checks begin.
wait_authz_ready() {
  local admin_id token ok=0 i code
  token=$(login admin "$(env_val ADMIN_PASSWORD)")
  [ -n "$token" ] || return 0
  admin_id=$(curl -s -m 10 -H "Authorization: Bearer $token" "$G/api/me/profile" | jget "d['id']")
  for i in $(seq 1 60); do
    code=$(curl -s -m 10 -o /dev/null -w '%{http_code}' -X PUT \
      "$G/api/roles/1/users/$admin_id" -H "Authorization: Bearer $token")
    if [ "$code" = "204" ]; then ok=$((ok + 1)); else ok=0; fi
    [ "$ok" -ge 10 ] && return 0
    sleep 0.5
  done
  echo "warning: authz-service did not become ready" >&2
}

wait_for_gateway() {
  local i
  for i in $(seq 1 20); do
    curl -s -m 2 -o /dev/null "$G/actuator/health" && return 0
    sleep 2
  done
  echo "gateway did not come up, see $E2E_WORK/api-gateway.log" >&2
  exit 2
}

# ---- assertions ------------------------------------------------------------

record() { # record <ok|FAIL> <message>
  if [ "$1" = "ok" ]; then E2E_PASS=$((E2E_PASS + 1)); else E2E_FAIL=$((E2E_FAIL + 1)); fi
  printf "%-5s %s\n" "$1" "$2"
}

# http <name> <expected-status> <curl args...>   (body is left in $BODY, status in $CODE)
http() {
  local name=$1 expected=$2; shift 2
  CODE=$(curl -s -m 10 -o "$BODY" -w '%{http_code}' "$@")
  local mark=ok; [ "$CODE" = "$expected" ] || mark=FAIL
  record "$mark" "$(printf '%-44s want %s got %s %s' "$name" "$expected" "$CODE" \
    "$(head -c 90 "$BODY" | tr '\n' ' ')")"
}

# req <name> <expected> <method> <url> <token> [json-body]
req() {
  local name=$1 expected=$2 method=$3 url=$4 token=$5 body=${6:-}
  if [ -n "$body" ]; then
    http "$name" "$expected" -X "$method" "$url" -H "Authorization: Bearer $token" -H "$J" -d "$body"
  else
    http "$name" "$expected" -X "$method" "$url" -H "Authorization: Bearer $token"
  fi
}

expect() { # expect <name> <expected> <actual>
  if [ "$2" = "$3" ]; then record ok "$1: $3"; else record FAIL "$1: want '$2' got '$3'"; fi
}

# login <user> <password> -> prints the access token (empty on failure)
login() {
  curl -s -m 10 -i -X POST "$G/api/auth/login" -H "$J" \
    -d "{\"username\":\"$1\",\"password\":\"$2\"}" \
    | grep -i '^authorization: Bearer ' | sed 's/^[Aa]uthorization: Bearer //' | tr -d '\r'
}

# login_cookie <jar> <user> <password>: logs in and stores the refresh-token cookie in <jar>
login_cookie() {
  curl -s -m 10 -c "$1" -o /dev/null -X POST "$G/api/auth/login" -H "$J" \
    -d "{\"username\":\"$2\",\"password\":\"$3\"}"
}

# register_user <username> <password> -> prints the new user id
register_user() {
  curl -s -m 10 -X POST "$G/api/users/register" -H "$J" \
    -d "{\"username\":\"$1\",\"password\":\"$2\",\"confirmPassword\":\"$2\",\"firstName\":\"Test\",\"lastName\":\"User\"}" \
    | jget "d['id']"
}

startup_failures() {
  [ "${E2E_NO_START:-}" = "1" ] && return 0
  echo "--- startup failures"
  local m n
  for m in user-service auth-service authz-service api-gateway; do
    n=$(grep -cE 'APPLICATION FAILED|Application run failed' "$E2E_WORK/$m.log" 2>/dev/null || true)
    expect "$m" 0 "${n:-0}"
  done
}

finish() {
  echo
  echo "passed: $E2E_PASS  failed: $E2E_FAIL  (logs: $E2E_WORK)"
  [ "$E2E_FAIL" -eq 0 ]
}
