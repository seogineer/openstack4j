# openstack4j 후속 포크 — A. 기반 및 최신화 설계

- 작성일: 2026-10-01
- 상태: 사용자 검토 대기
- 대상 저장소: `github.com/seogineer/openstack4j` (원본 `openstack4j/openstack4j` 의 `main`, 커밋 `fe5a4cd`, 2024-05-10 에서 포크)

## 1. 배경과 목적

원본 openstack4j 는 2024-05 의 3.12 릴리스 이후 개발이 멈췄다. 아직 Java 1.8 타깃이고, 의존성이 오래됐으며(SnakeYAML 1.33, HttpClient 4.3.6 등), javax 기반 connector 가 Spring Boot 3 과 충돌한다.

이 포크의 목적은 두 가지다.

1. **업무 사용** — JDK 17+ / Spring Boot 3.x 환경에서 문제없이 쓸 수 있는 OpenStack Java SDK.
2. **메인테이너 경험** — CI, 릴리스, Maven Central 배포를 갖춘 오픈소스 프로젝트 운영.

### 하위 프로젝트 분해

| 순서 | 하위 프로젝트 | 범위 |
|---|---|---|
| **A** | 기반 및 최신화 (**이 문서**) | 좌표 변경, JDK 17, 의존성·connector 최신화, CI, Maven Central 배포, 4.0.0 릴리스 |
| B | Placement 확장 | 용량·사용량 조회 보강, 쓰기 API(Inventory, Trait, Aggregate, Resource class), microversion 지원 |
| C | 이후 과제 | JDK `HttpClient` connector, JUnit 5 전환, 원본에 남은 Issue/PR 흡수, 다른 서비스 보강 |

B 와 C 는 각자 별도의 spec 과 plan 을 갖는다.

### 제약

- 회사(이노그리드)의 사내 openstack4j 수정본 코드는 **가져오지 않는다**. 사내 프로젝트는 요구사항(사용 환경, 필요한 API) 확인용으로만 참고한다.
- 라이선스는 Apache 2.0 을 유지한다.

## 2. 결정 사항

| 항목 | 결정 | 이유 |
|---|---|---|
| Java 패키지 | `org.openstack4j.*` **유지** | 좌표만 바꾸면 기존 코드가 동작하고, 원본 패치를 흡수하기 쉽다 |
| groupId | `io.github.seogineer` (단일 groupId) | Central Portal 이 GitHub 계정으로 namespace 를 인증한다 |
| artifactId | 원본 유지 (`openstack4j-core` 등) | 옮겨 오기 쉽다 |
| 최소 JDK | **17** | Spring Boot 3 의 최소 요구사항이고 업무 환경과 일치한다 |
| connector | httpclient(→ HttpClient 5), okhttp(→ 4.x), http-connector **유지** / jersey2, resteasy **제거** | javax 기반 connector 를 Jakarta 로 옮기는 비용이 사용자 수에 비해 크다 |
| 빌드 도구 | Maven 유지 | 원본 PR 흡수가 쉽다 |
| 단위 테스트 | TestNG 유지 | JUnit 5 전환은 C 로 미룬다 |
| 버전 | **4.0.0** 부터 SemVer | JDK, connector, groupId 변경 모두 하위 호환이 깨지는 변경이다 |
| 진행 방식 | 단계별 점진 전환 — 작은 PR 을 순서대로, 각 PR 은 빌드 통과 상태로 머지 | 회귀 지점을 찾기 쉽고 PR 이력이 남는다 |

## 3. 저장소와 모듈 구조

### 저장소

- `origin` = `seogineer/openstack4j`, `upstream` = `openstack4j/openstack4j`. 원본 PR cherry-pick 에 `upstream` 을 쓴다.
- README 맨 위에 후속 포크라는 사실과 원본과의 차이를 밝힌다.
- `MIGRATION.md` 에 3.x → 4.0 이전 방법(좌표 변경, 제거된 connector, JDK 17)을 정리한다.
- `LICENSE` 는 유지하고, `NOTICE` 를 새로 만들어 원저작자(openstack4j contributors)를 명시한다.

### Maven 좌표 (4.0.0)

| 모듈 | 좌표 |
|---|---|
| parent | `io.github.seogineer:openstack4j-parent` |
| core | `io.github.seogineer:openstack4j-core` |
| core-test | `io.github.seogineer:openstack4j-core-test` (배포하지 않음) |
| connectors parent | `io.github.seogineer:openstack4j-connectors` |
| connector | `io.github.seogineer:openstack4j-httpclient`, `openstack4j-okhttp`, `openstack4j-http-connector` |
| distribution | `io.github.seogineer:openstack4j` (core + 기본 connector 묶음) |

원본의 하위 groupId(`com.github.openstack4j.core.connectors` 등)는 쓰지 않는다.

### 제거

- `connectors/jersey2`, `connectors/resteasy`
- `core-integration-test/it-jersey2`, `core-integration-test/it-resteasy`
- `.travis.yml`, `release.sh`, `maven-release-plugin`, `maven-eclipse-plugin`, `nexus-staging-maven-plugin`

### 유지

- `core`, `core-test`(TestNG 단위 테스트 152개), `connectors/{httpclient,okhttp,http-connector}`
- `core-integration-test/{it-httpclient,it-okhttp}` (5.4 의 조건부)
- `.github/release-drafter.yml`, Issue/PR 템플릿

## 4. 빌드와 의존성

### 4.1 컴파일러와 플러그인

- `maven.compiler.release=17`. `source/target 1.8` 은 없앤다.
- `maven-enforcer-plugin`: JDK 17 이상, Maven 3.9 이상을 요구한다.
- 플러그인 버전은 parent 의 `pluginManagement` 에서 한 곳에 관리하고, 작업 시점의 최신 안정 버전으로 올린다(compiler, surefire, jar, source, javadoc, gpg, shade, bundle).
- core 의 `maven-shade-plugin`(1.3.3, `withdeps` classifier)과 `maven-bundle-plugin`(OSGi 메타데이터)은 기능을 유지한 채 버전만 올린다.

### 4.2 런타임 의존성

모든 버전은 parent 의 `<properties>` 와 `dependencyManagement` 에서 관리한다. 아래 "4.0.0" 열의 버전은 작업 시점의 최신 안정 버전으로 확정하되, 표에 적힌 메이저 버전을 따른다.

| 의존성 | 현재 | 4.0.0 |
|---|---|---|
| Jackson (databind, dataformat-yaml) | 2.14.2 | 최신 2.x |
| Guava | 29.0-jre | 최신 `-jre` |
| SnakeYAML | 1.33 | 2.x (API 변경분 코드 수정) |
| SLF4J API | 1.7.21 | 2.0.x |
| json-patch | `com.github.fge:json-patch` 1.9 | `com.github.java-json-tools:json-patch` 최신 |
| jsr305 | `com.google.code.findbugs:jsr305` 2.0.0 | 3.0.2, `provided` scope |

core 의 `javax.*` 사용은 `javax.annotation`(jsr305)과 `javax.net.ssl`(JDK)뿐이라 Jakarta 전환은 필요 없다.

### 4.3 connector

| connector | 변경 |
|---|---|
| `openstack4j-httpclient` | HttpClient 4.3.6 → **HttpClient 5** (`org.apache.httpcomponents.client5:httpclient5`). API 가 달라 connector 구현을 다시 쓴다. 공개 클래스 이름과 `HttpExecutorService` SPI 등록 방식은 유지한다. |
| `openstack4j-okhttp` | OkHttp 3.14.9 → **4.12.x**. Kotlin stdlib 이 전이 의존성으로 추가된다. |
| `openstack4j-http-connector` | 변경 없음 (외부 의존성 없음) |

### 4.4 테스트 의존성

- TestNG → 7.x, mockwebserver 3.3.0 → 4.12.x (okhttp 와 버전 일치).

### 4.5 통합 테스트 (조건부)

현재 Groovy 2.4.21 + Spock 1.0 + Betamax 2.0.1 이다. 다음 순서로 판단한다.

1. JDK 17/21 에서 그대로 통과하면 유지한다.
2. 실패하면 Groovy 4 + Spock 2 로 올린다.
3. 그래도 Betamax(JUnit 4 rule 기반) 때문에 동작하지 않으면 `core-integration-test` 를 빌드에서 제외하고, 대체 방안은 C 로 넘긴다.

어느 경우든 판단 근거를 해당 PR 설명에 기록한다.

### 4.6 Spring Boot 3 스모크 검증

`examples/spring-boot-smoke` — Spring Boot 3.5 BOM 을 import 하고 `openstack4j-core` + `openstack4j-httpclient` 를 의존하는 최소 프로젝트. `OSFactory.builderV3()` 로 클라이언트를 구성하는 코드가 컴파일되고 애플리케이션 컨텍스트가 뜨는지(OpenStack 호출 없이) 확인한다. 배포 대상이 아니며 CI 에서 빌드한다. 루트 `pom.xml` 의 모듈 목록에 넣되 Central 배포 대상에서는 제외한다.

## 5. CI 와 배포

### 5.1 CI (`.github/workflows/ci.yaml`)

- 트리거: `push`(main), `pull_request`.
- 매트릭스: JDK 17, 21, 25 (Temurin).
- 명령: `mvn -B --no-transfer-progress verify`.
- `actions/setup-java` 의 `cache: maven` 사용.

### 5.2 Dependabot (`.github/dependabot.yml`)

- `maven` 과 `github-actions` 를 매주 확인한다.

### 5.3 Maven Central 배포

- OSSRH(`nexus-staging-maven-plugin`)를 **Central Portal**(`org.sonatype.central:central-publishing-maven-plugin`)로 교체한다.
- 산출물: jar, sources jar, javadoc jar, GPG 서명. POM 에 name, description, url, licenses, scm, developers 를 채운다.
- `core-test`, `examples/spring-boot-smoke` 는 배포하지 않는다.

**사용자 사전 준비 (수동)**

1. central.sonatype.com 에 GitHub 계정으로 로그인 → `io.github.seogineer` namespace 인증.
2. GPG 키 생성, 공개키를 keyserver(keys.openpgp.org 등)에 등록.
3. 저장소 Secrets 등록: `CENTRAL_USERNAME`, `CENTRAL_TOKEN`, `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE`.

### 5.4 릴리스 흐름 (`.github/workflows/release.yml`)

- main 의 버전은 항상 다음 릴리스의 SNAPSHOT(예: `4.0.0-SNAPSHOT`)이다.
- `v*` tag push 시:
  1. `mvn versions:set -DnewVersion=<tag 에서 v 를 뺀 값>` (커밋하지 않음)
  2. `mvn -B verify`
  3. 서명 후 Central Portal 에 배포 (`autoPublish` 사용)
  4. release-drafter 로 GitHub Release 생성
- 릴리스 후 SNAPSHOT 버전 올리기는 수동 PR 로 한다.
- SNAPSHOT 배포는 하지 않는다.

### 5.5 배포 파이프라인 조기 검증

최신화 작업(의존성·connector) 전에 `4.0.0-alpha.1` 을 실제로 Central 에 배포해 namespace, 서명, POM 요건을 확인한다. 이 alpha 는 groupId 와 JDK 설정만 바뀐 코드이므로 README 와 Release 노트에 "파이프라인 검증용, 사용 금지" 라고 명시한다.

## 6. 작업 순서

각 PR 은 main 에서 빌드가 통과하는 상태로 머지한다.

| # | PR | 내용 | 확인 |
|---|---|---|---|
| 1 | 포크 정리 | README 후속 포크 안내, NOTICE, `.travis.yml`·`release.sh` 제거 | 원본 상태로 CI 통과 |
| 2 | 좌표 변경 | groupId `io.github.seogineer`, 버전 `4.0.0-SNAPSHOT`, Central 용 POM 메타데이터 | `mvn verify` |
| 3 | jersey2·resteasy 제거 | connector 와 대응 통합 테스트 모듈 삭제 | `mvn verify` |
| 4 | JDK 17 + 빌드 플러그인 | `release=17`, enforcer, 플러그인 갱신, CI 매트릭스 17/21/25 + PR 트리거 | CI 매트릭스 통과 |
| 5 | 배포 파이프라인 | Central Portal 플러그인, `release.yml`, `v4.0.0-alpha.1` 배포 | Central 에서 아티팩트 확인 |
| 6 | core 의존성 갱신 | Jackson, Guava, SnakeYAML 2, SLF4J 2, json-patch, jsr305 | 단위 테스트 통과 |
| 7 | HttpClient 5 전환 | `openstack4j-httpclient` 재작성 | 단위 테스트 (+ it-httpclient) |
| 8 | OkHttp 4 전환 | okhttp, mockwebserver 갱신 | 단위 테스트 (+ it-okhttp) |
| 9 | 통합 테스트 정비 | 4.5 의 판단 절차 수행 | CI 통과 |
| 10 | Boot 3 스모크 + Dependabot | `examples/spring-boot-smoke`, `dependabot.yml` | CI 에서 스모크 빌드 |
| 11 | 문서와 4.0.0 릴리스 | `MIGRATION.md`, README, CHANGELOG, `v4.0.0` tag | Central 배포, GitHub Release |

7 과 8 은 서로 독립이다. 9 의 범위는 7·8 결과에 따라 정해진다.

## 7. 완료 기준

1. `io.github.seogineer:openstack4j-core:4.0.0`, connector 3종, `openstack4j`(distribution)가 Maven Central 에 있다.
2. CI 가 JDK 17/21/25 에서 통과하고 PR 에서도 실행된다.
3. `examples/spring-boot-smoke` 가 Spring Boot 3.5 BOM 과 함께 충돌 없이 빌드된다.
4. 실제 OpenStack 환경에서 Keystone v3 인증과 기본 조회(서버 목록, Placement resource provider 목록)를 수동으로 한 번 확인한다. 이 확인 코드는 포크에 넣지 않는다.
5. `MIGRATION.md` 만 보고 3.x 사용자가 좌표를 바꿔 이전할 수 있다.

## 8. 범위 밖

- Placement 확장, microversion 지원 → B
- JDK `HttpClient` connector, JUnit 5 전환, 원본 Issue/PR 흡수, 다른 서비스 보강, 통합 테스트 대체(4.5 의 3번 경우) → C
- SNAPSHOT 배포, 릴리스 후 버전 자동 커밋
