#!/bin/bash
set -euo pipefail

files=()
while IFS='' read -r file
do
  files+=("$file")
done < <(git grep -Il '')

echo "::add-matcher::.github/eslint-compact.json"

exit_status=0
for file in "${files[@]}"
do
  if [[ -n $(tail -c1 "$file") ]]
  then
    lineno=$(wc -l "$file" | awk '{ print $1 }')
    (( lineno += 1 ))
    printf '%s: line %s, col 0, Error - No newline at EOF. (no-newline-at-eof)\n' "$file" "$lineno"
    exit_status=1
  fi
done
exit $exit_status
