#!/usr/bin/env bash
# One-time local setup: create the MySQL database/user, load schema + sample data,
# and write Tomcat's bin/setenv.sh with the connection settings.
#
# Run this from an interactive terminal (sudo asks for your password):
#   bash scripts/setup-db.sh
#
# The password is read without echoing and is written only to
# $CATALINA_HOME/bin/setenv.sh — never into this repository.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DB_NAME="campus_exchange"
DB_USER="campus_app"
CATALINA_HOME="${CATALINA_HOME:-$HOME/tools/apache-tomcat-10.1.55}"

[[ -d "$CATALINA_HOME" ]] || {
    echo "Tomcat not found at $CATALINA_HOME — set CATALINA_HOME and retry." >&2
    exit 1
}

read -r -s -p "Choose a password for MySQL user '$DB_USER': " DB_PASSWORD
echo
[[ "$DB_PASSWORD" =~ ^[A-Za-z0-9._-]{8,}$ ]] || {
    echo "Use at least 8 characters: letters, digits, dot, underscore, or hyphen." >&2
    exit 1
}

echo "Creating database and user (sudo)..."
sudo mysql <<SQL
CREATE DATABASE IF NOT EXISTS $DB_NAME
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '$DB_USER'@'localhost' IDENTIFIED BY '$DB_PASSWORD';
ALTER USER '$DB_USER'@'localhost' IDENTIFIED BY '$DB_PASSWORD';
GRANT ALL PRIVILEGES ON $DB_NAME.* TO '$DB_USER'@'localhost';
FLUSH PRIVILEGES;
SQL

echo "Loading schema and sample data..."
mysql -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < "$ROOT/database/schema.sql"
mysql -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < "$ROOT/database/sample_data.sql"

echo "Writing $CATALINA_HOME/bin/setenv.sh..."
cat > "$CATALINA_HOME/bin/setenv.sh" <<SETENV
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export DB_URL='jdbc:mysql://localhost:3306/$DB_NAME?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
export DB_USER='$DB_USER'
export DB_PASSWORD='$DB_PASSWORD'
export UPLOAD_DIR="\$HOME/campus-exchange/uploads"
export BACKUP_DIR="\$HOME/campus-exchange/backups"
SETENV
chmod +x "$CATALINA_HOME/bin/setenv.sh"
mkdir -p "$HOME/campus-exchange/uploads" "$HOME/campus-exchange/backups"

echo "Done. Build and deploy with: mvn clean package && cp target/campus-resource-exchange.war $CATALINA_HOME/webapps/"
