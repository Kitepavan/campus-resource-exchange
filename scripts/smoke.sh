#!/usr/bin/env bash
# End-to-end smoke test for the Campus Resource Exchange.
#
# Requires a running Tomcat with the WAR deployed, MySQL loaded with
# database/schema.sql + database/sample_data.sql, and the seeded admin account.
#
#   BASE_URL=http://localhost:8080/campus-resource-exchange bash scripts/smoke.sh
#
# The script creates uniquely named students/listing on every run, so it is safe
# to run repeatedly against the same database.

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080/campus-resource-exchange}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@campus.test}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@12345}"
STUDENT_PASSWORD="Smoke12345"

RUN_ID="$(date +%s)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

OWNER_NAME="Smoke Owner ${RUN_ID}"
OWNER_EMAIL="smoke.owner.${RUN_ID}@campus.test"
OWNER_STUDENT_ID="SMK-${RUN_ID}-A"
REQUESTER_NAME="Smoke Requester ${RUN_ID}"
REQUESTER_EMAIL="smoke.requester.${RUN_ID}@campus.test"
REQUESTER_STUDENT_ID="SMK-${RUN_ID}-B"
SELL_TITLE="Smoke SELL ${RUN_ID}"
LEND_TITLE="Smoke LEND ${RUN_ID}"

ADMIN_JAR="$WORK/admin.jar"
OWNER_JAR="$WORK/owner.jar"
REQUESTER_JAR="$WORK/requester.jar"
ANON_JAR="$WORK/anon.jar"

STEP=0
say() { STEP=$((STEP + 1)); printf '\n[%02d] %s\n' "$STEP" "$*"; }
ok() { printf '     ok  %s\n' "$*"; }
fail() { printf '\nFAIL: %s\n' "$*" >&2; exit 1; }

# --- HTTP helpers -----------------------------------------------------------

http() { # <jar> <method> <url> <out> [extra curl args...]
    local jar="$1" method="$2" url="$3" out="$4"
    shift 4
    curl -sS -o "$out" -D "$out.hdr" -w '%{http_code}' \
        -X "$method" -b "$jar" -c "$jar" "$@" "$url"
}

expect_status() { # <actual> <expected> <label>
    [[ "$1" == "$2" ]] || fail "$3: expected HTTP $2, got $1"
    ok "$3 -> HTTP $1"
}

expect_body() { # <file> <literal> <label>
    grep -qF -- "$2" "$1" || fail "$3: missing page marker [$2]"
    ok "$3"
}

expect_body_class() { # <file> <literal> <label>
    grep -q -- "$2" "$1" || fail "$3: missing markup [$2]"
    ok "$3"
}

expect_absent() { # <file> <literal> <label>
    if grep -qF -- "$2" "$1"; then
        fail "$3: unexpected marker [$2]"
    fi
    ok "$3"
}

location_of() { sed -n 's/^[Ll]ocation: //p' "$1.hdr" | tr -d '\r' | tail -1; }

expect_redirect() { # <out> <suffix> <label>
    local loc
    loc="$(location_of "$1")"
    [[ "$loc" == *"$2"* ]] || fail "$3: expected redirect to [$2], got [$loc]"
    ok "$3 -> ${loc#"$BASE_URL"}"
}

id_from_location() { sed -n 's/.*[?&]id=\([0-9][0-9]*\).*/\1/p' | sed -n '1p'; }

fetch_csrf() { # <jar> <url> <out> ; echoes the token
    http "$1" GET "$2" "$3" >/dev/null
    sed -n 's/.*name="_csrf" value="\([^"]*\)".*/\1/p' "$3" | sed -n '1p'
}

row_for() { # <html file> <literal needle> ; echoes the matching <tr> as one line
    tr '\n' ' ' < "$1" | sed 's|</tr>|\n|g' | { grep -F -- "$2" || true; } | sed -n '1p'
}

value_in_row() { # <row> <field name>
    printf '%s' "$1" | sed -n "s/.*name=\"$2\" value=\"\([^\"]*\)\".*/\1/p" | sed -n '1p'
}

tiny_png() { # <path> ; writes a valid 1x1 PNG
    printf '%s' 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==' \
        | base64 -d > "$1"
}

# --- Workflow helpers ------------------------------------------------------

register_student() { # <jar> <name> <email> <student id> <out prefix>
    local jar="$1" name="$2" email="$3" student_id="$4" out="$5"
    local csrf
    csrf="$(fetch_csrf "$jar" "$BASE_URL/register" "$WORK/$out.form")"
    [[ -n "$csrf" ]] || fail "register: no CSRF token on /register"
    expect_body "$WORK/$out.form" "Create a student account" "register form"

    local status
    status="$(http "$jar" POST "$BASE_URL/register" "$WORK/$out.post" \
        --data-urlencode "fullName=$name" \
        --data-urlencode "email=$email" \
        --data-urlencode "studentId=$student_id" \
        --data-urlencode "password=$STUDENT_PASSWORD" \
        --data-urlencode "department=Computer Science" \
        --data-urlencode "yearOfStudy=2" \
        --data-urlencode "_csrf=$csrf")"
    expect_status "$status" 302 "register $email"
    expect_redirect "$WORK/$out.post" "/login" "register redirect"

    http "$jar" GET "$BASE_URL/login" "$WORK/$out.flash" >/dev/null
    expect_body "$WORK/$out.flash" "Account created for $name." "register flash"
}

login() { # <jar> <email> <password> <expected suffix>
    local jar="$1" email="$2" password="$3" target="$4"
    local csrf
    csrf="$(fetch_csrf "$jar" "$BASE_URL/login" "$WORK/login.form")"
    [[ -n "$csrf" ]] || fail "login: no CSRF token on /login"

    local status
    status="$(http "$jar" POST "$BASE_URL/login" "$WORK/login.post" \
        --data-urlencode "email=$email" \
        --data-urlencode "password=$password" \
        --data-urlencode "_csrf=$csrf")"
    expect_status "$status" 302 "login $email"
    expect_redirect "$WORK/login.post" "$target" "login redirect"
}

verify_student() { # <admin jar> <student email> <out prefix>
    local jar="$1" email="$2" out="$3"
    local csrf
    csrf="$(fetch_csrf "$jar" "$BASE_URL/admin/users" "$WORK/$out.admin")"
    expect_body "$WORK/$out.admin" "Users and verification" "admin users page"

    local row user_id
    row="$(row_for "$WORK/$out.admin" "$email")"
    user_id="$(value_in_row "$row" "userId")"
    [[ -n "$user_id" ]] || fail "verify: no userId row for $email"

    local status
    status="$(http "$jar" POST "$BASE_URL/admin/users/verify" "$WORK/$out.post" \
        --data-urlencode "userId=$user_id" \
        --data-urlencode "_csrf=$csrf")"
    expect_status "$status" 302 "verify $email"
    expect_redirect "$WORK/$out.post" "/admin/users" "verify redirect"

    http "$jar" GET "$BASE_URL/admin/users" "$WORK/$out.done" >/dev/null
    expect_body "$WORK/$out.done" "Student verified." "verify flash"
}

create_listing() { # <jar> <out prefix> <title> <exchangeType> <price> <terms> [image]
    local jar="$1" out="$2" title="$3" exchange="$4" price="$5" terms="$6" image="${7:-}"
    local csrf
    csrf="$(fetch_csrf "$jar" "$BASE_URL/resources/new" "$WORK/$out.form")"
    [[ -n "$csrf" ]] || fail "create listing: no CSRF token on /resources/new"
    expect_body "$WORK/$out.form" "Add a resource" "listing form"

    local args=(
        -F "_csrf=$csrf"
        -F "title=$title"
        -F "description=End-to-end smoke test listing created by scripts/smoke.sh."
        -F "resourceType=BOOK"
        -F "category=Computer Science"
        -F "condition=GOOD"
        -F "exchangeType=$exchange"
        -F "courseCode=SMK101"
        -F "pickupLocation=Smoke Test Library"
    )
    if [[ "$exchange" != "DONATE" ]]; then
        args+=(-F "price=$price")
    fi
    if [[ "$exchange" == "LEND" ]]; then
        args+=(-F "lendingTerms=$terms")
    fi
    if [[ -n "$image" ]]; then
        args+=(-F "image=@$image;type=image/png")
    fi

    local status
    status="$(http "$jar" POST "$BASE_URL/resources/new" "$WORK/$out.post" "${args[@]}")"
    expect_status "$status" 302 "create $exchange listing"
    expect_redirect "$WORK/$out.post" "/resources/view?id=" "listing redirect"
    CREATED_RESOURCE_ID="$(id_from_location < "$WORK/$out.post.hdr")"
    [[ -n "$CREATED_RESOURCE_ID" ]] || fail "create listing: no id in redirect"
    ok "resource #$CREATED_RESOURCE_ID created"
}

send_request() { # <requester jar> <resource id> <out prefix>
    local jar="$1" resource_id="$2" out="$3"
    local csrf
    csrf="$(fetch_csrf "$jar" "$BASE_URL/resources/view?id=$resource_id" "$WORK/$out.view")"
    expect_body "$WORK/$out.view" "Request this resource" "request panel visible"

    local status
    status="$(http "$jar" POST "$BASE_URL/requests/new" "$WORK/$out.post" \
        --data-urlencode "resourceId=$resource_id" \
        --data-urlencode "message=Smoke request $RUN_ID" \
        --data-urlencode "_csrf=$csrf")"
    expect_status "$status" 302 "send request"
    expect_redirect "$WORK/$out.post" "/resources/view?id=$resource_id" "request redirect"

    http "$jar" GET "$BASE_URL/resources/view?id=$resource_id" "$WORK/$out.done" >/dev/null
    expect_body "$WORK/$out.done" "Request sent to the owner." "request flash"
}

accept_request() { # <owner jar> <resource title> <out prefix>
    local jar="$1" title="$2" out="$3"
    local csrf
    csrf="$(fetch_csrf "$jar" "$BASE_URL/requests/incoming" "$WORK/$out.list")"
    expect_body "$WORK/$out.list" "Incoming requests" "incoming requests page"
    expect_body "$WORK/$out.list" "Smoke request $RUN_ID" "request message visible to owner"

    local row request_id
    row="$(row_for "$WORK/$out.list" "$title")"
    request_id="$(value_in_row "$row" "requestId")"
    [[ -n "$request_id" ]] || fail "accept: no pending request row for [$title]"

    local status
    status="$(http "$jar" POST "$BASE_URL/requests/accept" "$WORK/$out.post" \
        --data-urlencode "requestId=$request_id" \
        --data-urlencode "_csrf=$csrf")"
    expect_status "$status" 302 "accept request #$request_id"
    expect_redirect "$WORK/$out.post" "/transactions/view?id=" "accept redirect"
    CREATED_TRANSACTION_ID="$(id_from_location < "$WORK/$out.post.hdr")"
    [[ -n "$CREATED_TRANSACTION_ID" ]] || fail "accept: no transaction id in redirect"
    ok "transaction #$CREATED_TRANSACTION_ID created"
}

# --- 1. Server and home page ----------------------------------------------

say "Home page is reachable and the database is connected"
status="$(http "$ANON_JAR" GET "$BASE_URL/home" "$WORK/home.html")"
expect_status "$status" 200 "GET /home"
expect_body "$WORK/home.html" "Find the book, kit, or notes you need." "home marker"

# --- 2. Administrator login ------------------------------------------------

say "Administrator logs in"
login "$ADMIN_JAR" "$ADMIN_EMAIL" "$ADMIN_PASSWORD" "/admin"
status="$(http "$ADMIN_JAR" GET "$BASE_URL/admin" "$WORK/admin.html")"
expect_status "$status" 200 "GET /admin"
expect_body "$WORK/admin.html" "Platform overview" "admin dashboard marker"

# --- 3. Register and verify the first student ------------------------------

say "New student registers (unverified)"
register_student "$OWNER_JAR" "$OWNER_NAME" "$OWNER_EMAIL" "$OWNER_STUDENT_ID" "owner"

say "Administrator verifies the new student"
verify_student "$ADMIN_JAR" "$OWNER_EMAIL" "ownerverify"

# --- 4. Student creates a SELL listing with an image -----------------------

say "Verified student logs in"
login "$OWNER_JAR" "$OWNER_EMAIL" "$STUDENT_PASSWORD" "/dashboard"
status="$(http "$OWNER_JAR" GET "$BASE_URL/dashboard" "$WORK/owner-dashboard.html")"
expect_status "$status" 200 "GET /dashboard"
expect_body "$WORK/owner-dashboard.html" "Your account is verified." "verified dashboard marker"

say "Student creates a SELL listing with a PNG image"
tiny_png "$WORK/tiny.png"
create_listing "$OWNER_JAR" "sell" "$SELL_TITLE" "SELL" "45.00" "" "$WORK/tiny.png"
SELL_RESOURCE_ID="$CREATED_RESOURCE_ID"
status="$(http "$OWNER_JAR" GET "$BASE_URL/resources/view?id=$CREATED_RESOURCE_ID" "$WORK/sell-view.html")"
expect_status "$status" 200 "GET listing detail"
expect_body "$WORK/sell-view.html" "About this resource" "listing detail marker"
expect_body "$WORK/sell-view.html" "$SELL_TITLE" "listing title rendered"
expect_body_class "$WORK/sell-view.html" "status-available" "listing starts available"

image_name="$(grep -o 'images/[a-f0-9-]\{36\}\.png' "$WORK/sell-view.html" | sed -n '1p' | cut -d/ -f2)"
[[ -n "$image_name" ]] || fail "uploaded image is not linked from the listing page"
status="$(http "$OWNER_JAR" GET "$BASE_URL/images/$image_name" "$WORK/image.bin")"
expect_status "$status" 200 "GET uploaded image"
grep -q '^Content-Type: image/png' "$WORK/image.bin.hdr" || fail "image served with wrong content type"
ok "image served as image/png"
[[ "$(wc -c < "$WORK/image.bin")" -gt 0 ]] || fail "image response was empty"
ok "image bytes returned"

# --- 5. Second student registers, is verified, and searches ----------------

say "Second student registers and is verified"
register_student "$REQUESTER_JAR" "$REQUESTER_NAME" "$REQUESTER_EMAIL" "$REQUESTER_STUDENT_ID" "requester"
verify_student "$ADMIN_JAR" "$REQUESTER_EMAIL" "requesterverify"
login "$REQUESTER_JAR" "$REQUESTER_EMAIL" "$STUDENT_PASSWORD" "/dashboard"

say "Second student finds the listing through search"
status="$(http "$REQUESTER_JAR" GET "$BASE_URL/resources" "$WORK/search.html" -G --data-urlencode "keyword=$SELL_TITLE")"
expect_status "$status" 200 "GET /resources?keyword=..."
expect_body "$WORK/search.html" "$SELL_TITLE" "search result contains listing"
expect_body "$WORK/search.html" "Browse academic resources" "catalogue marker"

# --- 6. Request, accept, and complete a SELL exchange ----------------------

say "Second student requests the listing"
send_request "$REQUESTER_JAR" "$CREATED_RESOURCE_ID" "sellrequest"

say "Owner accepts the request and a transaction is created"
accept_request "$OWNER_JAR" "$SELL_TITLE" "sellaccept"
SELL_TRANSACTION_ID="$CREATED_TRANSACTION_ID"
status="$(http "$OWNER_JAR" GET "$BASE_URL/transactions/view?id=$SELL_TRANSACTION_ID" "$WORK/sell-tx.html")"
expect_status "$status" 200 "GET transaction detail"
expect_body "$WORK/sell-tx.html" "Transaction #$SELL_TRANSACTION_ID" "transaction id rendered"
expect_body "$WORK/sell-tx.html" "Mark handover complete" "handover action available"

say "Owner completes the handover; the resource becomes unavailable"
csrf="$(fetch_csrf "$OWNER_JAR" "$BASE_URL/transactions/view?id=$SELL_TRANSACTION_ID" "$WORK/complete.form")"
status="$(http "$OWNER_JAR" POST "$BASE_URL/transactions/complete" "$WORK/complete.post" \
    --data-urlencode "id=$SELL_TRANSACTION_ID" --data-urlencode "_csrf=$csrf")"
expect_status "$status" 302 "complete handover"
expect_redirect "$WORK/complete.post" "/transactions" "handover redirect"
http "$OWNER_JAR" GET "$BASE_URL/transactions" "$WORK/complete.done" >/dev/null
expect_body "$WORK/complete.done" "Handover completed." "handover flash"
http "$OWNER_JAR" GET "$BASE_URL/resources/view?id=$CREATED_RESOURCE_ID" "$WORK/sell-done.html" >/dev/null
expect_body_class "$WORK/sell-done.html" "status-unavailable" "sold listing is unavailable"

# --- 7. Review and duplicate-review rejection ------------------------------

say "Requester reviews the completed exchange"
status="$(http "$REQUESTER_JAR" GET "$BASE_URL/transactions/view?id=$SELL_TRANSACTION_ID" "$WORK/review-tx.html")"
expect_status "$status" 200 "GET completed transaction as requester"
expect_body "$WORK/review-tx.html" "Leave a review" "review link visible"
status="$(http "$REQUESTER_JAR" GET "$BASE_URL/reviews/new?transactionId=$SELL_TRANSACTION_ID" "$WORK/review-form.html")"
expect_status "$status" 200 "GET review form"
expect_body "$WORK/review-form.html" "Review this exchange" "review form marker"
csrf="$(sed -n 's/.*name="_csrf" value="\([^"]*\)".*/\1/p' "$WORK/review-form.html" | sed -n '1p')"
status="$(http "$REQUESTER_JAR" POST "$BASE_URL/reviews/new" "$WORK/review.post" \
    --data-urlencode "transactionId=$SELL_TRANSACTION_ID" \
    --data-urlencode "rating=5" \
    --data-urlencode "comment=Smoke test review $RUN_ID" \
    --data-urlencode "_csrf=$csrf")"
expect_status "$status" 302 "submit review"
expect_redirect "$WORK/review.post" "/transactions/view?id=$SELL_TRANSACTION_ID" "review redirect"
http "$REQUESTER_JAR" GET "$BASE_URL/transactions/view?id=$SELL_TRANSACTION_ID" "$WORK/review.done" >/dev/null
expect_body "$WORK/review.done" "Thank you. Your review was saved." "review flash"

say "A duplicate review is rejected"
csrf="$(sed -n 's/.*name="_csrf" value="\([^"]*\)".*/\1/p' "$WORK/review.done" | sed -n '1p')"
status="$(http "$REQUESTER_JAR" POST "$BASE_URL/reviews/new" "$WORK/review-dup.post" \
    --data-urlencode "transactionId=$SELL_TRANSACTION_ID" \
    --data-urlencode "rating=4" \
    --data-urlencode "comment=Duplicate review $RUN_ID" \
    --data-urlencode "_csrf=$csrf")"
expect_status "$status" 302 "duplicate review is refused"
expect_redirect "$WORK/review-dup.post" "/transactions" "duplicate review redirect"
http "$REQUESTER_JAR" GET "$BASE_URL/transactions" "$WORK/review-dup.done" >/dev/null
expect_body "$WORK/review-dup.done" "already been reviewed" "duplicate review flash"

# --- 8. LEND flow: active loan then return --------------------------------

say "Owner creates a LEND listing"
create_listing "$OWNER_JAR" "lend" "$LEND_TITLE" "LEND" "200.00" "14 days, return to the library desk"
LEND_RESOURCE_ID="$CREATED_RESOURCE_ID"

say "Second student requests the LEND listing"
send_request "$REQUESTER_JAR" "$LEND_RESOURCE_ID" "lendrequest"

say "Owner accepts the loan request"
accept_request "$OWNER_JAR" "$LEND_TITLE" "lendaccept"
LEND_TRANSACTION_ID="$CREATED_TRANSACTION_ID"

say "Owner starts the loan; the resource becomes lent"
csrf="$(fetch_csrf "$OWNER_JAR" "$BASE_URL/transactions/view?id=$LEND_TRANSACTION_ID" "$WORK/lend.form")"
expect_body "$WORK/lend.form" "Mark as lent" "lend action available"
status="$(http "$OWNER_JAR" POST "$BASE_URL/transactions/lend" "$WORK/lend.post" \
    --data-urlencode "id=$LEND_TRANSACTION_ID" --data-urlencode "_csrf=$csrf")"
expect_status "$status" 302 "mark as lent"
http "$OWNER_JAR" GET "$BASE_URL/transactions" "$WORK/lend.done" >/dev/null
expect_body "$WORK/lend.done" "Loan started." "lend flash"
http "$OWNER_JAR" GET "$BASE_URL/resources/view?id=$LEND_RESOURCE_ID" "$WORK/lend-view.html" >/dev/null
expect_body_class "$WORK/lend-view.html" "status-lent" "listing is lent"

say "Owner records the return; the resource becomes available again"
csrf="$(fetch_csrf "$OWNER_JAR" "$BASE_URL/transactions/view?id=$LEND_TRANSACTION_ID" "$WORK/return.form")"
expect_body "$WORK/return.form" "Record return" "return action available"
status="$(http "$OWNER_JAR" POST "$BASE_URL/transactions/return" "$WORK/return.post" \
    --data-urlencode "id=$LEND_TRANSACTION_ID" --data-urlencode "_csrf=$csrf")"
expect_status "$status" 302 "record return"
http "$OWNER_JAR" GET "$BASE_URL/transactions" "$WORK/return.done" >/dev/null
expect_body "$WORK/return.done" "Return recorded." "return flash"
http "$OWNER_JAR" GET "$BASE_URL/resources/view?id=$LEND_RESOURCE_ID" "$WORK/return-view.html" >/dev/null
expect_body_class "$WORK/return-view.html" "status-available" "listing is available again"

# --- 9. Authorization and CSRF are enforced -------------------------------

say "A student cannot open the administrator area"
status="$(http "$OWNER_JAR" GET "$BASE_URL/admin" "$WORK/forbidden.html")"
expect_status "$status" 403 "student GET /admin"
expect_body "$WORK/forbidden.html" "Access denied" "403 page marker"

say "A POST without a CSRF token is rejected"
status="$(http "$ANON_JAR" POST "$BASE_URL/login" "$WORK/no-csrf.html" \
    --data-urlencode "email=$OWNER_EMAIL" --data-urlencode "password=$STUDENT_PASSWORD")"
expect_status "$status" 403 "POST /login without _csrf"

say "An anonymous visitor is redirected to the login page"
status="$(http "$ANON_JAR" GET "$BASE_URL/dashboard" "$WORK/anon.html")"
expect_status "$status" 302 "anonymous GET /dashboard"
expect_redirect "$WORK/anon.html" "/login" "anonymous redirect"

# --- 10. Admin backup and restore ----------------------------------------

say "Administrator creates a catalogue backup"
csrf="$(fetch_csrf "$ADMIN_JAR" "$BASE_URL/admin/backup" "$WORK/backup.form")"
expect_body "$WORK/backup.form" "Catalogue backup" "backup page marker"
status="$(http "$ADMIN_JAR" POST "$BASE_URL/admin/backup/create" "$WORK/backup.post" \
    --data-urlencode "_csrf=$csrf")"
expect_status "$status" 302 "create backup"
http "$ADMIN_JAR" GET "$BASE_URL/admin/backup" "$WORK/backup.done" >/dev/null
expect_body "$WORK/backup.done" "Backup created: catalog-" "backup flash"
BACKUP_FILE="$(grep -o 'catalog-[0-9]\{8\}-[0-9]\{6\}-[0-9]\{3,6\}\.ser' "$WORK/backup.done" | sed -n '1p')"
[[ -n "$BACKUP_FILE" ]] || fail "backup page did not list the new file"
ok "backup $BACKUP_FILE"

say "Administrator restores the backup (duplicates are skipped)"
csrf="$(sed -n 's/.*name="_csrf" value="\([^"]*\)".*/\1/p' "$WORK/backup.done" | sed -n '1p')"
status="$(http "$ADMIN_JAR" POST "$BASE_URL/admin/backup/restore" "$WORK/restore.post" \
    --data-urlencode "filename=$BACKUP_FILE" --data-urlencode "_csrf=$csrf")"
expect_status "$status" 302 "restore backup"
http "$ADMIN_JAR" GET "$BASE_URL/admin/backup" "$WORK/restore.done" >/dev/null
expect_body "$WORK/restore.done" "Backup restored." "restore flash"

# --- Summary --------------------------------------------------------------

printf '\nALL CHECKS PASSED (%d steps)\n' "$STEP"
printf '  SELL listing #%s / transaction #%s\n' "$SELL_RESOURCE_ID" "$SELL_TRANSACTION_ID"
printf '  LEND listing #%s / transaction #%s\n' "$LEND_RESOURCE_ID" "$LEND_TRANSACTION_ID"
printf '  backup file %s\n' "$BACKUP_FILE"
