# openstack4j 후속 포크 A (기반 및 최신화) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 원본 openstack4j 를 JDK 17+ / Spring Boot 3 환경에서 쓸 수 있게 최신화하고, `io.github.seogineer` 좌표로 Maven Central 에 4.0.0 을 배포한다.

**Architecture:** 원본 Maven 멀티모듈 구조(core, core-test, connectors, core-integration-test, distribution)를 유지한 채, 작업마다 브랜치 → PR → CI 통과 → main 머지를 반복한다. 각 작업이 끝난 main 은 항상 빌드가 통과한다. connector 는 httpclient(HttpClient 5), okhttp(OkHttp 4), http-connector(JDK `java.net.http.HttpClient`) 세 개만 남긴다.

**Tech Stack:** Java 17, Maven 3.9 (Maven Wrapper), TestNG 7, OkHttp MockWebServer, Groovy/Spock/Betamax(통합 테스트), GitHub Actions, Sonatype Central Portal, GPG.

**Spec:** `docs/superpowers/specs/2026-10-01-openstack4j-fork-foundation-design.md` (개정 1)

## Global Constraints

- 저장소: `github.com/seogineer/openstack4j`, 로컬 `~/IdeaProjects/openstack4j`. `origin` = 포크, `upstream` = `openstack4j/openstack4j`.
- Java 패키지 `org.openstack4j.*` 는 **바꾸지 않는다**.
- groupId 는 모든 모듈 `io.github.seogineer` 단일. artifactId 는 원본 유지.
- 최소 JDK 17 (`maven.compiler.release=17`). Maven 3.9 이상.
- 버전: main 은 `4.0.0-SNAPSHOT`, 첫 정식 릴리스 `4.0.0`, 이후 SemVer.
- 단위 테스트는 TestNG 유지. JUnit 5 는 스모크 프로젝트(Spring Boot 기본)에서만 쓴다.
- 회사(이노그리드) 코드, 특히 `~/IdeaProjects/openstackit-java` 의 어떤 코드도 복사하지 않는다.
- 라이선스 Apache 2.0. `LICENSE` 유지, `NOTICE` 에 원저작자 표기.
- 의존성 버전(2026-10-01 조회 기준): Jackson 2.22.3, Guava 33.7.2-jre, SnakeYAML 2.7, SLF4J 2.0.20, json-patch(java-json-tools) 1.13, jsr305 3.0.2, httpclient5 5.6.4, OkHttp 4.12.0, TestNG 7.12.0, Mockito 5.24.0, Spring Boot(스모크) 3.5.16.
- 플러그인 버전: compiler 3.16.0, surefire 3.6.0, jar 3.5.1, source 3.4.0, javadoc 3.12.0, gpg 3.2.8, shade 3.6.2, enforcer 3.6.3, deploy 3.2.0, install 3.2.0, resources 3.5.0, felix bundle 6.2.0, versions 2.22.0, central-publishing 0.11.0, maven-wrapper 3.3.4, Maven 3.9.11.
- 모든 커밋 메시지 끝에 `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. PR 본문 끝에 `🤖 Generated with [Claude Code](https://claude.com/claude-code)`.
- **🛑 표시 단계는 사용자 확인 없이 진행하지 않는다**: PR 머지, tag push(= Maven Central 배포, 되돌릴 수 없음).

## Review Focus

1. **PATCH 요청** (Keystone 사용자·도메인 수정, Glance v2 이미지 수정) — 세 connector 모두 JDK 17+ 에서 PATCH 를 그대로 보내야 한다. → Task 7·8 의 `patchIsSentWithBody` 테스트.
2. **호출자가 넣은 `Content-Length` 헤더** (Swift `createPath` 가 `Content-Length: 0` 을 직접 넣는다) — HttpClient 5 는 "header already present" 로, JDK HttpClient 는 "restricted header" 로 예외를 던질 수 있다. 요청은 성공해야 한다. → Task 7·8 의 `callerSuppliedContentLengthHeaderIsIgnored`.
3. **사용자 지정 `SSLContext`** (`Config.withSSLContext`) — OkHttp 의 인자 1개짜리 `sslSocketFactory()` 는 JDK 9+ 에서 `UnsupportedOperationException` 을 던진다. 사설 인증서 OpenStack 에서 바로 터진다. → Task 9 의 `customSslContextDoesNotThrow`.
4. **쿼리 파라미터 인코딩** (공백, 같은 키 반복 `?tag=x&tag=y`) — connector 를 바꿔도 서버가 받는 값이 같아야 한다. → Task 7·8 의 `queryParamsAreEncoded`.
5. **Spring Boot 가 Jackson 을 낮은 버전으로 고정하는 환경** (Boot 3.5.16 은 Jackson 2.21.4 로 고정, 라이브러리는 2.22.3 으로 컴파일) — 토큰 역직렬화가 깨지면 안 된다. → Task 11 의 `keystoneTokenDeserializesWithBootManagedJackson`.

---

## 실행 전 준비 (1회)

- [ ] **P1: 설계·계획 문서 PR 머지**

```bash
cd ~/IdeaProjects/openstack4j
gh repo set-default seogineer/openstack4j
git switch docs/foundation-spec
git add docs/superpowers
git commit -m "docs: add fork foundation (A) implementation plan

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git push -u origin docs/foundation-spec
gh pr create -R seogineer/openstack4j --base main --head docs/foundation-spec \
  --title "docs: fork foundation (A) design spec and plan" \
  --body "하위 프로젝트 A 의 설계 문서와 구현 계획.

🤖 Generated with [Claude Code](https://claude.com/claude-code)"
```

`-R seogineer/openstack4j --base main` 을 반드시 붙인다. 붙이지 않으면 gh 가 원본(`openstack4j/openstack4j`)으로 PR 을 연다.

🛑 사용자 확인 후: `gh pr merge -R seogineer/openstack4j --squash --delete-branch`

- [ ] **P2: Maven 준비 (Task 1 전용)**

Task 2 에서 Maven Wrapper 를 추가하기 전까지는 임시 Maven 을 쓴다.

```bash
export MVN_HOME="$HOME/.cache/openstack4j-maven/apache-maven-3.9.11"
[ -x "$MVN_HOME/bin/mvn" ] || { mkdir -p "$(dirname "$MVN_HOME")" && curl -sfL https://archive.apache.org/dist/maven/maven-3/3.9.11/binaries/apache-maven-3.9.11-bin.tar.gz | tar xz -C "$(dirname "$MVN_HOME")"; }
"$MVN_HOME/bin/mvn" -v   # Expected: Apache Maven 3.9.11, Java version: 17.x
```

로컬 JDK: 17 은 기본(`java -version`), 21 은 `~/.jdks/ms-21.0.12`.

### 공통 PR 절차 (모든 Task 의 마지막 단계에서 참조)

```bash
git push -u origin HEAD
gh pr create -R seogineer/openstack4j --base main --fill-first \
  --body "<이 Task 의 요약>

🤖 Generated with [Claude Code](https://claude.com/claude-code)"
gh pr checks -R seogineer/openstack4j --watch     # 모든 체크가 pass 여야 한다
```

🛑 사용자 확인 후: `gh pr merge -R seogineer/openstack4j --squash --delete-branch && git switch main && git pull`

각 Task 는 `git switch main && git pull && git switch -c <브랜치명>` 으로 시작한다.

---

### Task 1: jersey2·resteasy 제거와 JDK 17 빌드 복구

원본 main 은 JDK 17 에서 `it-jersey2` 가 빌드에서 빠진 `openstack4j-jersey2` 를 찾지 못해 실패한다. 이 Task 가 끝나면 JDK 17 에서 전체 빌드가 통과한다.

**Files:**
- Delete: `connectors/jersey2/`, `connectors/resteasy/`, `core-integration-test/it-jersey2/`, `core-integration-test/it-resteasy/`
- Modify: `connectors/pom.xml` (profiles → modules), `distribution/pom.xml` (resteasy → httpclient), `core-integration-test/pom.xml` (modules, exclusion)

**Interfaces:**
- Produces: 리액터 모듈 = core, core-test, connectors(httpclient, okhttp), core-integration-test(it-okhttp, it-httpclient), distribution. `http-connector` 는 Task 8 까지 리액터에서 빠진다(원본도 JDK 17+ 에서 제외했다).

- [ ] **Step 1: 실패하는 기준선 확인**

```bash
git switch main && git pull && git switch -c task/01-remove-jersey-resteasy
"$MVN_HOME/bin/mvn" -B --no-transfer-progress verify 2>&1 | tail -20
```
Expected: `BUILD FAILURE`, `Could not find artifact com.github.openstack4j.core.connectors:openstack4j-jersey2`

- [ ] **Step 2: 모듈 디렉터리 삭제**

```bash
git rm -r -q connectors/jersey2 connectors/resteasy core-integration-test/it-jersey2 core-integration-test/it-resteasy
```

- [ ] **Step 3: `connectors/pom.xml` 의 `<profiles>...</profiles>` 블록 전체를 아래로 교체**

```xml
    <modules>
        <module>httpclient</module>
        <module>okhttp</module>
        <!-- http-connector 는 JDK 17+ 에서 PATCH 가 동작하지 않아 제외. JDK HttpClient 로 재구현 후 다시 넣는다. -->
    </modules>
```

- [ ] **Step 4: `distribution/pom.xml` 의 resteasy 의존성을 httpclient 로 교체**

```xml
        <dependency>
            <groupId>com.github.openstack4j.core.connectors</groupId>
            <artifactId>openstack4j-httpclient</artifactId>
            <version>${project.version}</version>
        </dependency>
```
(기존 `<artifactId>openstack4j-resteasy</artifactId>` 블록을 지우고 이것으로 바꾼다.)

- [ ] **Step 5: `core-integration-test/pom.xml` 수정**

`<modules>` 에서 `it-jersey2`, `it-resteasy` 두 줄을 지운다. `openstack4j` 의존성의 exclusion 은 대상만 바꾼다. distribution 이 이제 httpclient 를 끌고 오므로, 빼지 않으면 it-okhttp 가 실제로는 httpclient 로 테스트된다.

```xml
            <exclusions>
                <exclusion> <!-- distribution 이 끌고 오는 기본 connector 를 빼서, 각 it-* 모듈이 자기 connector 로만 테스트되게 한다 -->
                    <groupId>com.github.openstack4j.core.connectors</groupId>
                    <artifactId>openstack4j-httpclient</artifactId>
                </exclusion>
            </exclusions>
```

- [ ] **Step 6: 빌드와 테스트 실행**

```bash
"$MVN_HOME/bin/mvn" -B --no-transfer-progress install 2>&1 | tee /tmp/task1.log | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
```
Expected: `BUILD SUCCESS`. 요약 줄이 `Tests run: 17`(core) 1개, `Tests run: 676` 2개(httpclient, okhttp), `Tests run: 29` 2개(it-okhttp, it-httpclient), 모두 `Failures: 0, Errors: 0`.

- [ ] **Step 7: it-okhttp 가 okhttp 로만 테스트되는지 확인**

```bash
"$MVN_HOME/bin/mvn" -B -q dependency:tree -pl core-integration-test/it-okhttp -Dincludes=com.github.openstack4j.core.connectors
```
Expected: `openstack4j-okhttp` 만 나오고 `openstack4j-httpclient` 는 없다.

- [ ] **Step 8: 커밋과 PR**

```bash
git add -A
git commit -m "build: remove jersey2 and resteasy connectors

Both depend on javax.ws.rs and conflict with Jakarta-based Spring Boot 3.
Removing it-jersey2 also fixes the JDK 17 build, which failed because the
jersey2 connector was excluded on JDK 17+ while its IT module was not.
The distribution artifact now bundles the httpclient connector.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
공통 PR 절차를 따른다. 원본 CI(JDK 11/17/21, push 트리거)가 돈다. 이 시점에는 JDK 11 도 통과해야 한다.

---

### Task 2: 포크 정리 (README, NOTICE, Maven Wrapper)

**Files:**
- Create: `NOTICE`, `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`
- Modify: `README.md` (맨 위)
- Delete: `.travis.yml`, `release.sh`

- [ ] **Step 1: Maven Wrapper 생성**

```bash
git switch main && git pull && git switch -c task/02-fork-housekeeping
"$MVN_HOME/bin/mvn" -B -N org.apache.maven.plugins:maven-wrapper-plugin:3.3.4:wrapper -Dmaven=3.9.11
./mvnw -v
```
Expected: `Apache Maven 3.9.11`. `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties` 가 생긴다(only-script 방식이라 jar 는 없다).

- [ ] **Step 2: 쓰지 않는 파일 삭제**

```bash
git rm -q .travis.yml release.sh
```

- [ ] **Step 3: `NOTICE` 작성**

```text
OpenStack4j (seogineer fork)
Copyright 2026 seogineer

This product is a fork of OpenStack4j
(https://github.com/openstack4j/openstack4j),
Copyright the OpenStack4j contributors, originally created by Jeremy Unruh.

Licensed under the Apache License, Version 2.0. See LICENSE.
```

- [ ] **Step 4: `README.md` 맨 위 교체**

첫 줄 `OpenStack4j` 부터 Sonar 배지 줄(`[![Quality Gate Status]...`)까지를 아래로 바꾼다. 나머지 본문은 Task 12 에서 다시 쓴다.

```markdown
OpenStack4j (seogineer fork)
============================

[![CI](https://github.com/seogineer/openstack4j/actions/workflows/ci.yaml/badge.svg)](https://github.com/seogineer/openstack4j/actions/workflows/ci.yaml)
[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](LICENSE)

> **이 저장소는 [openstack4j/openstack4j](https://github.com/openstack4j/openstack4j) 의 후속 포크입니다.**
> 원본은 2024-05 의 3.12 이후 개발이 멈췄습니다. 이 포크는 JDK 17+ / Spring Boot 3 지원과 최신 의존성을 목표로 하며,
> Java 패키지(`org.openstack4j`)는 그대로 두고 Maven 좌표만 `io.github.seogineer` 로 바꿉니다.
> 3.x 에서 옮겨 오는 방법은 [MIGRATION.md](MIGRATION.md) 를 보세요.
```

- [ ] **Step 5: Wrapper 로 전체 빌드**

```bash
./mvnw -B --no-transfer-progress verify 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
```
Expected: Task 1 Step 6 과 같은 결과, `BUILD SUCCESS`.

- [ ] **Step 6: 커밋과 PR**

```bash
git add -A
git commit -m "chore: mark repository as a successor fork and add Maven Wrapper

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
README 가 링크하는 `MIGRATION.md` 는 Task 12 에서 만든다. 공통 PR 절차를 따른다.

---

### Task 3: Maven 좌표 변경과 POM 메타데이터

**Files:**
- Modify: 모든 `pom.xml`, `README.md`, `connectors/README.md`, `core-integration-test/it-*/pom.xml`
- Modify: `pom.xml` (url, licenses, developers, scm, mailingLists)

**Interfaces:**
- Produces: 모든 모듈 groupId `io.github.seogineer`, 버전 `4.0.0-SNAPSHOT`. 이후 Task 는 이 좌표를 쓴다.

- [ ] **Step 1: 옛 좌표 잔존 검사를 먼저 실행 (실패 확인)**

```bash
git switch main && git pull && git switch -c task/03-coordinates
grep -rn 'com\.github\.openstack4j' --include=pom.xml --include='*.md' --include='*.xml' --include='*.groovy' . | grep -v -e '/target/' -e 'docs/superpowers/' -e 'CHANGELOG.md' | wc -l
```
Expected: 0 보다 큰 숫자(현재 잔존 건수).

- [ ] **Step 2: groupId 와 버전 일괄 치환**

하위 groupId(`.connectors`)를 먼저 바꿔야 상위 치환과 겹치지 않는다.

```bash
FILES=$(grep -rl -e 'com\.github\.openstack4j' -e '3\.13-SNAPSHOT' --include=pom.xml --include='*.md' . | grep -v -e '/target/' -e 'docs/superpowers/' -e 'CHANGELOG.md')
sed -i -e 's/com\.github\.openstack4j\.core\.connectors/io.github.seogineer/g' \
       -e 's/com\.github\.openstack4j\.core/io.github.seogineer/g' \
       -e 's/3\.13-SNAPSHOT/4.0.0-SNAPSHOT/g' $FILES
```

- [ ] **Step 3: 루트 `pom.xml` 메타데이터 교체**

`<url>` 부터 `</mailingLists>` 까지(url, licenses, developers, scm, mailingLists)를 아래로 바꾼다. `mailingLists` 는 원본 Google Groups 가 사라져 제거한다. 라이선스는 원본 POM 이 MIT 로 잘못 적어 둔 것을 LICENSE 파일과 맞춘다.

```xml
    <url>https://github.com/seogineer/openstack4j</url>
    <packaging>pom</packaging>
    <licenses>
        <license>
            <name>Apache License, Version 2.0</name>
            <url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>
            <distribution>repo</distribution>
        </license>
    </licenses>
    <developers>
        <developer>
            <id>seogineer</id>
            <name>DoGyeong Seo</name>
            <url>https://github.com/seogineer</url>
            <roles><role>maintainer</role></roles>
        </developer>
        <developer>
            <name>Jeremy Unruh</name>
            <url>http://www.pacesys.org</url>
            <roles><role>original author</role></roles>
        </developer>
    </developers>
    <scm>
        <connection>scm:git:https://github.com/seogineer/openstack4j.git</connection>
        <developerConnection>scm:git:git@github.com:seogineer/openstack4j.git</developerConnection>
        <url>https://github.com/seogineer/openstack4j</url>
        <tag>HEAD</tag>
    </scm>
```
(원래 `<packaging>pom</packaging>` 은 `<url>` 바로 뒤에 있었으므로 중복되지 않게 한 번만 남긴다.) 하위 모듈의 `<url>https://github.com/openstack4j/openstack4j/</url>` 줄은 모두 지운다(부모에서 상속).

```bash
sed -i '/<url>https:\/\/github.com\/openstack4j\/openstack4j\/<\/url>/d' $(git ls-files '*pom.xml' | grep -v '^pom.xml$')
```

- [ ] **Step 4: 잔존 검사 재실행**

Step 1 명령을 다시 실행한다.
Expected: `0`. 남은 것이 있으면 하나씩 열어 `io.github.seogineer` 로 고친다(README 배지 URL 은 Task 12 에서 다시 쓰므로 그대로 두지 말고 지금 고친다).

- [ ] **Step 5: 빌드와 좌표 확인**

```bash
./mvnw -B --no-transfer-progress install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
ls ~/.m2/repository/io/github/seogineer/
```
Expected: `BUILD SUCCESS`, 테스트 수는 Task 1 과 같다. 디렉터리에 `openstack4j`, `openstack4j-core`, `openstack4j-httpclient`, `openstack4j-okhttp`, `openstack4j-parent`, `openstack4j-connectors` 등이 있다.

- [ ] **Step 6: 커밋과 PR**

```bash
git add -A
git commit -m "build: change coordinates to io.github.seogineer 4.0.0-SNAPSHOT

Also fix the license declared in the POM (MIT -> Apache 2.0, matching
the LICENSE file) and point url/scm/developers at the fork.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
공통 PR 절차를 따른다.

---

### Task 4: JDK 17 기준 빌드, 플러그인 갱신, CI

**Files:**
- Modify: `pom.xml` (properties, build plugins, pluginManagement, distributionManagement·release profile 제거, Karaf features 제거)
- Modify: `connectors/pom.xml`, `core/pom.xml`, `distribution/pom.xml`, `core-test/pom.xml` (플러그인 버전 하드코딩 제거, jar `finalName` 제거)
- Delete: `src/main/resources/features.xml` (2014년 `org.pacesys` 좌표·Guava 17 기준으로 방치된 Karaf feature), `.github/workflows/release-management.yml` (존재하지 않는 `master` 브랜치를 트리거로 해서 한 번도 돌지 않음. 릴리스 노트는 Task 5 에서 처리)
- Modify: `.github/workflows/ci.yaml`

**Interfaces:**
- Produces: 루트 `pluginManagement` 에 모든 플러그인 버전. 하위 모듈은 버전을 적지 않는다. CI job 이름 `build (17)`, `build (21)`.

- [ ] **Step 1: JDK 요구사항 검사를 먼저 추가하고 JDK 11 에서 실패하는지 확인**

```bash
git switch main && git pull && git switch -c task/04-jdk17-build
```
루트 `pom.xml` 의 `maven-enforcer-plugin` 블록 전체를 아래로 바꾼다(버전은 Step 2 의 pluginManagement 로 옮긴다).

```xml
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-enforcer-plugin</artifactId>
                <executions>
                    <execution>
                        <id>enforce-build-environment</id>
                        <goals>
                            <goal>enforce</goal>
                        </goals>
                        <configuration>
                            <rules>
                                <requireJavaVersion>
                                    <version>[17,)</version>
                                </requireJavaVersion>
                                <requireMavenVersion>
                                    <version>[3.9,)</version>
                                </requireMavenVersion>
                            </rules>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
```
루트 `<pluginManagement><plugins>` 에 `<plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-enforcer-plugin</artifactId><version>3.6.3</version></plugin>` 한 줄을 먼저 넣고(Step 2 에서 나머지를 추가) 실행한다.

```bash
JAVA_HOME=~/.jdks/ms-11.0.27 ./mvnw -B -q validate -N 2>&1 | tail -3
```
Expected: `Rule 0: org.apache.maven.enforcer.rules.version.RequireJavaVersion failed` 로 실패.

- [ ] **Step 2: 루트 `pom.xml` properties 와 pluginManagement 교체**

`<properties>` 전체를 교체한다(`release.version`, `aries.spifly.version`, 플러그인 버전 3개 property 는 없앤다).

```xml
    <properties>
        <maven.compiler.release>17</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <slf4j.version>1.7.21</slf4j.version>
        <jackson.version>2.14.2</jackson.version>
        <doclint>none</doclint>
    </properties>
```
(slf4j·jackson 버전은 Task 6 에서 올린다.)

`<pluginManagement><plugins>` 안, 기존 m2e `lifecycle-mapping` 항목 **앞**에 추가한다.

```xml
                <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-compiler-plugin</artifactId><version>3.16.0</version></plugin>
                <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-surefire-plugin</artifactId><version>3.6.0</version></plugin>
                <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-jar-plugin</artifactId><version>3.5.1</version></plugin>
                <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-source-plugin</artifactId><version>3.4.0</version></plugin>
                <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-javadoc-plugin</artifactId><version>3.12.0</version></plugin>
                <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-gpg-plugin</artifactId><version>3.2.8</version></plugin>
                <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-shade-plugin</artifactId><version>3.6.2</version></plugin>
                <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-enforcer-plugin</artifactId><version>3.6.3</version></plugin>
                <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-deploy-plugin</artifactId><version>3.2.0</version></plugin>
                <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-install-plugin</artifactId><version>3.2.0</version></plugin>
                <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-resources-plugin</artifactId><version>3.5.0</version></plugin>
                <plugin><groupId>org.apache.felix</groupId><artifactId>maven-bundle-plugin</artifactId><version>6.2.0</version></plugin>
                <plugin><groupId>org.codehaus.mojo</groupId><artifactId>versions-maven-plugin</artifactId><version>2.22.0</version></plugin>
```

- [ ] **Step 3: 루트 `<build><plugins>` 정리**

루트 `<build><plugins>` 를 아래 두 개만 남기고 나머지를 지운다. 지우는 것: `maven-compiler-plugin`(release property 로 대체), `maven-jar-plugin`(read-only `finalName` 설정), `maven-eclipse-plugin`, `maven-resources-plugin`·`build-helper-maven-plugin`(features.xml 처리용), `maven-release-plugin`.

```xml
        <plugins>
            <!-- Step 1 의 maven-enforcer-plugin 블록 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-source-plugin</artifactId>
                <executions>
                    <execution>
                        <id>attach-sources</id>
                        <phase>verify</phase>
                        <goals>
                            <goal>jar-no-fork</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
```
루트의 `<distributionManagement>` 블록과 `<profiles>`(OSSRH `release` profile) 블록을 통째로 지운다. Task 5 에서 Central Portal 기준으로 새로 만든다. `<reporting>` 의 javadoc `<version>${maven.javadoc.plugin.version}</version>` 줄도 지운다.

```bash
git rm -q src/main/resources/features.xml .github/workflows/release-management.yml
```

- [ ] **Step 4: 하위 모듈의 플러그인 버전과 `finalName` 제거**

maven-jar-plugin 3.x 는 `finalName` 설정을 거부한다(read-only). 아래를 지운다.

- `connectors/pom.xml`: `maven-surefire-plugin` 의 `<version>2.18.1</version>`, pluginManagement 의 `maven-jar-plugin` `<version>2.5</version>` 과 `<finalName>${project.artifactId}-${release.version}</finalName>`, `maven-bundle-plugin` `<version>5.1.8</version>`.
- `core/pom.xml`: `maven-shade-plugin` `<version>1.3.3</version>`, `maven-bundle-plugin` `<version>5.1.8</version>`, `<reporting>` javadoc 의 `<version>` 줄.
- `distribution/pom.xml`: `maven-javadoc-plugin` `<version>${maven.javadoc.plugin.version}</version>`, `maven-shade-plugin` `<version>1.3.3</version>`.
- `core-test/pom.xml`: `maven-deploy-plugin` `<version>2.8.2</version>`.

`core-integration-test` 와 `it-*` 의 플러그인(surefire 2.19, gmavenplus 1.10.0, jar, deploy)은 Task 10 에서 다룬다. 지금 건드리지 않는다.

```bash
grep -rn '<version>\${maven\.\|release\.version\|<version>1\.3\.3<\|<version>5\.1\.8<\|<version>2\.18\.1<' --include=pom.xml . | grep -v '/target/'
```
Expected: 출력 없음.

- [ ] **Step 5: JDK 17 과 21 에서 빌드**

```bash
./mvnw -B --no-transfer-progress clean install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD|WARNING.*deprecated'
JAVA_HOME=~/.jdks/ms-21.0.12 ./mvnw -B --no-transfer-progress clean install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD'
unzip -p core/target/openstack4j-core-4.0.0-SNAPSHOT.jar META-INF/MANIFEST.MF | grep -E 'Bundle-SymbolicName|Export-Package' | cut -c1-80
javap -v -cp core/target/classes org.openstack4j.api.OSClient | grep 'major version'
```
Expected: 두 JDK 모두 `BUILD SUCCESS`, 테스트 수는 Task 1 과 같다(17 / 676 ×2 / 29 ×2). OSGi manifest 가 남아 있다. `major version: 61`(Java 17).

surefire 3 로 바뀌며 테스트 수가 줄었다면(예: 676 → 0) connectors 의 `suiteXmlFiles` 경로가 무시된 것이다. `connectors/pom.xml` 의 surefire `<configuration>` 이 그대로인지 확인한다.

- [ ] **Step 6: `.github/workflows/ci.yaml` 교체**

```yaml
name: CI

on:
  push:
    branches: [ main ]
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        java-version: [ 17, 21 ]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: ${{ matrix.java-version }}
          cache: maven
      - name: Build and test
        run: ./mvnw -B --no-transfer-progress install
```

- [ ] **Step 7: 커밋과 PR**

```bash
git add -A
git commit -m "build: require JDK 17, refresh build plugins, run CI on PRs

- maven.compiler.release=17, enforcer requires JDK 17+ and Maven 3.9+
- all plugin versions managed in the parent pluginManagement
- drop the OSSRH release setup (replaced by Central Portal next),
  the stale Karaf features.xml and the never-triggered release workflow
- CI: JDK 17/21 matrix, runs on pull_request and pushes to main

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
공통 PR 절차를 따른다. CI 의 `build (17)`, `build (21)` 이 모두 pass 여야 한다.

---

### Task 5: Maven Central 배포 파이프라인과 4.0.0-alpha.1

**Files:**
- Modify: `pom.xml` (`release` profile 추가)
- Modify: `core-test/pom.xml`, `core-integration-test/pom.xml` (배포 제외 확인)
- Create: `.github/workflows/release.yml`
- Modify: `.github/release-drafter.yml`

**Interfaces:**
- Consumes: GitHub Secrets `CENTRAL_USERNAME`, `CENTRAL_TOKEN`, `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE` (사용자가 2026-10-01 등록 완료).
- Produces: `v*` tag push 시 Central 배포 + GitHub Release. profile 이름 `release`.

- [ ] **Step 1: 루트 `pom.xml` 끝(`</project>` 앞)에 `release` profile 추가**

```xml
    <profiles>
        <profile>
            <id>release</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-javadoc-plugin</artifactId>
                        <executions>
                            <execution>
                                <id>attach-javadocs</id>
                                <goals>
                                    <goal>jar</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-gpg-plugin</artifactId>
                        <executions>
                            <execution>
                                <id>sign-artifacts</id>
                                <phase>verify</phase>
                                <goals>
                                    <goal>sign</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                    <plugin>
                        <groupId>org.sonatype.central</groupId>
                        <artifactId>central-publishing-maven-plugin</artifactId>
                        <version>0.11.0</version>
                        <extensions>true</extensions>
                        <configuration>
                            <publishingServerId>central</publishingServerId>
                            <autoPublish>true</autoPublish>
                            <waitUntil>published</waitUntil>
                            <excludeArtifacts>
                                <artifact>openstack4j-core-test</artifact>
                                <artifact>openstack4j-core-integration-test</artifact>
                                <artifact>it-httpclient</artifact>
                                <artifact>it-okhttp</artifact>
                            </excludeArtifacts>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>
```
maven-gpg-plugin 3.x 는 환경 변수 `MAVEN_GPG_PASSPHRASE` 로 passphrase 를 받는다.

- [ ] **Step 2: 로컬에서 번들을 만들어 내용 검증 (업로드하지 않음)**

```bash
git switch main && git pull && git switch -c task/05-release-pipeline
./mvnw -B --no-transfer-progress -Prelease deploy -DskipTests -Dgpg.skip -DskipPublishing=true 2>&1 | grep -E 'central|BUILD|ERROR' | tail -10
unzip -l target/central-publishing/central-bundle.zip | awk '{print $4}' | grep -E '\.(pom|jar)$' | sort
```
Expected: `BUILD SUCCESS`. 목록에 `openstack4j-parent`, `openstack4j-core`(jar, sources, javadoc, pom), `openstack4j-connectors`, `openstack4j-httpclient`, `openstack4j-okhttp`, `openstack4j`(distribution) 이 있고, `core-test`, `core-integration-test`, `it-` 는 **없다**.

`excludeArtifacts` 형식 오류로 실패하거나 제외 모듈이 번들에 들어 있으면, 대신 해당 모듈 POM 의 `<properties>` 에 `<maven.deploy.skip>true</maven.deploy.skip>` 를 넣고 Step 1 의 `excludeArtifacts` 를 지운 뒤 다시 확인한다. javadoc 오류로 실패하면 `maven-javadoc-plugin` 의 `<configuration>` 에 `<failOnError>false</failOnError>` 를 넣는다(원본도 doclint 를 끈 상태였다).

- [ ] **Step 3: POM 필수 항목 확인**

```bash
for m in core connectors/httpclient connectors/okhttp distribution; do ./mvnw -B -q help:effective-pom -pl $m -Doutput=/tmp/ep.xml >/dev/null && echo "$m: $(grep -c -E '<(name|description|url|licenses|scm|developers)>' /tmp/ep.xml)"; done
```
Expected: 각 모듈 6 이상(name, description, url, licenses, scm, developers 가 모두 있음). description 이 빠진 모듈은 그 POM 에 `<description>OpenStack Java API - ...</description>` 를 추가한다.

- [ ] **Step 4: `.github/workflows/release.yml` 작성**

```yaml
name: Release

on:
  push:
    tags: [ 'v*' ]

permissions:
  contents: write

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
          cache: maven
          server-id: central
          server-username: CENTRAL_USERNAME
          server-password: CENTRAL_TOKEN
          gpg-private-key: ${{ secrets.GPG_PRIVATE_KEY }}
          gpg-passphrase: MAVEN_GPG_PASSPHRASE
      - name: Set version from tag
        run: ./mvnw -B -q versions:set -DnewVersion="${GITHUB_REF_NAME#v}" -DgenerateBackupPoms=false -DprocessAllModules=true
      - name: Build, sign and publish to Maven Central
        run: ./mvnw -B --no-transfer-progress -Prelease deploy
        env:
          CENTRAL_USERNAME: ${{ secrets.CENTRAL_USERNAME }}
          CENTRAL_TOKEN: ${{ secrets.CENTRAL_TOKEN }}
          MAVEN_GPG_PASSPHRASE: ${{ secrets.GPG_PASSPHRASE }}
      - name: Publish GitHub Release
        uses: release-drafter/release-drafter@v6
        with:
          tag: ${{ github.ref_name }}
          name: ${{ github.ref_name }}
          version: ${{ github.ref_name }}
          publish: true
          prerelease: ${{ contains(github.ref_name, '-') }}
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

- [ ] **Step 5: `.github/release-drafter.yml` 교체**

```yaml
name-template: 'v$RESOLVED_VERSION'
tag-template: 'v$RESOLVED_VERSION'
categories:
  - title: 'Breaking changes'
    labels: [ 'breaking' ]
  - title: 'Features'
    labels: [ 'feature', 'enhancement' ]
  - title: 'Fixes'
    labels: [ 'bug', 'fix' ]
  - title: 'Dependencies'
    labels: [ 'dependencies' ]
template: |
  ## Changes

  $CHANGES
```

- [ ] **Step 6: 커밋, PR, 머지**

```bash
git add -A
git commit -m "ci: publish releases to Maven Central via Central Portal

Pushing a v* tag sets the version from the tag, signs all artifacts,
publishes them to Maven Central and creates a GitHub Release.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
공통 PR 절차를 따른다(머지 포함).

- [ ] **Step 7: 🛑 `v4.0.0-alpha.1` 배포 (사용자 확인 필수)**

Maven Central 에 올라간 버전은 **삭제할 수 없다.** 사용자에게 "groupId·JDK 설정만 바뀐 3.x 코드를 `4.0.0-alpha.1` 로 Central 에 올려 파이프라인을 검증합니다" 라고 알리고 승인을 받은 뒤에만 실행한다.

```bash
git switch main && git pull
git tag -a v4.0.0-alpha.1 -m "Pipeline verification release. Not for use."
git push origin v4.0.0-alpha.1
gh run watch -R seogineer/openstack4j $(gh run list -R seogineer/openstack4j -w Release -L 1 --json databaseId -q '.[0].databaseId')
```
Expected: Release workflow 성공.

- [ ] **Step 8: Central 반영 확인**

동기화에 최대 30분 이상 걸릴 수 있다. 5분 간격으로 확인한다.

```bash
curl -s -o /dev/null -w '%{http_code}\n' https://repo1.maven.org/maven2/io/github/seogineer/openstack4j-core/4.0.0-alpha.1/openstack4j-core-4.0.0-alpha.1.pom
```
Expected: `200`. GitHub Release `v4.0.0-alpha.1` 가 prerelease 로 생성됐는지 `gh release view v4.0.0-alpha.1 -R seogineer/openstack4j` 로 확인하고, 본문 맨 위에 "파이프라인 검증용 릴리스입니다. 사용하지 마세요." 를 `gh release edit` 로 추가한다.

실패 시: workflow 로그에서 Central 의 검증 오류 메시지(서명, POM 항목, javadoc 누락 등)를 확인해 고친 뒤 `v4.0.0-alpha.2` 로 다시 시도한다. 같은 버전 번호는 재사용하지 않는다.

---

### Task 6: core 의존성 갱신

**Files:**
- Modify: `pom.xml` (properties, `<dependencies>`, `<dependencyManagement>`)
- Modify: `core/pom.xml` (guava, snakeyaml)
- Modify: `connectors/pom.xml`, `core-test/pom.xml` (mockito)
- Create: `core/src/test/java/org/openstack4j/test/heat/HeatYamlParsingTest.java`
- Create: `core/src/test/resources/heat/template.yaml`, `core/src/test/resources/heat/script.sh`, `core/src/test/resources/heat/nested.yaml`, `core/src/test/resources/heat/env.yaml`

**Interfaces:**
- Produces: core 의 전이 의존성 = Jackson 2.22.3, Guava 33.7.2-jre, SnakeYAML 2.7, SLF4J API 2.0.20, `com.github.java-json-tools:json-patch` 1.13. jsr305 는 `provided`.

- [ ] **Step 1: SnakeYAML 사용부(Heat Template/Environment) 특성 테스트 작성**

SnakeYAML 1 → 2 는 API 가 바뀐 메이저 업그레이드인데, Heat YAML 파싱에는 테스트가 없다. 현재 동작을 먼저 고정한다.

```bash
git switch main && git pull && git switch -c task/06-core-dependencies
mkdir -p core/src/test/resources/heat core/src/test/java/org/openstack4j/test/heat
```

`core/src/test/resources/heat/template.yaml`:
```yaml
heat_template_version: 2018-08-31
resources:
  server:
    type: OS::Nova::Server
    properties:
      user_data:
        get_file: script.sh
  nested:
    type: nested.yaml
```

`core/src/test/resources/heat/script.sh`:
```text
#!/bin/sh
echo hello
```

`core/src/test/resources/heat/nested.yaml`:
```yaml
heat_template_version: 2018-08-31
resources: {}
```

`core/src/test/resources/heat/env.yaml`:
```yaml
resource_registry:
  My::Nested: nested.yaml
```

`core/src/test/java/org/openstack4j/test/heat/HeatYamlParsingTest.java`:
```java
package org.openstack4j.test.heat;

import java.net.URL;

import org.openstack4j.openstack.heat.utils.Environment;
import org.openstack4j.openstack.heat.utils.Template;
import org.testng.Assert;
import org.testng.annotations.Test;

public class HeatYamlParsingTest {

    private static URL resource(String name) {
        return HeatYamlParsingTest.class.getResource("/heat/" + name);
    }

    @Test
    public void templateResolvesGetFileAndNestedTemplate() throws Exception {
        Template template = new Template(resource("template.yaml"));

        Assert.assertTrue(template.getFiles().get("script.sh").contains("echo hello"));
        Assert.assertTrue(template.getFiles().get("nested.yaml").contains("heat_template_version"));
    }

    @Test
    public void environmentResolvesResourceRegistryTemplates() throws Exception {
        Environment environment = new Environment(resource("env.yaml"));

        Assert.assertEquals(environment.getFiles().size(), 1);
        Assert.assertTrue(environment.getFiles().values().iterator().next().contains("heat_template_version"));
    }
}
```

- [ ] **Step 2: 현재 버전(SnakeYAML 1.33)에서 통과 확인**

```bash
./mvnw -B test -pl core -Dtest=HeatYamlParsingTest 2>&1 | grep -E 'Tests run:|FAIL' | tail -3
```
Expected: `Tests run: 2, Failures: 0, Errors: 0`. 실패하면 키 이름(`getFiles()` 의 키가 상대경로인지 절대 URL 인지)을 실제 값에 맞춰 assertion 을 고친다. 이 테스트는 **현재 동작을 고정**하는 것이 목적이다.

- [ ] **Step 3: 루트 `pom.xml` 의존성 교체**

properties:
```xml
        <slf4j.version>2.0.20</slf4j.version>
        <jackson.version>2.22.3</jackson.version>
```

루트 `<dependencies>` 의 jsr305 와 json-patch 를 교체한다.
```xml
        <dependency>
            <groupId>com.google.code.findbugs</groupId>
            <artifactId>jsr305</artifactId>
            <version>3.0.2</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>com.github.java-json-tools</groupId>
            <artifactId>json-patch</artifactId>
            <version>1.13</version>
        </dependency>
```
(json-patch 1.13 은 같은 프로젝트의 후속 좌표이고 패키지 `com.github.fge.jsonpatch` 는 그대로다.)

`<dependencyManagement>` 의 testng 를 `7.12.0` 으로 올리고, mockito 를 추가한다.
```xml
            <dependency>
                <groupId>org.mockito</groupId>
                <artifactId>mockito-core</artifactId>
                <version>5.24.0</version>
            </dependency>
```
`connectors/pom.xml` 과 `core-test/pom.xml` 의 mockito `<version>5.11.0</version>` 줄을 지운다.

`core/pom.xml`: guava `29.0-jre` → `33.7.2-jre`, snakeyaml `1.33` → `2.7`.

- [ ] **Step 4: 컴파일·테스트**

```bash
./mvnw -B --no-transfer-progress clean install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD|ERROR' | head -20
```
Expected: `BUILD SUCCESS`, core 19개(기존 17 + 신규 2), 676 ×2, 29 ×2.

SnakeYAML 2 에서 컴파일 오류가 나면 `Template.java`·`Environment.java` 의 `new Yaml()` 을 `new Yaml(new org.yaml.snakeyaml.constructor.SafeConstructor(new org.yaml.snakeyaml.LoaderOptions()))` 로 바꾼다. Heat 템플릿은 일반 Map/List 만 쓰므로 SafeConstructor 로 충분하고, 임의 객체 생성 취약점도 막는다.

- [ ] **Step 5: 의존성 트리 확인**

```bash
./mvnw -B -q dependency:tree -pl core -Dscope=compile | grep -E 'jackson-databind|guava|snakeyaml|slf4j-api|json-patch|jsr305'
```
Expected: jackson-databind 2.22.3, guava 33.7.2-jre, snakeyaml 2.7, slf4j-api 2.0.20, json-patch 1.13. `jsr305` 는 compile scope 트리에 나오지 않는다. `com.github.fge:json-patch` 가 없어야 한다.

- [ ] **Step 6: 커밋과 PR**

```bash
git add -A
git commit -m "build: update core dependencies

Jackson 2.22.3, Guava 33.7.2-jre, SnakeYAML 2.7, SLF4J 2.0.20,
json-patch 1.13 (com.github.java-json-tools, successor of com.github.fge),
jsr305 3.0.2 as provided. Adds characterization tests for Heat YAML parsing
before the SnakeYAML major upgrade.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
공통 PR 절차를 따른다.

---

### Task 7: httpclient connector 를 HttpClient 5 로 전환

**Files:**
- Modify: `connectors/httpclient/pom.xml`
- Modify: `connectors/httpclient/src/main/java/org/openstack4j/connectors/httpclient/{HttpClientConfigInterceptor,HttpClientFactory,HttpCommand,HttpExecutorServiceImpl,HttpResponseImpl}.java`
- Create: `connectors/httpclient/src/test/java/org/openstack4j/connectors/httpclient/HttpClientConnectorTest.java`

**Interfaces:**
- Produces (공개 API 변경 — MIGRATION.md 에 기록):
  - `HttpClientConfigInterceptor.onClientCreate(org.apache.hc.client5.http.impl.classic.HttpClientBuilder client, org.apache.hc.client5.http.config.RequestConfig.Builder requestConfig, Config config)`
  - `HttpResponseImpl.wrap(org.apache.hc.core5.http.ClassicHttpResponse)`, `HttpResponseImpl.unwrap()` → `ClassicHttpResponse`
  - `HttpCommand.execute()` → `ClassicHttpResponse`
- 표시 이름 `"Apache HttpClient Connector"` 유지(Task 11 이 사용).

- [ ] **Step 1: connector 동작 테스트 작성**

connector 모듈의 테스트는 core-test 의 `all.xml`(패키지 `org.openstack4j.*`)로 함께 실행된다.

```bash
git switch main && git pull && git switch -c task/07-httpclient5
mkdir -p connectors/httpclient/src/test/java/org/openstack4j/connectors/httpclient
```

`HttpClientConnectorTest.java`:
```java
package org.openstack4j.connectors.httpclient;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.core.transport.Config;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.core.transport.HttpRequest;
import org.openstack4j.core.transport.HttpResponse;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class HttpClientConnectorTest {

    private MockWebServer server;

    @BeforeMethod
    public void startServer() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterMethod(alwaysRun = true)
    public void stopServer() throws Exception {
        server.shutdown();
    }

    private HttpRequest.RequestBuilder<Void> request(HttpMethod method) {
        return HttpRequest.builder()
                .endpoint(server.url("/").toString())
                .path("v3/resource")
                .method(method)
                .config(Config.newConfig());
    }

    private HttpResponse execute(HttpRequest<Void> request) {
        HttpResponse response = new HttpExecutorServiceImpl().execute(request);
        Assert.assertNotNull(response, "connector returned null (exception was swallowed and logged)");
        return response;
    }

    @Test
    public void patchIsSentWithBody() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        HttpResponse response = execute(request(HttpMethod.PATCH).json("{\"name\":\"x\"}").build());
        RecordedRequest recorded = server.takeRequest(5, TimeUnit.SECONDS);

        Assert.assertEquals(response.getStatus(), 200);
        Assert.assertEquals(recorded.getMethod(), "PATCH");
        Assert.assertEquals(recorded.getBody().readUtf8(), "{\"name\":\"x\"}");
        response.close();
    }

    @Test
    public void callerSuppliedContentLengthHeaderIsIgnored() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(201));

        HttpResponse response = execute(request(HttpMethod.PUT).header("Content-Length", 0).build());
        RecordedRequest recorded = server.takeRequest(5, TimeUnit.SECONDS);

        Assert.assertEquals(response.getStatus(), 201);
        Assert.assertEquals(recorded.getMethod(), "PUT");
        Assert.assertEquals(recorded.getBodySize(), 0);
        response.close();
    }

    @Test
    public void queryParamsAreEncoded() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        HttpResponse response = execute(request(HttpMethod.GET)
                .queryParam("name", "a b").queryParam("tag", "x").queryParam("tag", "y").build());
        RecordedRequest recorded = server.takeRequest(5, TimeUnit.SECONDS);

        Assert.assertEquals(recorded.getRequestUrl().queryParameter("name"), "a b");
        Assert.assertEquals(recorded.getRequestUrl().queryParameterValues("tag"), Arrays.asList("x", "y"));
        response.close();
    }
}
```

- [ ] **Step 2: HttpClient 4 에서 실행해 결과 기록**

```bash
./mvnw -B test -pl connectors/httpclient -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=HttpClientConnectorTest 2>&1 | grep -E 'Tests run:|FAIL|already present' | tail -5
```
Expected: `callerSuppliedContentLengthHeaderIsIgnored` 가 실패(HttpClient 가 `Content-Length header already present` 로 거부 → connector 가 null 반환). 다른 두 개는 통과. 만약 셋 다 통과하면 그대로 진행한다(특성 테스트로서 HttpClient 5 전환 후에도 같은 동작을 보장한다).

`-Dtest` 가 suiteXmlFiles 와 함께 동작하지 않아 0개가 실행되면, 이 Step 과 Step 8 은 모듈 전체 테스트(`./mvnw -B test -pl connectors/httpclient -am`)로 실행하고 결과에서 `HttpClientConnectorTest` 줄을 찾는다.

- [ ] **Step 3: `connectors/httpclient/pom.xml` 의존성 교체**

`<properties>` 블록(`httpclient-version` 4.3.1, 쓰이지 않음)을 지우고, 의존성을 바꾼다.

```xml
    <dependencies>
        <dependency>
            <groupId>org.apache.httpcomponents.client5</groupId>
            <artifactId>httpclient5</artifactId>
            <version>5.6.4</version>
        </dependency>
    </dependencies>
```

- [ ] **Step 4: `HttpClientConfigInterceptor.java` 교체**

```java
package org.openstack4j.connectors.httpclient;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.openstack4j.core.transport.Config;

/**
 * Allows for interception during the creation of a new HttpClient.  To register a custom singleton interceptor you must invoke
 * {@link HttpClientFactory#registerInterceptor(HttpClientConfigInterceptor)}
 *
 * @author Jeremy Unruh
 */
public interface HttpClientConfigInterceptor {

    /**
     * This method is invoked prior to the HttpClientBuilder build is called allowing any overrides or custom configuration.
     * The connection manager has already been set on {@code client}; set a new one to override it.
     *
     * @param client the http client builder
     * @param requestConfig the request config builder
     * @param config the openstack4j config
     */
    void onClientCreate(HttpClientBuilder client, RequestConfig.Builder requestConfig, Config config);
}
```

- [ ] **Step 5: `HttpClientFactory.java` 교체**

```java
package org.openstack4j.connectors.httpclient;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.HttpsSupport;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;
import org.openstack4j.core.transport.Config;
import org.openstack4j.core.transport.UntrustedSSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates the initial HttpClient and keeps it as a singleton to preserve pooling strategies within the Http Client
 *
 * @author Jeremy Unruh
 */
public class HttpClientFactory {

    public static final HttpClientFactory INSTANCE = new HttpClientFactory();
    private static final String USER_AGENT = "OpenStack4j-Agent";
    private static final Logger LOG = LoggerFactory.getLogger(HttpClientFactory.class);
    private static HttpClientConfigInterceptor INTERCEPTOR;
    private CloseableHttpClient client;

    /**
     * Registers a HttpClientConfigInterceptor that is invoked prior to a new HttpClient being created.
     *
     * @param interceptor the http config interceptor
     */
    public static void registerInterceptor(HttpClientConfigInterceptor interceptor) {
        INTERCEPTOR = interceptor;
    }

    /**
     * Creates or Returns an existing HttpClient
     *
     * @param config the configuration
     * @return CloseableHttpClient
     */
    CloseableHttpClient getClient(Config config) {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = buildClient(config);
                }
            }
        }
        return client;
    }

    private CloseableHttpClient buildClient(Config config) {
        HttpClientBuilder cb = HttpClientBuilder.create().setUserAgent(USER_AGENT);

        if (config.getProxy() != null) {
            try {
                URL url = new URL(config.getProxy().getHost());
                cb.setProxy(new HttpHost(url.getProtocol(), url.getHost(), config.getProxy().getPort()));
            } catch (MalformedURLException e) {
                LOG.error(e.getMessage(), e);
            }
        }

        PoolingHttpClientConnectionManagerBuilder cmb = PoolingHttpClientConnectionManagerBuilder.create();

        SSLContext sslContext = null;
        HostnameVerifier hostnameVerifier = null;
        if (config.isIgnoreSSLVerification()) {
            sslContext = UntrustedSSL.getSSLContext();
            hostnameVerifier = NoopHostnameVerifier.INSTANCE;
        }
        if (config.getSslContext() != null)
            sslContext = config.getSslContext();
        if (config.getHostNameVerifier() != null)
            hostnameVerifier = config.getHostNameVerifier();
        if (sslContext != null || hostnameVerifier != null) {
            cmb.setTlsSocketStrategy(new DefaultClientTlsStrategy(
                    sslContext != null ? sslContext : SSLContexts.createDefault(),
                    hostnameVerifier != null ? hostnameVerifier : HttpsSupport.getDefaultHostnameVerifier()));
        }

        if (config.getMaxConnections() > 0)
            cmb.setMaxConnTotal(config.getMaxConnections());

        if (config.getMaxConnectionsPerRoute() > 0)
            cmb.setMaxConnPerRoute(config.getMaxConnectionsPerRoute());

        if (config.getConnectTimeout() > 0)
            cmb.setDefaultConnectionConfig(ConnectionConfig.custom()
                    .setConnectTimeout(Timeout.ofMilliseconds(config.getConnectTimeout())).build());

        RequestConfig.Builder rcb = RequestConfig.custom();

        if (config.getReadTimeout() > 0)
            rcb.setResponseTimeout(Timeout.ofMilliseconds(config.getReadTimeout()));

        cb.setConnectionManager(cmb.build());

        if (INTERCEPTOR != null) {
            INTERCEPTOR.onClientCreate(cb, rcb, config);
        }

        return cb.setDefaultRequestConfig(rcb.build()).build();
    }
}
```

- [ ] **Step 6: `HttpCommand.java` 교체**

```java
package org.openstack4j.connectors.httpclient;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.routing.RoutingSupport;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;
import org.openstack4j.api.exceptions.ConnectionException;
import org.openstack4j.core.transport.HttpRequest;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.core.transport.functions.EndpointURIFromRequestFunction;

/**
 * HttpCommand is responsible for executing the actual request driven by the
 * HttpExecutor.
 */
public final class HttpCommand<R> {

    HttpUriRequestBase clientReq;
    private HttpRequest<R> request;
    private CloseableHttpClient client;
    private int retries;

    private HttpCommand(HttpRequest<R> request) {
        this.request = request;
    }

    /**
     * Creates a new HttpCommand from the given request
     *
     * @param request the request
     * @return the command
     */
    public static <R> HttpCommand<R> create(HttpRequest<R> request) {
        HttpCommand<R> command = new HttpCommand<R>(request);
        command.initialize();
        return command;
    }

    private void initialize() {
        URI url = null;
        try {
            url = populateQueryParams(request);
        } catch (URISyntaxException e) {
            throw new ConnectionException(e.getMessage(), e.getIndex(), e);
        }
        client = HttpClientFactory.INSTANCE.getClient(request.getConfig());

        switch (request.getMethod()) {
            case POST:
                clientReq = new HttpPost(url);
                break;
            case PUT:
                clientReq = new HttpPut(url);
                break;
            case DELETE:
                clientReq = new HttpDelete(url);
                break;
            case HEAD:
                clientReq = new HttpHead(url);
                break;
            case PATCH:
                clientReq = new HttpPatch(url);
                break;
            case GET:
                clientReq = new HttpGet(url);
                break;
            default:
                throw new IllegalArgumentException("Unsupported http method: " + request.getMethod());
        }
        clientReq.setHeader("Accept", "application/json");
        populateHeaders(request);
    }

    /**
     * Executes the command and returns the Response
     *
     * @return the response
     */
    public ClassicHttpResponse execute() throws Exception {
        if (request.getEntity() != null) {
            if (InputStream.class.isAssignableFrom(request.getEntity().getClass())) {
                clientReq.setEntity(new InputStreamEntity((InputStream) request.getEntity(), -1,
                        ContentType.parse(request.getContentType())));
            } else {
                String json = ObjectMapperSingleton.getContext(request.getEntity().getClass()).writer()
                        .writeValueAsString(request.getEntity());
                clientReq.setEntity(new StringEntity(json,
                        ContentType.parse(request.getContentType()).withCharset(StandardCharsets.UTF_8)));
            }
        } else if (request.hasJson()) {
            clientReq.setEntity(new StringEntity(request.getJson(), ContentType.APPLICATION_JSON));
        }

        return client.executeOpen(RoutingSupport.determineHost(clientReq), clientReq, null);
    }

    /**
     * @return true if a request entity has been set
     */
    public boolean hasEntity() {
        return request.getEntity() != null;
    }

    /**
     * @return current retry execution count for this command
     */
    public int getRetries() {
        return retries;
    }

    /**
     * @return incremement's the retry count and returns self
     */
    public HttpCommand<R> incrementRetriesAndReturn() {
        initialize();
        retries++;
        return this;
    }

    public HttpRequest<R> getRequest() {
        return request;
    }

    private URI populateQueryParams(HttpRequest<R> request) throws URISyntaxException {

        URIBuilder uri = new URIBuilder(new EndpointURIFromRequestFunction().apply(request));

        if (!request.hasQueryParams())
            return uri.build();

        for (Map.Entry<String, List<Object>> entry : request.getQueryParams().entrySet()) {
            for (Object o : entry.getValue()) {
                uri.addParameter(entry.getKey(), String.valueOf(o));
            }
        }
        return uri.build();
    }

    private void populateHeaders(HttpRequest<R> request) {

        if (!request.hasHeaders())
            return;

        for (Map.Entry<String, Object> h : request.getHeaders().entrySet()) {
            // HttpClient computes these from the entity and rejects requests that already carry them
            if (HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(h.getKey())
                    || HttpHeaders.TRANSFER_ENCODING.equalsIgnoreCase(h.getKey()))
                continue;
            clientReq.addHeader(h.getKey(), String.valueOf(h.getValue()));
        }
    }
}
```

- [ ] **Step 7: `HttpExecutorServiceImpl.java`, `HttpResponseImpl.java` 수정**

`HttpExecutorServiceImpl.java`: import `org.apache.http.client.methods.CloseableHttpResponse` 를 `org.apache.hc.core5.http.ClassicHttpResponse` 로 바꾸고, `invokeRequest` 를 아래로 교체한다(나머지 동일).

```java
    private <R> HttpResponse invokeRequest(HttpCommand<R> command) throws Exception {
        ClassicHttpResponse response = command.execute();

        if (command.getRetries() == 0 && response.getCode() == 401 && !command.getRequest().getHeaders().containsKey(ClientConstants.HEADER_OS4J_AUTH)) {
            try {
                OSAuthenticator.reAuthenticate();
                command.getRequest().getHeaders().put(ClientConstants.HEADER_X_AUTH_TOKEN, OSClientSession.getCurrent().getTokenId());
            } finally {
                response.close();
            }
            return invokeRequest(command.incrementRetriesAndReturn());
        }

        return HttpResponseImpl.wrap(response);
    }
```

`HttpResponseImpl.java`: 타입만 바꾼다.
- import: `org.apache.http.Header` → `org.apache.hc.core5.http.Header`, `org.apache.http.HttpEntity` → `org.apache.hc.core5.http.HttpEntity`, `org.apache.http.client.methods.CloseableHttpResponse` → `org.apache.hc.core5.http.ClassicHttpResponse`.
- 필드·`wrap`·`unwrap`·생성자의 `CloseableHttpResponse` → `ClassicHttpResponse`.
- `getStatus()`: `return response.getCode();`
- `getStatusMessage()`: `return response.getReasonPhrase();`
- `headers()`: `Header[] headers = response.getHeaders();`

- [ ] **Step 8: 테스트 통과 확인**

```bash
./mvnw -B --no-transfer-progress install -pl connectors/httpclient,core-integration-test,core-integration-test/it-httpclient,distribution -am 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD|ERROR' | head
grep -rn 'org\.apache\.http\.' connectors/httpclient/src || echo "no httpclient4 imports"
```
Expected: `BUILD SUCCESS`, httpclient 모듈 `Tests run: 679`(676 + 신규 3), it-httpclient `Tests run: 29`, `no httpclient4 imports`.

- [ ] **Step 9: 커밋과 PR**

```bash
git add -A
git commit -m "feat(httpclient): migrate connector to Apache HttpClient 5

BREAKING: HttpClientConfigInterceptor and HttpResponseImpl now expose
HttpClient 5 types (org.apache.hc.*). Caller-supplied Content-Length /
Transfer-Encoding headers are skipped because HttpClient computes them.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
공통 PR 절차를 따른다. PR 에 `breaking` 라벨을 단다(`gh pr edit --add-label breaking`, 라벨이 없으면 `gh label create breaking`).

---

### Task 8: http-connector 를 JDK HttpClient 로 재구현

**Files:**
- Modify: `connectors/pom.xml` (modules 에 http-connector 복귀)
- Create: `connectors/http-connector/src/main/java/org/openstack4j/connectors/http/HttpClientFactory.java`
- Modify: `connectors/http-connector/src/main/java/org/openstack4j/connectors/http/{HttpCommand,HttpExecutorServiceImpl,HttpResponseImpl}.java`
- Modify: `connectors/http-connector/pom.xml` (m2e pluginManagement 제거)
- Create: `connectors/http-connector/src/test/java/org/openstack4j/connectors/http/HttpConnectorTest.java`

**Interfaces:**
- Produces: `HttpCommand.execute()` → `org.openstack4j.core.transport.HttpResponse` (기존과 같음). 표시 이름 `"Http URL Connector"` 유지. 패키지 전용 `HttpClientFactory.get(Config)` → `java.net.http.HttpClient`.

- [ ] **Step 1: 리액터에 복귀시키고 실패 확인**

```bash
git switch main && git pull && git switch -c task/08-http-connector-jdk-httpclient
```
`connectors/pom.xml` 의 `<modules>` 를 아래로 바꾼다.
```xml
    <modules>
        <module>http-connector</module>
        <module>httpclient</module>
        <module>okhttp</module>
    </modules>
```
`connectors/http-connector/pom.xml` 의 `<pluginManagement>`(m2e lifecycle-mapping) 블록을 지운다.

```bash
./mvnw -B test -pl connectors/http-connector -am 2>&1 | grep -E 'Tests run: [0-9]+, Failures|InaccessibleObjectException' | sort | uniq -c | tail -3
```
Expected: 실패 약 20개, `InaccessibleObjectException: Unable to make field protected java.lang.String java.net.HttpURLConnection.method accessible`.

- [ ] **Step 2: connector 동작 테스트 작성**

`connectors/http-connector/src/test/java/org/openstack4j/connectors/http/HttpConnectorTest.java`:
```java
package org.openstack4j.connectors.http;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.openstack4j.core.transport.Config;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.core.transport.HttpRequest;
import org.openstack4j.core.transport.HttpResponse;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class HttpConnectorTest {

    private MockWebServer server;

    @BeforeMethod
    public void startServer() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterMethod(alwaysRun = true)
    public void stopServer() throws Exception {
        server.shutdown();
    }

    private HttpRequest.RequestBuilder<Void> request(HttpMethod method) {
        return HttpRequest.builder()
                .endpoint(server.url("/").toString())
                .path("v3/resource")
                .method(method)
                .config(Config.newConfig());
    }

    private HttpResponse execute(HttpRequest<Void> request) {
        HttpResponse response = new HttpExecutorServiceImpl().execute(request);
        Assert.assertNotNull(response, "connector returned null (exception was swallowed and logged)");
        return response;
    }

    @Test
    public void patchIsSentWithBody() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        HttpResponse response = execute(request(HttpMethod.PATCH).json("{\"name\":\"x\"}").build());
        RecordedRequest recorded = server.takeRequest(5, TimeUnit.SECONDS);

        Assert.assertEquals(response.getStatus(), 200);
        Assert.assertEquals(recorded.getMethod(), "PATCH");
        Assert.assertEquals(recorded.getBody().readUtf8(), "{\"name\":\"x\"}");
    }

    @Test
    public void callerSuppliedContentLengthHeaderIsIgnored() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(201));

        HttpResponse response = execute(request(HttpMethod.PUT).header("Content-Length", 0).build());
        RecordedRequest recorded = server.takeRequest(5, TimeUnit.SECONDS);

        Assert.assertEquals(response.getStatus(), 201);
        Assert.assertEquals(recorded.getMethod(), "PUT");
        Assert.assertEquals(recorded.getBodySize(), 0);
    }

    @Test
    public void queryParamsAreEncoded() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        execute(request(HttpMethod.GET)
                .queryParam("name", "a b").queryParam("tag", "x").queryParam("tag", "y").build());
        RecordedRequest recorded = server.takeRequest(5, TimeUnit.SECONDS);

        Assert.assertEquals(recorded.getRequestUrl().queryParameter("name"), "a b");
        Assert.assertEquals(recorded.getRequestUrl().queryParameterValues("tag"), Arrays.asList("x", "y"));
    }

    @Test
    public void headResponseExposesStatusAndHeadersCaseInsensitively() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(204).addHeader("X-Subject-Token", "abc"));

        HttpResponse response = execute(request(HttpMethod.HEAD).build());

        Assert.assertEquals(response.getStatus(), 204);
        Assert.assertEquals(response.header("x-subject-token"), "abc");
        Assert.assertEquals(response.headers().get("X-Subject-Token"), "abc");
        Assert.assertEquals(response.getStatusMessage(), "No Content");
    }
}
```

- [ ] **Step 3: `HttpClientFactory.java` 작성**

```java
package org.openstack4j.connectors.http;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openstack4j.core.transport.Config;
import org.openstack4j.core.transport.UntrustedSSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds one JDK {@link HttpClient} per {@link Config} and reuses it, because each client owns a selector thread
 * and a connection pool.
 */
final class HttpClientFactory {

    private static final Logger LOG = LoggerFactory.getLogger(HttpClientFactory.class);
    private static final Map<Config, HttpClient> CLIENTS = new ConcurrentHashMap<>();

    private HttpClientFactory() {
    }

    static HttpClient get(Config config) {
        return CLIENTS.computeIfAbsent(config != null ? config : Config.DEFAULT, HttpClientFactory::build);
    }

    private static HttpClient build(Config config) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL);

        if (config.getConnectTimeout() > 0)
            builder.connectTimeout(Duration.ofMillis(config.getConnectTimeout()));

        if (config.getProxy() != null)
            builder.proxy(ProxySelector.of(new InetSocketAddress(config.getProxy().getRawHost(), config.getProxy().getPort())));

        if (config.isIgnoreSSLVerification())
            builder.sslContext(UntrustedSSL.getSSLContext());

        if (config.getSslContext() != null)
            builder.sslContext(config.getSslContext());

        if (config.getHostNameVerifier() != null)
            LOG.warn("Config.withHostnameVerifier is not supported by the JDK HttpClient connector and is ignored. "
                    + "Use -Djdk.internal.httpclient.disableHostnameVerification=true to disable hostname verification.");

        return builder.build();
    }
}
```

- [ ] **Step 4: `HttpCommand.java` 교체**

```java
package org.openstack4j.connectors.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.openstack4j.core.transport.Config;
import org.openstack4j.core.transport.HttpRequest;
import org.openstack4j.core.transport.HttpResponse;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.util.IOUtil;

/**
 * HttpCommand is responsible for executing the actual request driven by the
 * HttpExecutor.
 */
public final class HttpCommand<R> {

    /** Headers the JDK HttpClient sets itself and refuses to accept from callers. */
    private static final Set<String> RESTRICTED_HEADERS = Set.of("connection", "content-length", "expect", "host", "upgrade");

    private final HttpRequest<R> request;
    private int retries;

    private HttpCommand(HttpRequest<R> request) {
        this.request = request;
    }

    /**
     * Creates a new HttpCommand from the given request
     *
     * @param request the request
     * @return the command
     */
    public static <R> HttpCommand<R> create(HttpRequest<R> request) {
        return new HttpCommand<>(request);
    }

    /**
     * Executes the command and returns the Response
     *
     * @return the response
     */
    public HttpResponse execute() throws IOException, InterruptedException {
        java.net.http.HttpRequest.Builder builder = java.net.http.HttpRequest.newBuilder(URI.create(request.getUrl()))
                .method(request.getMethod().name(), bodyPublisher());

        Config config = request.getConfig();
        if (config != null && config.getReadTimeout() > 0)
            builder.timeout(Duration.ofMillis(config.getReadTimeout()));

        if (request.getContentType() != null)
            builder.setHeader("Content-Type", request.getContentType());
        builder.setHeader("Accept", "application/json; charset=utf-8");

        if (request.hasHeaders()) {
            for (Map.Entry<String, Object> h : request.getHeaders().entrySet()) {
                if (!RESTRICTED_HEADERS.contains(h.getKey().toLowerCase(Locale.ROOT)))
                    builder.setHeader(h.getKey(), String.valueOf(h.getValue()));
            }
        }

        java.net.http.HttpResponse<byte[]> response = HttpClientFactory.get(config).send(builder.build(), BodyHandlers.ofByteArray());
        return HttpResponseImpl.wrap(response.headers().map(), response.statusCode(),
                HttpResponseImpl.reasonPhrase(response.statusCode()), response.body());
    }

    private BodyPublisher bodyPublisher() throws IOException {
        Object entity = request.getEntity();
        if (entity != null) {
            if (entity instanceof InputStream)
                return BodyPublishers.ofByteArray(IOUtil.readBytes((InputStream) entity));
            String content = ObjectMapperSingleton.getContext(entity.getClass()).writer().writeValueAsString(entity);
            return BodyPublishers.ofString(content, StandardCharsets.UTF_8);
        }
        if (request.hasJson())
            return BodyPublishers.ofString(request.getJson(), StandardCharsets.UTF_8);
        return BodyPublishers.noBody();
    }

    /**
     * @return true if a request entity has been set
     */
    public boolean hasEntity() {
        return request.getEntity() != null;
    }

    /**
     * @return current retry execution count for this command
     */
    public int getRetries() {
        return retries;
    }

    /**
     * The JDK request is rebuilt from {@link #getRequest()} on every {@link #execute()}, so updated headers
     * (for example a refreshed auth token) are picked up without re-initialisation.
     *
     * @return incremement's the retry count and returns self
     */
    public HttpCommand<R> incrementRetriesAndReturn() {
        retries++;
        return this;
    }

    public HttpRequest<R> getRequest() {
        return request;
    }
}
```

- [ ] **Step 5: `HttpResponseImpl.java` 수정**

`headers()` 를 대소문자 무시 Map 으로 바꾸고(JDK 는 헤더 이름을 소문자로 넘길 수 있다), 상태 문구 헬퍼를 추가한다.

import 에 `java.util.TreeMap` 을 추가하고 `headers()` 를 교체:
```java
    /**
     * @return the a Map of Header Name to Header Value, with case-insensitive keys
     */
    public Map<String, String> headers() {
        Map<String, String> retHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() == null) continue;
            for (String value : entry.getValue()) {
                retHeaders.put(entry.getKey(), value);
            }
        }

        return retHeaders;
    }

    /**
     * The JDK HttpClient does not expose the reason phrase, so map the status codes OpenStack APIs commonly return.
     */
    static String reasonPhrase(int status) {
        switch (status) {
            case 200: return "OK";
            case 201: return "Created";
            case 202: return "Accepted";
            case 204: return "No Content";
            case 300: return "Multiple Choices";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 409: return "Conflict";
            case 413: return "Request Entity Too Large";
            case 415: return "Unsupported Media Type";
            case 500: return "Internal Server Error";
            case 501: return "Not Implemented";
            case 503: return "Service Unavailable";
            default: return "";
        }
    }
```
`import java.util.HashMap;` 과 `import java.util.Set;` 이 더 이상 쓰이지 않으면 지운다.

- [ ] **Step 6: `HttpExecutorServiceImpl.java` 정리**

`e.printStackTrace()` 두 곳을 로그로 바꾸고, 인터럽트 상태를 복원한다. 클래스 Javadoc 의 "interfacing with OKHttp" 를 "interfacing with the JDK HttpClient" 로 고친다.

```java
    private static final Logger LOG = LoggerFactory.getLogger(HttpExecutorServiceImpl.class);

    @Override
    public <R> HttpResponse execute(HttpRequest<R> request) {
        try {
            return invoke(request);
        } catch (ResponseException re) {
            throw re;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    private <R> HttpResponse invoke(HttpRequest<R> request) throws Exception {

        HttpCommand<R> command = HttpCommand.create(request);

        try {
            return invokeRequest(command);
        } catch (ResponseException re) {
            throw re;
        } catch (Exception pe) {
            if (pe instanceof InterruptedException)
                Thread.currentThread().interrupt();
            throw new ConnectionException(pe.getMessage(), 0, pe);
        }
    }
```
import `org.slf4j.Logger`, `org.slf4j.LoggerFactory` 추가.

- [ ] **Step 7: 테스트 통과 확인**

```bash
./mvnw -B --no-transfer-progress install -pl connectors/http-connector -am 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD|ERROR' | head
grep -rn 'HttpURLConnection\|setAccessible' connectors/http-connector/src/main || echo "no reflection workaround"
```
Expected: `BUILD SUCCESS`, http-connector `Tests run: 680`(676 + 신규 4), `Failures: 0`, `no reflection workaround`.

core-test 의 일부 테스트가 상태 문구나 헤더 대소문자 때문에 실패하면, 실패 메시지를 보고 `reasonPhrase` 매핑 또는 `headers()` 를 고친다. 테스트를 `@SkipTest` 로 건너뛰지 않는다.

- [ ] **Step 8: 커밋과 PR**

```bash
git add -A
git commit -m "feat(http-connector): reimplement on java.net.http.HttpClient

HttpURLConnection cannot send PATCH and the reflection workaround is
blocked by JDK 16+ module encapsulation, so the connector was excluded on
JDK 17+. It now uses the JDK HttpClient and still has no external
dependencies. Known limitation: Config.withHostnameVerifier is ignored
(the JDK HttpClient has no HostnameVerifier hook).

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
공통 PR 절차를 따른다. 라벨 `breaking`(hostname verifier 동작 변경).

---

### Task 9: okhttp connector 를 OkHttp 4 로 전환

**Files:**
- Modify: `connectors/okhttp/pom.xml`, `connectors/pom.xml`, `core-test/pom.xml` (mockwebserver)
- Modify: `connectors/okhttp/src/main/java/org/openstack4j/connectors/okhttp/HttpCommand.java`
- Create: `connectors/okhttp/src/test/java/org/openstack4j/connectors/okhttp/OkHttpConnectorTest.java`

**Interfaces:**
- Produces: mockwebserver 4.12.0 (패키지 `okhttp3.mockwebserver` 그대로). 표시 이름 유지.

- [ ] **Step 1: 사용자 SSLContext 테스트 작성**

```bash
git switch main && git pull && git switch -c task/09-okhttp4
mkdir -p connectors/okhttp/src/test/java/org/openstack4j/connectors/okhttp
```

`OkHttpConnectorTest.java`:
```java
package org.openstack4j.connectors.okhttp;

import javax.net.ssl.SSLContext;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.openstack4j.core.transport.Config;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.core.transport.HttpRequest;
import org.openstack4j.core.transport.HttpResponse;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class OkHttpConnectorTest {

    private MockWebServer server;

    @BeforeMethod
    public void startServer() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterMethod(alwaysRun = true)
    public void stopServer() throws Exception {
        server.shutdown();
    }

    @Test
    public void customSslContextDoesNotThrow() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        Config config = Config.newConfig().withSSLContext(SSLContext.getDefault());

        HttpResponse response = new HttpExecutorServiceImpl().execute(HttpRequest.builder()
                .endpoint(server.url("/").toString())
                .path("v3/resource")
                .method(HttpMethod.GET)
                .config(config)
                .build());

        Assert.assertNotNull(response, "connector returned null (exception was swallowed and logged)");
        Assert.assertEquals(response.getStatus(), 200);
        response.close();
    }
}
```

- [ ] **Step 2: OkHttp 3.14.9 에서 실패 확인**

```bash
./mvnw -B test -pl connectors/okhttp -am 2>&1 | grep -E 'customSslContextDoesNotThrow|UnsupportedOperationException|Tests run: [0-9]+, Failures' | head -5
```
Expected: `customSslContextDoesNotThrow` 실패, 로그에 `UnsupportedOperationException: clientBuilder.sslSocketFactory(SSLSocketFactory) not supported on JDK 9+`.

- [ ] **Step 3: 버전 갱신**

`connectors/okhttp/pom.xml`: `<okhttp.version>3.14.9</okhttp.version>` → `4.12.0`.
`connectors/pom.xml`: mockwebserver `3.14.9` → `4.12.0`.
`core-test/pom.xml`: mockwebserver `3.3.0` → `4.12.0`.

- [ ] **Step 4: `HttpCommand.java` 수정**

import 정리: `okhttp3.internal.Util` 를 지우고 아래를 추가한다.
```java
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
```

`initialize()` 의 사용자 SSLContext 분기를 교체:
```java
        if (config.getSslContext() != null)
            okHttpClientBuilder.sslSocketFactory(config.getSslContext().getSocketFactory(), defaultTrustManager());
```

클래스에 메서드 추가:
```java
    /**
     * OkHttp requires a trust manager next to the socket factory on JDK 9+. It is used only for certificate chain
     * cleaning (certificate pinning); the TLS handshake still trusts what the configured SSLContext trusts.
     */
    private static X509TrustManager defaultTrustManager() {
        try {
            TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            factory.init((KeyStore) null);
            for (TrustManager trustManager : factory.getTrustManagers()) {
                if (trustManager instanceof X509TrustManager)
                    return (X509TrustManager) trustManager;
            }
            throw new IllegalStateException("No X509TrustManager available from the default TrustManagerFactory");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
```

`execute()` 의 deprecated API 를 OkHttp 4 형태로 바꾼다.
```java
        RequestBody body = null;
        if (request.getEntity() != null) {
            if (InputStream.class.isAssignableFrom(request.getEntity().getClass())) {
                byte[] content = IOUtil.readBytes((InputStream) request.getEntity());
                body = RequestBody.create(content, MediaType.parse(request.getContentType()));
            } else {
                String content = ObjectMapperSingleton.getContext(request.getEntity().getClass()).writer().writeValueAsString(request.getEntity());
                body = RequestBody.create(content, MediaType.parse(request.getContentType()));
            }
        } else if (request.hasJson()) {
            body = RequestBody.create(request.getJson(), MediaType.parse(ClientConstants.CONTENT_TYPE_JSON));
        }
        //Added to address https://github.com/square/okhttp/issues/751
        //Set body as empty byte array if request is POST or PUT and body is sent as null
        if ((request.getMethod() == HttpMethod.POST || request.getMethod() == HttpMethod.PUT) && body == null) {
            body = RequestBody.create(new byte[0], null);
        }
```

- [ ] **Step 5: 테스트 통과 확인**

```bash
./mvnw -B --no-transfer-progress clean install 2>&1 | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD|ERROR' | head
./mvnw -B -q dependency:tree -pl connectors/okhttp -Dincludes=com.squareup.okhttp3,org.jetbrains.kotlin
```
Expected: `BUILD SUCCESS`. okhttp 모듈 `Tests run: 677`, httpclient 679, http-connector 680, it-okhttp·it-httpclient 각 29. 트리에 okhttp 4.12.0, logging-interceptor 4.12.0, kotlin-stdlib.

- [ ] **Step 6: 커밋과 PR**

```bash
git add -A
git commit -m "feat(okhttp): upgrade connector to OkHttp 4.12

Also fixes Config.withSSLContext, which threw UnsupportedOperationException
on JDK 9+ because OkHttp needs an X509TrustManager next to the socket
factory. OkHttp 4 adds kotlin-stdlib as a transitive dependency.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
공통 PR 절차를 따른다.

---

### Task 10: 통합 테스트 정비와 JDK 25

**Files:**
- Modify: `.github/workflows/ci.yaml` (matrix 에 25)
- Modify (조건부): `core-integration-test/pom.xml`, `core-integration-test/it-httpclient/pom.xml`, `core-integration-test/it-okhttp/pom.xml`

**Interfaces:**
- Produces: CI job `build (17)`, `build (21)`, `build (25)`.

- [ ] **Step 1: JDK 25 준비와 현재 스택 확인**

```bash
git switch main && git pull && git switch -c task/10-integration-tests-jdk25
JDK25="$HOME/.cache/openstack4j-jdk/jdk-25"
[ -x "$JDK25/bin/java" ] || { mkdir -p "$JDK25" && curl -sfL "https://api.adoptium.net/v3/binary/latest/25/ga/linux/x64/jdk/hotspot/normal/eclipse" | tar xz -C "$JDK25" --strip-components=1; }
"$JDK25/bin/java" -version 2>&1 | head -1
JAVA_HOME="$JDK25" ./mvnw -B --no-transfer-progress clean install 2>&1 | tee /tmp/jdk25.log | grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$|BUILD|Reactor Summary' 
```
- 전부 통과하면 Step 3 으로 간다(옛 스택 유지, 판단 근거를 PR 에 기록).
- it-* 모듈이 Groovy/Spock 오류(예: `Unsupported class file major version 69`, `groovyjarjarasm`)로 실패하면 Step 2 로 간다.
- 다른 모듈이 실패하면 그 원인을 먼저 고친다.

- [ ] **Step 2 (조건부): Groovy 4 + Spock 2 로 업그레이드**

`core-integration-test/pom.xml` 의 테스트 의존성을 교체한다(`junit:junit`, betamax 두 개는 유지).
```xml
        <dependency>
            <groupId>org.spockframework</groupId>
            <artifactId>spock-core</artifactId>
            <version>2.4-groovy-4.0</version>
            <scope>provided</scope>
        </dependency>
        <dependency> <!-- Betamax 는 JUnit 4 Rule 이라 Spock 2 에서 쓰려면 spock-junit4 가 필요하다 -->
            <groupId>org.spockframework</groupId>
            <artifactId>spock-junit4</artifactId>
            <version>2.4-groovy-4.0</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.groovy</groupId>
            <artifactId>groovy</artifactId>
            <version>4.0.33</version>
            <scope>provided</scope>
        </dependency>
```
(`org.codehaus.groovy:groovy-all:2.4.21` 블록은 지운다.) gmavenplus 를 `4.2.1` 로 올리고, 이 POM 과 `it-*` POM 의 surefire `<version>2.19</version>`, 루트 jar 플러그인 버전 하드코딩 `${maven.jar.plugin.version}` 을 지운다. `<outputDirectory>${basedir}\target</outputDirectory>` 의 역슬래시를 `${project.build.directory}` 로 바꾼다. 원본의 `sonatype-snapshots` `<repositories>` 블록(oss.sonatype.org, 종료됨)도 지운다.

```bash
curl -s https://repo1.maven.org/maven2/org/codehaus/gmavenplus/gmavenplus-plugin/maven-metadata.xml | grep -oP '(?<=<release>)[^<]+'
```
(4.2.1 이 아니면 출력된 최신 버전을 쓴다.)

```bash
JAVA_HOME="$JDK25" ./mvnw -B --no-transfer-progress install -pl core-integration-test,core-integration-test/it-okhttp,core-integration-test/it-httpclient 2>&1 | grep -E 'Tests run:|BUILD|ERROR' | tail -8
./mvnw -B --no-transfer-progress install -pl core-integration-test,core-integration-test/it-okhttp,core-integration-test/it-httpclient 2>&1 | grep -E 'Tests run: [0-9]+, F.*$|BUILD' | tail -3
```
Expected: JDK 25, 17 모두 it-okhttp·it-httpclient 각 `Tests run: 29, Failures: 0`.

Spock 2 문법 차이로 일부 Spec 이 컴파일되지 않으면 오류 메시지대로 고친다(대표적으로 `@Rule` 필드는 `spock-junit4` 가 있으면 그대로 동작한다). 그래도 Betamax 가 동작하지 않으면 `core-integration-test` 모듈을 루트 `<modules>` 에서 주석 처리하고 이유를 남긴다. 이 경우 대체 방안은 하위 프로젝트 C 로 넘긴다(spec 4.5 의 3번).

- [ ] **Step 3: CI 매트릭스에 25 추가**

`.github/workflows/ci.yaml`: `java-version: [ 17, 21 ]` → `java-version: [ 17, 21, 25 ]`.

- [ ] **Step 4: 커밋과 PR**

```bash
git add -A
git commit -m "test: run the build on JDK 25 in CI

<Step 1 결과에 따라 한 줄: 'Groovy 2.4/Spock 1.0 integration tests pass on JDK 25 unchanged.' 또는 'Upgrade integration tests to Groovy 4 / Spock 2.4 (spock-junit4 keeps the Betamax JUnit 4 rule working).'>

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
PR 본문에 Step 1 의 판단 근거(JDK 25 결과 로그 요약)를 적는다. 공통 PR 절차를 따른다.

---

### Task 11: Spring Boot 3 스모크 프로젝트와 Dependabot

**Files:**
- Create: `examples/spring-boot-smoke/pom.xml`
- Create: `examples/spring-boot-smoke/src/main/java/io/github/seogineer/openstack4j/smoke/SmokeApplication.java`
- Create: `examples/spring-boot-smoke/src/test/java/io/github/seogineer/openstack4j/smoke/SmokeApplicationTests.java`
- Create: `.github/dependabot.yml`
- Modify: `.github/workflows/ci.yaml`

**Interfaces:**
- Consumes: `io.github.seogineer:openstack4j-core`, `openstack4j-httpclient` (로컬 `~/.m2` 에 install 된 현재 버전). 표시 이름 `"Apache HttpClient Connector"`(Task 7).

- [ ] **Step 1: `examples/spring-boot-smoke/pom.xml` 작성**

openstack4j-parent 를 상속하지 않는다. 상속하면 parent 의 `dependencyManagement` 가 Boot 의 버전 관리보다 우선해서, Boot 환경 검증이 되지 않는다.

```bash
git switch main && git pull && git switch -c task/11-boot-smoke-dependabot
mkdir -p examples/spring-boot-smoke/src/main/java/io/github/seogineer/openstack4j/smoke examples/spring-boot-smoke/src/test/java/io/github/seogineer/openstack4j/smoke
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.16</version>
        <relativePath/>
    </parent>
    <groupId>io.github.seogineer.examples</groupId>
    <artifactId>spring-boot-smoke</artifactId>
    <version>0.0.0</version>
    <name>OpenStack4j Spring Boot 3 smoke test</name>
    <description>Verifies openstack4j against Spring Boot managed dependency versions. Not published.</description>

    <properties>
        <java.version>17</java.version>
        <openstack4j.version>4.0.0-SNAPSHOT</openstack4j.version>
        <maven.deploy.skip>true</maven.deploy.skip>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-json</artifactId>
        </dependency>
        <dependency>
            <groupId>io.github.seogineer</groupId>
            <artifactId>openstack4j-core</artifactId>
            <version>${openstack4j.version}</version>
        </dependency>
        <dependency>
            <groupId>io.github.seogineer</groupId>
            <artifactId>openstack4j-httpclient</artifactId>
            <version>${openstack4j.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 테스트 작성**

`SmokeApplicationTests.java`:
```java
package io.github.seogineer.openstack4j.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.core.transport.internal.HttpExecutor;
import org.openstack4j.openstack.identity.v3.domain.KeystoneToken;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SmokeApplicationTests {

    @Test
    void httpClientConnectorIsSelected() {
        assertThat(HttpExecutor.create().getExecutorName()).isEqualTo("Apache HttpClient Connector");
    }

    @Test
    void keystoneTokenDeserializesWithBootManagedJackson() throws Exception {
        String json = "{\"token\":{\"methods\":[\"password\"],"
                + "\"expires_at\":\"2026-10-01T00:00:00.000000Z\","
                + "\"user\":{\"id\":\"u1\",\"name\":\"admin\",\"domain\":{\"id\":\"default\",\"name\":\"Default\"}}}}";

        KeystoneToken token = ObjectMapperSingleton.getContext(KeystoneToken.class)
                .readerFor(KeystoneToken.class).readValue(json);

        assertThat(token.getMethods()).containsExactly("password");
        assertThat(token.getUser().getName()).isEqualTo("admin");
    }
}
```

- [ ] **Step 3: 애플리케이션 클래스 없이 실행해 실패 확인**

```bash
./mvnw -B -q install -DskipTests
./mvnw -B -f examples/spring-boot-smoke test 2>&1 | grep -E 'Unable to find a @SpringBootConfiguration|Tests run|BUILD' | head -3
```
Expected: `Unable to find a @SpringBootConfiguration` 로 실패.

- [ ] **Step 4: `SmokeApplication.java` 작성**

```java
package io.github.seogineer.openstack4j.smoke;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SmokeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmokeApplication.class, args);
    }
}
```

- [ ] **Step 5: 통과 확인**

```bash
./mvnw -B -f examples/spring-boot-smoke verify 2>&1 | grep -E 'Tests run:|BUILD' | tail -2
./mvnw -B -q -f examples/spring-boot-smoke dependency:tree -Dincludes=com.fasterxml.jackson.core:jackson-databind,org.apache.httpcomponents.client5
```
Expected: `Tests run: 2, Failures: 0`, `BUILD SUCCESS`. 트리에 Boot 가 정한 jackson-databind 2.21.x, httpclient5 5.5.x 가 나온다(= 라이브러리가 컴파일된 버전보다 낮은 Boot 관리 버전으로 실제 실행됐다는 뜻).

`keystoneTokenDeserializesWithBootManagedJackson` 이 실패하면 Jackson 2.22 전용 API 를 core 가 쓰고 있다는 뜻이다. 이 경우 루트 `jackson.version` 을 Boot 3.5 가 정한 2.21.x 로 낮추는 것을 검토하고 사용자에게 알린다.

- [ ] **Step 6: CI 에 스모크 단계 추가**

`.github/workflows/ci.yaml` 의 `Build and test` 단계 뒤에 추가한다.
```yaml
      - name: Spring Boot 3 smoke test
        run: |
          VERSION=$(./mvnw -q help:evaluate -Dexpression=project.version -DforceStdout)
          ./mvnw -B --no-transfer-progress -f examples/spring-boot-smoke verify -Dopenstack4j.version="$VERSION"
```

- [ ] **Step 7: `.github/dependabot.yml` 작성**

```yaml
version: 2
updates:
  - package-ecosystem: maven
    directory: /
    schedule:
      interval: weekly
    open-pull-requests-limit: 10
    labels: [ dependencies ]
  - package-ecosystem: maven
    directory: /examples/spring-boot-smoke
    schedule:
      interval: weekly
    labels: [ dependencies ]
    ignore:
      # 스모크 대상은 Spring Boot 3 계열이다. Boot 4 지원은 별도로 결정한다.
      - dependency-name: "org.springframework.boot:*"
        update-types: [ "version-update:semver-major" ]
  - package-ecosystem: github-actions
    directory: /
    schedule:
      interval: weekly
    labels: [ dependencies ]
```

- [ ] **Step 8: 커밋과 PR**

```bash
git add -A
git commit -m "test: add Spring Boot 3 smoke project and Dependabot

The smoke project is not part of the reactor so that Spring Boot, not the
openstack4j parent, manages dependency versions. CI runs it after install.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
공통 PR 절차를 따른다. CI 의 모든 매트릭스에서 스모크 단계가 pass 여야 한다.

---

### Task 12: 문서와 4.0.0 릴리스

**Files:**
- Create: `MIGRATION.md`
- Modify: `README.md`, `connectors/README.md`, `CHANGELOG.md`

- [ ] **Step 1: `MIGRATION.md` 작성**

```bash
git switch main && git pull && git switch -c task/12-docs-release-4.0.0
```

````markdown
# 3.x → 4.0 이전 가이드

4.0 은 원본 openstack4j 3.x 의 후속 포크의 첫 릴리스입니다. Java 패키지(`org.openstack4j`)와 API 는 그대로이고, 주로 빌드 좌표와 실행 환경이 바뀌었습니다.

## 1. 요구 사항

- **JDK 17 이상** (3.x 는 Java 8 타깃)

## 2. Maven 좌표

groupId 가 하나로 합쳐졌습니다. artifactId 는 같습니다.

| 3.x | 4.0 |
|---|---|
| `com.github.openstack4j.core:openstack4j` | `io.github.seogineer:openstack4j` |
| `com.github.openstack4j.core:openstack4j-core` | `io.github.seogineer:openstack4j-core` |
| `com.github.openstack4j.core.connectors:openstack4j-httpclient` | `io.github.seogineer:openstack4j-httpclient` |
| `com.github.openstack4j.core.connectors:openstack4j-okhttp` | `io.github.seogineer:openstack4j-okhttp` |
| `com.github.openstack4j.core.connectors:openstack4j-http-connector` | `io.github.seogineer:openstack4j-http-connector` |

```xml
<dependency>
    <groupId>io.github.seogineer</groupId>
    <artifactId>openstack4j</artifactId>
    <version>4.0.0</version>
</dependency>
```

3.x 와 4.0 은 같은 패키지를 쓰므로 **한 클래스패스에 함께 두지 마세요.**

## 3. connector

| connector | 4.0 |
|---|---|
| jersey2 | **제거** (javax 기반, Spring Boot 3 과 충돌) |
| resteasy | **제거** (같은 이유) |
| httpclient | Apache HttpClient **5** 로 전환. `openstack4j`(distribution)의 기본 connector |
| okhttp | OkHttp **4.12** 로 전환. kotlin-stdlib 가 전이 의존성으로 추가됨 |
| http-connector | JDK `java.net.http.HttpClient` 로 재구현. JDK 17+ 에서 PATCH 가 동작함 |

jersey2·resteasy 를 쓰던 경우 `openstack4j-httpclient` 로 바꾸세요. 코드 변경은 필요 없습니다(connector 는 ServiceLoader 로 선택됩니다).

### httpclient connector 를 직접 설정하던 경우

`HttpClientConfigInterceptor` 와 `HttpResponseImpl.unwrap()` 이 HttpClient 5 타입을 씁니다.

```java
// 3.x
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
// 4.0
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
```

`RequestConfig.Builder.setSocketTimeout(...)` 은 `setResponseTimeout(Timeout.ofMilliseconds(...))` 로 바뀌었습니다.

### http-connector 의 TLS 제약

- `Config.withHostnameVerifier(...)` 는 무시되고 WARN 로그가 남습니다(JDK HttpClient 에 해당 기능이 없음).
- `Config.withSSLVerificationDisabled()` 는 인증서 검증만 끕니다. 호스트명 검증까지 끄려면 JVM 옵션 `-Djdk.internal.httpclient.disableHostnameVerification=true` 를 쓰세요.

## 4. 의존성

| 의존성 | 3.12 | 4.0 |
|---|---|---|
| Jackson | 2.14 | 2.22 |
| Guava | 29 | 33 |
| SnakeYAML | 1.33 | 2.7 |
| SLF4J API | 1.7 | 2.0 |
| json-patch | `com.github.fge` 1.9 | `com.github.java-json-tools` 1.13 (패키지 동일) |
| jsr305 | compile | provided (전이되지 않음) |

## 5. 그 밖의 제거

- OSGi Karaf `features.xml` 부가 아티팩트(2014년 이후 갱신되지 않았음). 번들 manifest 는 그대로 있습니다.
````

- [ ] **Step 2: `README.md` 본문 갱신**

Task 2 에서 바꾼 머리말 아래, `## Documentation and Support` 부터 파일 끝까지를 아래로 교체한다.

````markdown
## 설치

```xml
<dependency>
    <groupId>io.github.seogineer</groupId>
    <artifactId>openstack4j</artifactId>
    <version>4.0.0</version>
</dependency>
```

`openstack4j` 는 core 와 Apache HttpClient 5 connector 를 묶은 아티팩트입니다. 다른 connector 를 쓰려면 [connectors/README.md](connectors/README.md) 를 보세요.

## 지원 환경

| 구성 | JDK 17 | JDK 21 | JDK 25 |
|---|---|---|---|
| core | 🟢 | 🟢 | 🟢 |
| httpclient (HttpClient 5) | 🟢 | 🟢 | 🟢 |
| okhttp (OkHttp 4) | 🟢 | 🟢 | 🟢 |
| http-connector (JDK HttpClient) | 🟢 | 🟢 | 🟢 |

Spring Boot 3.5 와 함께 쓰는 구성을 CI 에서 확인합니다(`examples/spring-boot-smoke`).

## 사용 예

```java
OSClient.OSClientV3 os = OSFactory.builderV3()
        .endpoint("https://keystone.example.com:5000/v3")
        .credentials("admin", "secret", Identifier.byName("Default"))
        .scopeToProject(Identifier.byName("admin"), Identifier.byName("Default"))
        .authenticate();

List<? extends Server> servers = os.compute().servers().list();
```

원본 문서([openstack4j.github.io](https://openstack4j.github.io/))의 사용법이 그대로 적용됩니다. Maven 좌표만 위의 것으로 바꾸세요.

## 빌드

```bash
./mvnw verify
```

## 이전 버전에서 옮기기

[MIGRATION.md](MIGRATION.md)

## 버그 신고

[GitHub Issues](https://github.com/seogineer/openstack4j/issues)

## 라이선스

Apache License 2.0. 원본 openstack4j 기여자들의 작업에 기반합니다([NOTICE](NOTICE)).
````

- [ ] **Step 3: `connectors/README.md` 갱신**

jersey2·resteasy 절을 지우고, 남은 세 connector 의 좌표를 `io.github.seogineer` / `4.0.0` 으로 적는다. http-connector 절에 Step 1 의 "TLS 제약" 두 줄을 넣는다. 확인:

```bash
grep -n -i 'jersey\|resteasy\|com\.github\.openstack4j' connectors/README.md README.md || echo clean
```
Expected: `clean`.

- [ ] **Step 4: `CHANGELOG.md` 맨 위에 항목 추가**

```markdown
## 4.0.0 (seogineer fork)

첫 후속 포크 릴리스. 이전 방법은 MIGRATION.md 참고.

- BREAKING: groupId `io.github.seogineer`, JDK 17 이상 필요
- BREAKING: jersey2, resteasy connector 제거
- BREAKING: httpclient connector 가 Apache HttpClient 5 사용
- http-connector 를 JDK HttpClient 로 재구현 (JDK 17+ 에서 PATCH 동작)
- okhttp connector 를 OkHttp 4.12 로 갱신, `Config.withSSLContext` 가 JDK 9+ 에서 예외를 던지던 문제 수정
- Jackson 2.22, Guava 33, SnakeYAML 2.7, SLF4J 2.0, json-patch 1.13 으로 갱신
- Maven Central 배포를 Central Portal 로 전환, CI 를 JDK 17/21/25 와 Spring Boot 3 스모크 테스트로 확장
```

- [ ] **Step 5: 커밋, PR, 머지**

```bash
./mvnw -B -q verify
git add -A
git commit -m "docs: add migration guide and update README for 4.0.0

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
공통 PR 절차를 따른다(머지 포함).

- [ ] **Step 6: 🛑 사용자 실환경 확인 (spec 7절 완료 기준 4)**

사용자가 직접 테스트용 OpenStack 에서 확인한다. 이 코드는 저장소에 넣지 않는다. 계정 정보는 대화창에 붙여 넣지 말고 사용자 터미널에서만 입력한다.

```bash
cd ~/IdeaProjects/openstack4j && ./mvnw -B -q install -DskipTests
CP=$(./mvnw -q -pl distribution dependency:build-classpath -Dmdep.outputFile=/dev/stdout):distribution/target/openstack4j.jar
jshell --class-path "$CP"
```
```java
import org.openstack4j.openstack.OSFactory;
import org.openstack4j.model.common.Identifier;
var os = OSFactory.builderV3().endpoint("https://<keystone>:5000/v3").credentials("<user>", "<password>", Identifier.byName("Default")).scopeToProject(Identifier.byName("<project>"), Identifier.byName("Default")).authenticate();
os.compute().servers().list().size();
os.placement().resourceProviders().list().size();
```
Expected: 인증 성공, 두 줄 모두 숫자 반환. 사설 인증서 환경이면 `OSFactory.builderV3().withConfig(org.openstack4j.core.transport.Config.newConfig().withSSLVerificationDisabled())...` 를 쓴다.

사용자에게 결과를 받은 뒤 다음 Step 으로 간다. 실패하면 오류를 분석해 수정 PR 을 먼저 낸다.

- [ ] **Step 7: 🛑 `v4.0.0` 배포 (사용자 확인 필수)**

```bash
git switch main && git pull
git tag -a v4.0.0 -m "OpenStack4j 4.0.0 (seogineer fork)"
git push origin v4.0.0
gh run watch -R seogineer/openstack4j $(gh run list -R seogineer/openstack4j -w Release -L 1 --json databaseId -q '.[0].databaseId')
```
Task 5 Step 8 과 같은 방법으로 Central 반영(`openstack4j-core/4.0.0/...pom` → 200)과 GitHub Release `v4.0.0`(prerelease 아님)을 확인한다. 추가로 `openstack4j-http-connector` 4.0.0 도 확인한다.

- [ ] **Step 8: 다음 개발 버전으로 올리기**

다음 릴리스는 Placement 확장(하위 프로젝트 B)이 들어가는 4.1.0 이다.

```bash
git switch -c chore/bump-4.1.0-snapshot
./mvnw -B -q versions:set -DnewVersion=4.1.0-SNAPSHOT -DgenerateBackupPoms=false -DprocessAllModules=true
sed -i 's|<openstack4j.version>4.0.0-SNAPSHOT</openstack4j.version>|<openstack4j.version>4.1.0-SNAPSHOT</openstack4j.version>|' examples/spring-boot-smoke/pom.xml
./mvnw -B -q verify
git add -A
git commit -m "chore: bump version to 4.1.0-SNAPSHOT

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
공통 PR 절차를 따른다.
