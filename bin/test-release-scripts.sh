#!/usr/bin/env bash
# Test harness for release-check.sh and check-staging.sh. Fixtures are rebuilt under target/release-script-tests/.
# Requires: mvn, JAVA_HOME pointing at a JDK 21+.
set -uo pipefail

BASE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
FIX_DIR="${BASE_DIR}/target/release-script-tests"
BIN="${BASE_DIR}/bin"

IDS=(quarkus-seo-parent quarkus-seo quarkus-seo-deployment)

OUT=""
RC=0
passed=0
failed=0

# fixture <case> <pom-version> <readme-text>: creates the project-root fixture and prints its path
fixture() {
  local dir="${FIX_DIR}/$1"
  mkdir -p "${dir}"
  cat >"${dir}/pom.xml" <<POM
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>org.sitenetsoft</groupId>
  <artifactId>fixture</artifactId>
  <version>$2</version>
  <packaging>pom</packaging>
</project>
POM
  printf '%s\n' "$3" >"${dir}/README.md"
  mkdir -p "${dir}/docs/modules/ROOT/pages/includes"
  echo ":project-version: $2" >"${dir}/docs/modules/ROOT/pages/includes/attributes.adoc"
  echo "${dir}"
}

# staging <case> <version-dir> <name-version> <signed>: builds a complete staging layout and prints its path
staging() {
  local dir="${FIX_DIR}/$1/staging" id d
  mkdir -p "${dir}"
  for id in "${IDS[@]}"; do
    d="${dir}/org/sitenetsoft/${id}/$2"
    mkdir -p "${d}"
    touch "${d}/${id}-$3.pom"
    if [[ "${id}" != *-parent ]]; then
      touch "${d}/${id}-$3.jar" "${d}/${id}-$3-sources.jar" "${d}/${id}-$3-javadoc.jar"
    fi
    if [[ "$4" == signed ]]; then
      local f
      for f in "${d}"/*.pom "${d}"/*.jar; do touch "${f}.asc"; done
    fi
  done
  echo "${dir}"
}

run() {
  OUT="$("$@" 2>&1)"
  RC=$?
}

# check <case> <expected-rc> <substring-or-empty>
check() {
  if [[ "${RC}" -ne "$2" ]]; then
    echo "FAIL $1: expected exit $2, got ${RC}"
    echo "${OUT}" | sed 's/^/    /'
    failed=$((failed + 1))
  elif [[ -n "$3" && "${OUT}" != *"$3"* ]]; then
    echo "FAIL $1: output lacks '$3'"
    echo "${OUT}" | sed 's/^/    /'
    failed=$((failed + 1))
  else
    echo "ok   $1"
    passed=$((passed + 1))
  fi
}

release_check_ok() {
  local r; r="$(fixture "$FUNCNAME" 0.1.0 'Use 0.1.0 from Central.')"
  RELEASE_CHECK_ROOT="$r" run "${BIN}/release-check.sh" v0.1.0
  [[ "$(echo "${OUT}" | tail -n 1)" == "version=0.1.0" ]] || RC=99
  check "$FUNCNAME" 0 ""
}

release_check_snapshot_refused() {
  local r; r="$(fixture "$FUNCNAME" 0.1.0-SNAPSHOT 'Use it.')"
  RELEASE_CHECK_ROOT="$r" run "${BIN}/release-check.sh" v0.1.0-SNAPSHOT
  check "$FUNCNAME" 1 "Refusing to release a SNAPSHOT"
}

release_check_tag_mismatch() {
  local r; r="$(fixture "$FUNCNAME" 0.1.0 'Use it.')"
  RELEASE_CHECK_ROOT="$r" run "${BIN}/release-check.sh" v0.2.0
  check "$FUNCNAME" 1 "does not match"
}

release_check_readme_snapshot() {
  local r; r="$(fixture "$FUNCNAME" 0.1.0 'Depend on 0.1.0-SNAPSHOT.')"
  RELEASE_CHECK_ROOT="$r" run "${BIN}/release-check.sh" v0.1.0
  check "$FUNCNAME" 1 "README.md still"
}

release_check_no_tag() {
  local r; r="$(fixture "$FUNCNAME" 0.1.0 'Use it.')"
  RELEASE_CHECK_ROOT="$r" run "${BIN}/release-check.sh"
  check "$FUNCNAME" 0 "version=0.1.0"
}

release_check_attributes_snapshot() {
  local r; r="$(fixture "$FUNCNAME" 0.1.0 'Use it.')"
  echo ':project-version: 0.1.0-SNAPSHOT' >"${r}/docs/modules/ROOT/pages/includes/attributes.adoc"
  RELEASE_CHECK_ROOT="$r" run "${BIN}/release-check.sh" v0.1.0
  check "$FUNCNAME" 1 "attributes.adoc still"
}

release_check_nested_pom_snapshot() {
  local r; r="$(fixture "$FUNCNAME" 0.1.0 'Use it.')"
  mkdir -p "${r}/sub"
  printf '<project><version>0.1.0-SNAPSHOT</version></project>\n' >"${r}/sub/pom.xml"
  RELEASE_CHECK_ROOT="$r" run "${BIN}/release-check.sh" v0.1.0
  check "$FUNCNAME" 1 "still declares"
}

release_check_missing_readme() {
  local r; r="$(fixture "$FUNCNAME" 0.1.0 'Use it.')"
  rm "${r}/README.md"
  RELEASE_CHECK_ROOT="$r" run "${BIN}/release-check.sh" v0.1.0
  check "$FUNCNAME" 1 "README.md not found"
}

release_check_no_pom() {
  local r; r="$(fixture "$FUNCNAME" 0.1.0 'Use it.')"
  rm "${r}/pom.xml"
  RELEASE_CHECK_ROOT="$r" run "${BIN}/release-check.sh" v0.1.0
  check "$FUNCNAME" 1 "Could not read"
}

release_check_readme_status_sentence() {
  local r; r="$(fixture "$FUNCNAME" 0.1.0 'Status: pre-release. No artifact is on Maven Central yet.')"
  RELEASE_CHECK_ROOT="$r" run "${BIN}/release-check.sh" v0.1.0
  check "$FUNCNAME" 1 "README.md still"
}

staging_ok() {
  local s; s="$(staging "$FUNCNAME" 0.1.0 0.1.0 signed)"
  run "${BIN}/check-staging.sh" "$s"
  check "$FUNCNAME" 0 ""
}

staging_unsigned_flag() {
  local s; s="$(staging "$FUNCNAME" 0.1.0 0.1.0 unsigned)"
  run "${BIN}/check-staging.sh" "$s" --unsigned
  check "$FUNCNAME" 0 ""
}

staging_missing_signature() {
  local s; s="$(staging "$FUNCNAME" 0.1.0 0.1.0 signed)"
  rm "${s}/org/sitenetsoft/quarkus-seo/0.1.0/quarkus-seo-0.1.0.jar.asc"
  run "${BIN}/check-staging.sh" "$s"
  check "$FUNCNAME" 1 "quarkus-seo-0.1.0.jar.asc"
}

staging_missing_javadoc() {
  local s; s="$(staging "$FUNCNAME" 0.1.0 0.1.0 signed)"
  rm "${s}/org/sitenetsoft/quarkus-seo-deployment/0.1.0/quarkus-seo-deployment-0.1.0-javadoc.jar"
  run "${BIN}/check-staging.sh" "$s"
  check "$FUNCNAME" 1 "javadoc"
}

staging_metadata_present() {
  local s; s="$(staging "$FUNCNAME" 0.1.0 0.1.0 signed)"
  touch "${s}/org/sitenetsoft/quarkus-seo/maven-metadata.xml"
  run "${BIN}/check-staging.sh" "$s"
  check "$FUNCNAME" 1 "maven-metadata"
}

staging_extra_artifact() {
  local s; s="$(staging "$FUNCNAME" 0.1.0 0.1.0 signed)"
  mkdir -p "${s}/org/sitenetsoft/quarkus-seo-integration-tests/0.1.0"
  touch "${s}/org/sitenetsoft/quarkus-seo-integration-tests/0.1.0/quarkus-seo-integration-tests-0.1.0.pom"
  run "${BIN}/check-staging.sh" "$s"
  check "$FUNCNAME" 1 "unexpected"
}

staging_missing_artifact() {
  local s; s="$(staging "$FUNCNAME" 0.1.0 0.1.0 signed)"
  rm -r "${s}/org/sitenetsoft/quarkus-seo-deployment"
  run "${BIN}/check-staging.sh" "$s"
  check "$FUNCNAME" 1 "missing"
}

staging_snapshot_names() {
  local s; s="$(staging "$FUNCNAME" 0.1.0-SNAPSHOT 0.1.0-20261021.033054-1 signed)"
  run "${BIN}/check-staging.sh" "$s"
  check "$FUNCNAME" 0 ""
}

rm -rf "${FIX_DIR}"
mkdir -p "${FIX_DIR}"

for t in release_check_ok release_check_snapshot_refused release_check_tag_mismatch release_check_readme_snapshot \
  release_check_no_tag release_check_attributes_snapshot release_check_nested_pom_snapshot \
  release_check_missing_readme release_check_no_pom release_check_readme_status_sentence staging_ok staging_unsigned_flag staging_missing_signature staging_missing_javadoc \
  staging_metadata_present staging_extra_artifact staging_missing_artifact staging_snapshot_names; do
  "${t}"
done

echo "${passed} passed, ${failed} failed"
[[ "${failed}" -eq 0 ]]
