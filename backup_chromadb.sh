#!/usr/bin/env bash
set -euo pipefail

backup_dir="/wp/PMS_IC/backup"
mkdir -p "$backup_dir"

timestamp="$(date +%Y%m%d_%H%M%S)"
container_id="$(docker-compose -f /wp/PMS_IC/docker-compose.yml ps -q chromadb)"

if [[ -z "$container_id" ]]; then
  echo "chromadb container not running" >&2
  exit 1
fi

tmp_file="$backup_dir/chroma.sqlite3.$timestamp"
backup_file="$backup_dir/chroma.sqlite3.$timestamp.tgz"

# Copy sqlite out of container, then compress
rm -f "$tmp_file" "$backup_file"
docker cp "$container_id":/data/chroma.sqlite3 "$tmp_file"
tar czf "$backup_file" -C "$backup_dir" "$(basename "$tmp_file")"
rm -f "$tmp_file"

echo "Backup created: $backup_file"
