#!/usr/bin/env bash
# End-to-end: login, refresh rotation and reuse detection, logout, registration,
# actuator lockdown, circuit breaker, concurrent refresh. Starts the whole stack unless E2E_NO_START=1.
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
start_stack

ADMIN_PW=$(env_val ADMIN_PASSWORD)
INTERNAL_KEY=$(env_val INTERNAL_API_KEY)
SUFFIX=$RANDOM

echo "--- login"
http "bad password"  401 -X POST "$G/api/auth/login" -H "$J" -d '{"username":"admin","password":"x"}'
http "unknown user"  401 -X POST "$G/api/auth/login" -H "$J" -d '{"username":"nobody","password":"x"}'
ADMIN=$(login admin "$ADMIN_PW")
expect "admin gets an access token" yes "$([ -n "$ADMIN" ] && echo yes || echo no)"

echo "--- gateway authorization"
http "no token"                 401 "$G/api/users"
http "garbage token"            401 "$G/api/users" -H "Authorization: Bearer abc.def.ghi"
req  "GET /api/users"           200 GET "$G/api/users" "$ADMIN"
req  "GET /api/me/profile"      200 GET "$G/api/me/profile" "$ADMIN"
req  "GET /api/me/permissions"  200 GET "$G/api/me/permissions" "$ADMIN"
req  "GET /api/roles"           200 GET "$G/api/roles?size=2" "$ADMIN"
req  "GET /api/permissions"     200 GET "$G/api/permissions" "$ADMIN"
req  "unknown path is denied"   403 GET "$G/api/whatever" "$ADMIN"

echo "--- refresh rotation, reuse detection, logout"
JAR="$E2E_WORK/jar"; OLD="$E2E_WORK/jar_old"
login_cookie "$JAR" admin "$ADMIN_PW"
cp "$JAR" "$OLD"
http "refresh (valid cookie)"         200 -b "$JAR" -c "$JAR" -X POST "$G/api/auth/refresh"
http "refresh with the new cookie"    200 -b "$JAR" -c "$JAR" -X POST "$G/api/auth/refresh"
cp "$JAR" "$E2E_WORK/jar_latest"
http "reuse of a rotated cookie"      401 -b "$OLD" -X POST "$G/api/auth/refresh"
http "latest cookie dies with session" 401 -b "$E2E_WORK/jar_latest" -X POST "$G/api/auth/refresh"
# a different session of the same user is not affected
login_cookie "$E2E_WORK/jar_other" admin "$ADMIN_PW"
http "other session still works"      200 -b "$E2E_WORK/jar_other" -c "$E2E_WORK/jar_other" -X POST "$G/api/auth/refresh"
http "logout"                         204 -b "$E2E_WORK/jar_other" -X POST "$G/api/auth/logout"
http "refresh after logout"           401 -b "$E2E_WORK/jar_other" -X POST "$G/api/auth/refresh"
http "refresh without cookie"         401 -X POST "$G/api/auth/refresh"

echo "--- registration"
http "register, weak password" 400 -X POST "$G/api/users/register" -H "$J" \
  -d '{"username":"bob","password":"x","confirmPassword":"x","firstName":"Bob","lastName":"B"}'
http "register ok" 201 -X POST "$G/api/users/register" -H "$J" \
  -d "{\"username\":\"bob$SUFFIX\",\"password\":\"Passw0rd@1\",\"confirmPassword\":\"Passw0rd@1\",\"firstName\":\"Bob\",\"lastName\":\"Bee\"}"
http "register, name taken" 409 -X POST "$G/api/users/register" -H "$J" \
  -d "{\"username\":\"bob$SUFFIX\",\"password\":\"Passw0rd@1\",\"confirmPassword\":\"Passw0rd@1\",\"firstName\":\"Bob\",\"lastName\":\"Bee\"}"

echo "--- actuator"
http "health via gateway (public)"   200 "$G/actuator/health"
http "gateway routes, no token"       401 "$G/actuator/gateway/routes"
http "gateway routes, admin"          403 "$G/actuator/gateway/routes" -H "Authorization: Bearer $ADMIN"
http "env, admin"                     403 "$G/actuator/env" -H "Authorization: Bearer $ADMIN"
http "service health, direct"         200 localhost:8081/actuator/health
http "service info, no key"           401 localhost:8081/actuator/info
http "service info, with key"         200 localhost:8081/actuator/info -H "X-Internal-Api-Key: $INTERNAL_KEY"
http "internal endpoint, no key"      401 "localhost:8081/internal/users?username=admin"

echo "--- circuit breaker: 15 wrong passwords must not trip it"
codes=$(for _ in $(seq 1 15); do
  curl -s -m 10 -o /dev/null -w '%{http_code}\n' -X POST "$G/api/auth/login" -H "$J" \
    -d '{"username":"admin","password":"wrong"}'
done | sort | uniq -c | awk '{printf "%sx%s ", $1, $2}')
expect "15 bad logins" "15x401 " "$codes"
http "correct login right after" 200 -X POST "$G/api/auth/login" -H "$J" \
  -d "{\"username\":\"admin\",\"password\":\"$ADMIN_PW\"}"

echo "--- 6 concurrent refreshes with one token: exactly one wins"
login_cookie "$E2E_WORK/jar_race" admin "$ADMIN_PW"
rm -f "$E2E_WORK/race.txt"; pids=()
for _ in 1 2 3 4 5 6; do
  curl -s -m 10 -o /dev/null -w '%{http_code}\n' -b "$E2E_WORK/jar_race" -X POST "$G/api/auth/refresh" \
    >> "$E2E_WORK/race.txt" &
  pids+=($!)
done
for p in "${pids[@]}"; do wait "$p"; done
expect "status distribution" "1x200 5x401 " "$(sort "$E2E_WORK/race.txt" | uniq -c | awk '{printf "%sx%s ", $1, $2}')"

startup_failures
finish
