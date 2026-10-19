#!/bin/bash
set -euo pipefail

MAX_LINES=800

excludes=(
  'detekt.yml'
)

check_if_valid() {
  for pattern in "${excludes[@]}"
  do
    [[ $1 =~ $pattern ]] && return 1
  done
  return 0
}

files=()
while IFS='' read -r file
do
  if check_if_valid "$file"
  then
    files+=("$file")
  fi
done < <(git grep -Il '')

echo "::add-matcher::.github/eslint-compact.json"

exit_status=0
for file in "${files[@]}"
do
  line_count=$(wc -l "$file" | awk '{ print $1 }')
  ((line_count += 1))
  if (( line_count > MAX_LINES ))
  then
    printf '%s: line %d, col 0, Error - File has %d lines, exceeds maximum of %d lines. (file-too-large)\n' "$file" "$line_count" "$line_count" "$MAX_LINES"
    exit_status=1
  fi
done
exit $exit_status
