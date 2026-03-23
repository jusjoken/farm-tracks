#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   ./deploy/sql/run_parent_migration.sh --env-file .env.prod
#   ./deploy/sql/run_parent_migration.sh --env-file .env.prod --dry-run
#
# Requires env file keys:
#   DM_DB_URL=jdbc:mariadb://host:port/database?...
#   DM_DB_USER=...
#   DM_DB_PASSWORD=...

ENV_FILE=""
DRY_RUN=false
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_FILE="$SCRIPT_DIR/migrate_legacy_parent_ext_names.sql"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --env-file)
      ENV_FILE="${2:-}"
      shift 2
      ;;
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 1
      ;;
  esac
done

if [[ -z "$ENV_FILE" ]]; then
  echo "Missing required --env-file <path>" >&2
  exit 1
fi

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Env file not found: $ENV_FILE" >&2
  exit 1
fi

if [[ ! -f "$SQL_FILE" ]]; then
  echo "SQL file not found: $SQL_FILE" >&2
  exit 1
fi

get_env_value() {
  local key="$1"
  local file="$2"
  local line
  line="$(grep -E "^[[:space:]]*${key}=" "$file" | tail -n 1 || true)"
  if [[ -z "$line" ]]; then
    echo ""
    return 0
  fi

  line="${line#*=}"
  line="${line%%[[:space:]]#*}"

  if [[ "$line" =~ ^\".*\"$ ]]; then
    line="${line:1:${#line}-2}"
  elif [[ "$line" =~ ^\'.*\'$ ]]; then
    line="${line:1:${#line}-2}"
  fi

  printf '%s' "$line"
}

DM_DB_URL="$(get_env_value "DM_DB_URL" "$ENV_FILE")"
DM_DB_USER="$(get_env_value "DM_DB_USER" "$ENV_FILE")"
DM_DB_PASSWORD="$(get_env_value "DM_DB_PASSWORD" "$ENV_FILE")"

: "${DM_DB_URL:?DM_DB_URL is required in env file}"
: "${DM_DB_USER:?DM_DB_USER is required in env file}"
: "${DM_DB_PASSWORD:?DM_DB_PASSWORD is required in env file}"

# Parse jdbc:mariadb://host:port/database?params
jdbc_no_prefix="${DM_DB_URL#jdbc:mariadb://}"
host_port_db="${jdbc_no_prefix%%\?*}"
DB_HOSTPORT="${host_port_db%%/*}"
DB_NAME="${host_port_db#*/}"

DB_HOST="${DB_HOSTPORT%%:*}"
DB_PORT="${DB_HOSTPORT##*:}"
if [[ "$DB_PORT" == "$DB_HOSTPORT" ]]; then
  DB_PORT="3306"
fi

if [[ "$DRY_RUN" == true ]]; then
  echo "Dry run"
  echo "  Host: $DB_HOST"
  echo "  Port: $DB_PORT"
  echo "  DB:   $DB_NAME"
  echo "  User: $DM_DB_USER"
  echo "  SQL:  $SQL_FILE"
  exit 0
fi

export MYSQL_PWD="$DM_DB_PASSWORD"

mysql \
  --host="$DB_HOST" \
  --port="$DB_PORT" \
  --user="$DM_DB_USER" \
  --database="$DB_NAME" \
  --default-character-set=utf8mb4 \
  --show-warnings \
  < "$SQL_FILE"

echo "Migration complete on $DB_NAME@$DB_HOST:$DB_PORT"
