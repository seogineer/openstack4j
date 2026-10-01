#!/usr/bin/env bash
# Checks the packaged distribution artifacts that Maven Central users get. Run after `./mvnw install`.
set -eu
cd "$(dirname "$0")/../.."
fail=0
check() { if eval "$2"; then echo "ok   - $1"; else echo "FAIL - $1"; fail=1; fi; }
D=distribution/target
WITHDEPS=$(ls $D/openstack4j-*-withdeps.jar 2>/dev/null | grep -v -- -sources | head -1 || true)
check "distribution sources jar exists (Central requires one)" "[ -f $D/openstack4j-sources.jar ]"
check "distribution withdeps jar contains OSFactory" "unzip -l $WITHDEPS 2>/dev/null | grep -q 'org/openstack4j/openstack/OSFactory.class'"
check "distribution withdeps jar contains the httpclient connector" "unzip -l $WITHDEPS 2>/dev/null | grep -q 'org/openstack4j/connectors/httpclient/HttpExecutorServiceImpl.class'"
check "core POM keeps guava dependency" "grep -q '<artifactId>guava</artifactId>' ~/.m2/repository/io/github/seogineer/openstack4j-core/*/openstack4j-core-*.pom"
exit $fail
