#!/usr/bin/env bash
# End-to-end: creating permissions, granting them to roles, assigning/revoking roles for users.
# Starts the whole stack unless E2E_NO_START=1.
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
start_stack

S=$RANDOM
ADMIN=$(login admin "$(env_val ADMIN_PASSWORD)")
BOB_ID=$(register_user "bob$S" 'Passw0rd@1')
register_user "carol$S" 'Passw0rd@1' > /dev/null
CAROL=$(login "carol$S" 'Passw0rd@1')

echo "--- create permission"
req "create permission"                 201 POST "$G/api/permissions" "$ADMIN" "{\"resource\":\"e2e\",\"action\":\"act$S\",\"description\":\"e2e\"}"
NEWPERM=$(jget "d['id']" < "$BODY")
req "create same permission again"      409 POST "$G/api/permissions" "$ADMIN" "{\"resource\":\"e2e\",\"action\":\"act$S\"}"
req "create permission, bad name"       400 POST "$G/api/permissions" "$ADMIN" '{"resource":"E2E:x","action":"y"}'
req "create permission, malformed json" 400 POST "$G/api/permissions" "$ADMIN" '{oops'
req "create permission as regular user" 403 POST "$G/api/permissions" "$CAROL" "{\"resource\":\"e2e\",\"action\":\"hack$S\"}"

echo "--- grant permissions to a role"
req "create role"                       201 POST "$G/api/roles" "$ADMIN" "{\"code\":\"ROLE_E2E_$S\",\"name\":\"E2E\"}"
ROLE=$(jget "d['id']" < "$BODY")
curl -s -H "Authorization: Bearer $ADMIN" "$G/api/permissions" > "$E2E_WORK/perms"
RM=$(jget "[p['id'] for p in d if p['name']=='roles:manage'][0]" < "$E2E_WORK/perms")
req "add permissions (2 real + 1 unknown)" 200 POST "$G/api/roles/$ROLE/permissions" "$ADMIN" "{\"permissions\":[$RM,$NEWPERM,999999]}"
expect "unknown id reported" "[999999]" "$(jget "d['errorPermissions']" < "$BODY" | tr -d ' ')"
req "add permissions to unknown role"   404 POST "$G/api/roles/999999/permissions" "$ADMIN" '{"permissions":[1]}'
req "add empty permissions list"        400 POST "$G/api/roles/$ROLE/permissions" "$ADMIN" '{"permissions":[]}'
req "add permissions as regular user"   403 POST "$G/api/roles/$ROLE/permissions" "$CAROL" "{\"permissions\":[$RM]}"

echo "--- assign / revoke role"
req "assign role to bob"                204 PUT    "$G/api/roles/$ROLE/users/$BOB_ID" "$ADMIN"
req "assign again (idempotent)"         204 PUT    "$G/api/roles/$ROLE/users/$BOB_ID" "$ADMIN"
req "assign to unknown user"            404 PUT    "$G/api/roles/$ROLE/users/00000000-0000-0000-0000-000000000000" "$ADMIN"
req "assign unknown role"               404 PUT    "$G/api/roles/999999/users/$BOB_ID" "$ADMIN"
req "assign with bad uuid"              400 PUT    "$G/api/roles/$ROLE/users/not-a-uuid" "$ADMIN"
req "assign as regular user"            403 PUT    "$G/api/roles/$ROLE/users/$BOB_ID" "$CAROL"
req "roles of bob (admin view)"         200 GET    "$G/api/roles?user_id=$BOB_ID" "$ADMIN"
expect "bob holds the new role" yes "$(jget "'ROLE_E2E_$S' in [r['code'] for r in d['data']]" < "$BODY" | sed 's/True/yes/;s/False/no/')"
BOB=$(login "bob$S" 'Passw0rd@1')
req "bob can now manage roles"          200 GET    "$G/api/roles?size=1" "$BOB"
req "role holds the e2e permission"     200 GET    "$G/api/permissions?role_id=$ROLE" "$ADMIN"
expect "permission listed" yes "$(jget "'e2e:act$S' in [p['name'] for p in d]" < "$BODY" | sed 's/True/yes/;s/False/no/')"
req "revoke role from bob"              204 DELETE "$G/api/roles/$ROLE/users/$BOB_ID" "$ADMIN"
req "revoke again (idempotent)"         204 DELETE "$G/api/roles/$ROLE/users/$BOB_ID" "$ADMIN"
req "roles of bob after revoke"         200 GET    "$G/api/roles?user_id=$BOB_ID" "$ADMIN"
expect "role is gone" no "$(jget "'ROLE_E2E_$S' in [r['code'] for r in d['data']]" < "$BODY" | sed 's/True/yes/;s/False/no/')"

startup_failures
finish
