#!/usr/bin/env bash
# Build (if stale), deploy and start the Campus Resource Exchange on local Tomcat.
#
#   ./run.sh                build if needed, deploy, start, wait until it answers
#   ./run.sh --skip-build   reuse target/campus-resource-exchange.war
#   ./run.sh smoke          start, then run scripts/smoke.sh
#   ./run.sh stop           stop Tomcat
#
# DB_URL/DB_USER/DB_PASSWORD/UPLOAD_DIR come from Tomcat's bin/setenv.sh,
# so no secrets live in this repo.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
export PATH="$JAVA_HOME/bin:$HOME/tools/apache-maven-3.9.12/bin:$PATH"
CATALINA_HOME="${CATALINA_HOME:-$HOME/tools/apache-tomcat-10.1.55}"
SETENV="$CATALINA_HOME/bin/setenv.sh"
WAR="$ROOT/target/campus-resource-exchange.war"
APP="campus-resource-exchange"
BASE_URL="${BASE_URL:-http://localhost:8080/$APP}"

die() { echo "error: $*" >&2; exit 1; }
say() { printf '\n==> %s\n' "$*"; }
tomcat_running() { pgrep -f 'org.apache.catalina.startup.Bootstrap' >/dev/null; }

usage() { sed -n '2,10p' "$0"; }

stop_tomcat() {
    tomcat_running || return 0
    say "stopping Tomcat"
    "$CATALINA_HOME/bin/shutdown.sh" >/dev/null 2>&1 || true
    for _ in $(seq 1 30); do
        tomcat_running || { echo "Tomcat stopped"; return 0; }
        sleep 1
    done
    die "Tomcat did not stop; kill it and retry (logs: $CATALINA_HOME/logs/catalina.out)"
}

wait_for_app() {
    say "waiting for $BASE_URL"
    for _ in $(seq 1 90); do
        code="$(curl -sL -o /dev/null -w '%{http_code}' "$BASE_URL/" || true)"
        [ "$code" = "200" ] && { echo "up (HTTP $code)"; return 0; }
        sleep 1
    done
    echo "last HTTP code: ${code:-none}" >&2
    tail -n 40 "$CATALINA_HOME/logs/catalina.out" >&2 || true
    die "app did not answer after 90s"
}

cmd=start
skip_build=0
for arg in "$@"; do
    case "$arg" in
        start|smoke|stop) cmd="$arg" ;;
        --skip-build) skip_build=1 ;;
        -h|--help) usage; exit 0 ;;
        *) die "unknown argument: $arg (try --help)" ;;
    esac
done

[ -x "$CATALINA_HOME/bin/startup.sh" ] || die "Tomcat not found at $CATALINA_HOME"

if [ "$cmd" = "stop" ]; then
    stop_tomcat
    exit 0
fi

is_stale() {
    [ ! -f "$WAR" ] && return 0
    [ -n "$(find "$ROOT/src" "$ROOT/pom.xml" -newer "$WAR" -print -quit)" ]
}

# Build before sourcing setenv.sh: BACKUP_DIR/UPLOAD_DIR are exported there and
# would bleed into the test JVM, which expects them unset.
if [ "$skip_build" = 0 ] && is_stale; then
    say "building"
    ( cd "$ROOT" && mvn -q clean package ) || die "build failed"
fi
[ -f "$WAR" ] || die "no WAR at $WAR — run without --skip-build"

[ -f "$SETENV" ] || die "missing $SETENV — create it with: bash scripts/setup-db.sh"
. "$SETENV"
mkdir -p "$UPLOAD_DIR" "$BACKUP_DIR"

say "checking MySQL"
if ! mysql -u"$DB_USER" -p"$DB_PASSWORD" -e 'SELECT 1' >/dev/null 2>&1; then
    echo "not reachable, trying to start the service"
    sudo -n systemctl start mysql 2>/dev/null || sudo -n service mysql start 2>/dev/null || true
    sleep 3
    mysql -u"$DB_USER" -p"$DB_PASSWORD" -e 'SELECT 1' >/dev/null 2>&1 \
        || die "cannot connect to $DB_URL — start MySQL with: sudo systemctl start mysql"
fi
echo "MySQL OK"

say "deploying $APP"
stop_tomcat
rm -rf "$CATALINA_HOME/webapps/$APP"
cp "$WAR" "$CATALINA_HOME/webapps/$APP.war"
"$CATALINA_HOME/bin/startup.sh" >/dev/null

wait_for_app
say "running at $BASE_URL"
echo "dev logins (database/sample_data.sql): admin@campus.test / Admin@12345, students / Student@123"
echo "logs: $CATALINA_HOME/logs/catalina.out   stop: ./run.sh stop"

if [ "$cmd" = "smoke" ]; then
    say "smoke test"
    BASE_URL="$BASE_URL" bash "$ROOT/scripts/smoke.sh"
    case "$BASE_URL" in
        *//localhost[:/]*|*//127.0.0.1[:/]*)
            say "removing smoke-test data"
            bash "$ROOT/scripts/clean-smoke.sh" ;;
        *) echo "note: smoke data left behind on $BASE_URL (not a local database)" ;;
    esac
fi
