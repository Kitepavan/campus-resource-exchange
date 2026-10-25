#!/usr/bin/env bash
# Removes the data scripts/smoke.sh created (accounts matching smoke.*@campus.test
# and everything they own). Only useful when the smoke test ran against the local
# database. Credentials come from Tomcat's bin/setenv.sh, like every other script.
#
#   bash scripts/clean-smoke.sh

set -euo pipefail

SETENV="${SETENV:-$HOME/tools/apache-tomcat-10.1.55/bin/setenv.sh}"
[ -f "$SETENV" ] || { echo "error: missing $SETENV" >&2; exit 1; }
. "$SETENV"

mysql -u"$DB_USER" -p"$DB_PASSWORD" campus_exchange <<'SQL'
DELETE FROM reviews
 WHERE reviewer_id IN (SELECT user_id FROM users WHERE email LIKE 'smoke.%@campus.test')
    OR transaction_id IN (
        SELECT transaction_id FROM exchange_transactions
         WHERE owner_id IN (SELECT user_id FROM users WHERE email LIKE 'smoke.%@campus.test')
            OR requester_id IN (SELECT user_id FROM users WHERE email LIKE 'smoke.%@campus.test'));

DELETE FROM exchange_transactions
 WHERE owner_id IN (SELECT user_id FROM users WHERE email LIKE 'smoke.%@campus.test')
    OR requester_id IN (SELECT user_id FROM users WHERE email LIKE 'smoke.%@campus.test');

DELETE FROM resource_requests
 WHERE requester_id IN (SELECT user_id FROM users WHERE email LIKE 'smoke.%@campus.test')
    OR resource_id IN (
        SELECT resource_id FROM resources
         WHERE owner_id IN (SELECT user_id FROM users WHERE email LIKE 'smoke.%@campus.test'));

DELETE FROM resources
 WHERE owner_id IN (SELECT user_id FROM users WHERE email LIKE 'smoke.%@campus.test');

DELETE FROM users WHERE email LIKE 'smoke.%@campus.test';
SQL

echo "cleaned smoke-test data"
