#!/usr/bin/env bash
# Preflight for a release: refuses SNAPSHOT versions, tag/POM mismatches and a README that still advertises a snapshot.
# Usage: bin/release-check.sh [TAG]   (prints version=<v> as the last stdout line on success)
# Requires: mvn, JAVA_HOME pointing at a JDK 21+. RELEASE_CHECK_ROOT overrides the project root.
set -euo pipefail

ROOT="${RELEASE_CHECK_ROOT:-$(cd "$(dirname "$0")/.." && pwd)}"
TAG="${1:-}"

VERSION="$(mvn -q -f "${ROOT}/pom.xml" org.apache.maven.plugins:maven-help-plugin:3.5.1:evaluate -Dexpression=project.version -DforceStdout)" || VERSION=""
if [[ -z "${VERSION}" ]]; then
  echo "::error::Could not read the project version with Maven."
  exit 1
fi

fail=0
if [[ "${VERSION}" == *-SNAPSHOT ]]; then
  echo "::error::Refusing to release a SNAPSHOT version (${VERSION})."
  fail=1
fi
if [[ -n "${TAG}" && "${TAG}" != "v${VERSION}" ]]; then
  echo "::error::Tag ${TAG} does not match the POM version ${VERSION}."
  fail=1
fi
if [[ ! -f "${ROOT}/README.md" ]]; then
  echo "::error::README.md not found."
  fail=1
elif grep -qE -- '-SNAPSHOT|No artifact is on Maven Central yet' "${ROOT}/README.md"; then
  echo "::error::README.md still advertises a snapshot."
  fail=1
fi
ATTRS="${ROOT}/docs/modules/ROOT/pages/includes/attributes.adoc"
if [[ -f "${ATTRS}" ]] && grep -q -- '-SNAPSHOT' "${ATTRS}"; then
  echo "::error::docs/modules/ROOT/pages/includes/attributes.adoc still says -SNAPSHOT."
  fail=1
fi
# versions:set can miss profile-only modules, so scan every POM rather than trusting it.
while IFS= read -r pom; do
  echo "::error::${pom} still declares a -SNAPSHOT version."
  fail=1
done < <(grep -rl --include=pom.xml --exclude-dir=target -- '-SNAPSHOT' "${ROOT}" || true)
[[ "${fail}" -eq 0 ]] || exit 1

echo "version=${VERSION}"
