#!/usr/bin/env bash
# Verifies a Maven staging directory holds exactly the expected release artifacts, complete and signed.
# Usage: bin/check-staging.sh STAGING_DIR [--unsigned]
set -euo pipefail

EXPECTED=(quarkus-seo-parent quarkus-seo quarkus-seo-deployment)

STAGING="${1:?usage: check-staging.sh STAGING_DIR [--unsigned]}"
SIGNED=1
[[ "${2:-}" == "--unsigned" ]] && SIGNED=0
BASE="${STAGING}/org/sitenetsoft"

errors=0
err() {
  echo "::error::$*"
  errors=$((errors + 1))
}

if [[ ! -d "${BASE}" ]]; then
  err "missing artifact directory ${BASE}"
  exit 1
fi

if [[ -n "$(find "${STAGING}" -name 'maven-metadata.xml*' -print -quit)" ]]; then
  while IFS= read -r f; do err "maven-metadata present: ${f}"; done < <(find "${STAGING}" -name 'maven-metadata.xml*')
fi

is_expected() {
  local e
  for e in "${EXPECTED[@]}"; do [[ "$e" == "$1" ]] && return 0; done
  return 1
}

for dir in "${BASE}"/*/; do
  id="$(basename "${dir}")"
  is_expected "${id}" || err "unexpected artifact ${id}"
done

versions=()
for id in "${EXPECTED[@]}"; do
  dir="${BASE}/${id}"
  if [[ ! -d "${dir}" ]]; then
    err "missing artifact ${id}"
    continue
  fi
  vdirs=("${dir}"/*/)
  if [[ "${#vdirs[@]}" -ne 1 || ! -d "${vdirs[0]}" ]]; then
    err "${id}: expected exactly one version directory"
    continue
  fi
  vdir="${vdirs[0]%/}"
  versions+=("$(basename "${vdir}")")

  files=()
  poms=("${vdir}/${id}"-*.pom)
  [[ -e "${poms[0]}" ]] || err "${id}: missing pom"
  [[ -e "${poms[0]}" ]] && files+=("${poms[@]}")

  if [[ "${id}" != *-parent ]]; then
    mains=()
    for j in "${vdir}/${id}"-*.jar; do
      [[ -e "$j" && "$j" != *-sources.jar && "$j" != *-javadoc.jar ]] && mains+=("$j")
    done
    [[ "${#mains[@]}" -gt 0 ]] || err "${id}: missing main jar"
    srcs=("${vdir}/${id}"-*-sources.jar)
    [[ -e "${srcs[0]}" ]] || err "${id}: missing sources jar"
    docs=("${vdir}/${id}"-*-javadoc.jar)
    [[ -e "${docs[0]}" ]] || err "${id}: missing javadoc jar"
    [[ "${#mains[@]}" -gt 0 ]] && files+=("${mains[@]}")
    [[ -e "${srcs[0]}" ]] && files+=("${srcs[@]}")
    [[ -e "${docs[0]}" ]] && files+=("${docs[@]}")
  fi

  if [[ "${SIGNED}" -eq 1 ]]; then
    for f in "${files[@]}"; do
      [[ -e "${f}.asc" ]] || err "missing signature ${f}.asc"
    done
  fi
done

if [[ "${#versions[@]}" -gt 0 ]] && [[ "$(printf '%s\n' "${versions[@]}" | sort -u | wc -l)" -ne 1 ]]; then
  err "mixed versions: $(printf '%s\n' "${versions[@]}" | sort -u | paste -sd' ')"
fi

if [[ "${errors}" -gt 0 ]]; then
  echo "${errors} problem(s) found in ${STAGING}"
  exit 1
fi
echo "Staging layout OK (${#EXPECTED[@]} artifacts, version ${versions[0]})"
