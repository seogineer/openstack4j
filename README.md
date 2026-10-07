OpenStack4j (seogineer fork)
============================

[![CI](https://github.com/seogineer/openstack4j/actions/workflows/ci.yaml/badge.svg)](https://github.com/seogineer/openstack4j/actions/workflows/ci.yaml)
[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](LICENSE)

> **이 저장소는 [openstack4j/openstack4j](https://github.com/openstack4j/openstack4j) 의 후속 포크입니다.**
> 원본은 2024-05 의 3.12 이후 개발이 멈췄습니다. 이 포크는 JDK 17+ / Spring Boot 3 지원과 최신 의존성을 목표로 하며,
> Java 패키지(`org.openstack4j`)는 그대로 두고 Maven 좌표만 `io.github.seogineer` 로 바꿉니다.
> 3.x 에서 옮겨 오는 방법은 [MIGRATION.md](MIGRATION.md) 를 보세요.

OpenStack4j is a fluent OpenStack client that allows provisioning and control of an OpenStack deployment.   This includes support for Identity, Compute, Image, Network, Block Storage, Telemetry, Data Processing as well as many extensions (LBaaS, FWaaS, Quota-Sets, etc)

## 설치

```xml
<dependency>
    <groupId>io.github.seogineer</groupId>
    <artifactId>openstack4j</artifactId>
    <version>4.0.0</version>
</dependency>
```

`openstack4j` 는 core 와 Apache HttpClient 5 connector 를 함께 끌어오는 아티팩트입니다. 다른 connector 를 쓰려면 [connectors/README.md](connectors/README.md) 를 보세요.

## 지원 환경

| 구성 | JDK 17 | JDK 21 | JDK 25 |
|---|---|---|---|
| core | 🟢 | 🟢 | 🟢 |
| httpclient (Apache HttpClient 5) | 🟢 | 🟢 | 🟢 |
| okhttp (OkHttp 4) | 🟢 | 🟢 | 🟢 |
| http-connector (JDK HttpClient) | 🟢 | 🟢 | 🟢 |

Spring Boot 3.5 와 함께 쓰는 구성을 CI 에서 확인합니다(`examples/spring-boot-smoke`).

## 문서

원본 문서([openstack4j.github.io](https://openstack4j.github.io/))와 아래 사용 예가 그대로 적용됩니다. Maven 좌표만 위의 것으로 바꾸세요.
3.x 에서 옮겨 오는 방법은 [MIGRATION.md](MIGRATION.md), 변경 내역은 [CHANGELOG.md](CHANGELOG.md) 를 보세요.

## Placement

4.1.0 부터 Placement API 전체(microversion 1.28~1.39)를 지원합니다. 서버의 microversion 범위를 자동으로 협상하고, 원하면 고정할 수 있습니다.

```java
PlacementService placement = os.placement();
placement.versions().get();                      // 서버 범위와 협상된 버전
placement.useMicroVersion("1.36");               // 선택: 버전 고정

// 용량 조회
Map<String, ResourceCapacity> capacity = placement.usages().capacity(rpUuid);

// inventory 수정 (generation 충돌 시 재시도)
Placement.retryOnConcurrentUpdate(3, () -> {
    ResourceProviderInventories inv = placement.inventories().list(rpUuid);
    return placement.inventories().update(rpUuid, inv.getResourceProviderGeneration(), "VCPU",
            Inventory.builder().total(64).allocationRatio(4.0f).build());
});

// 스케줄링 후보
placement.allocationCandidates().list(AllocationCandidatesQuery.builder()
        .resources("VCPU", 2).resources("MEMORY_MB", 2048).required("HW_CPU_X86_AVX2").limit(10).build());
```

기존 `placement().resourceProviders()` 는 그대로 동작합니다. 1.28 보다 오래된 서버에서는 새 API 가 `PlacementMicroVersionException` 을 던집니다.

## Compute microversion

4.2.0 부터 Nova microversion(2.1~2.104)을 선택적으로 켤 수 있습니다. 기본값은 꺼짐이라, 켜지 않으면 요청에 microversion 헤더가 붙지 않고(Nova 는 2.1 로 처리) 기존 코드는 그대로 동작합니다.

```java
ComputeVersion v = os.compute().microVersions().negotiate();   // min(2.104, 서버 최대)
os.compute().microVersions().use("2.79");                      // 또는 버전 고정
os.compute().microVersions().clear();                          // 다시 헤더 없음

Server server = os.compute().servers().get(id);
server.getFlavorSummary().getOriginalName();                   // 2.47+ 내장 flavor
server.getTags();                                              // 2.26+

os.compute().servers().lock(id, "maintenance");                // 2.73+
os.compute().servers().list(ServerListOptions.create().locked(true).tags("web"));
os.compute().servers().boot(Builders.server().name("vm").flavor(f).image(i)
        .autoAllocateNetwork().hostname("vm-01").build());     // 2.37+, 2.90+
```

- Nova 가 이후 microversion 에서 없앤 API 를 쓰는 기존 메서드(`floatingIps()`, `securityGroups()`, `images()`, `host()` 같은 프록시 API, 기존 `getVNCConsole`, `diagnostics`, 공개키 없는 키페어 생성 등)는 지원하는 가장 높은 microversion 으로 보내므로 `negotiate()` 후에도 계속 동작합니다.
- 새 기능은 필요한 최소 microversion 을 요청 전에 검사하고, 세션이 그 버전을 보내지 않으면(또는 꺼져 있으면) `MicroVersionException` 을 던집니다.
- 선택한 버전은 클라이언트 세션과 compute endpoint 별로 유지됩니다.

## Block storage microversion

4.3.0 부터 Cinder v3 microversion(3.0~3.71)을 선택적으로 켤 수 있습니다. 기본값은 꺼짐이라, 켜지 않으면 요청에 microversion 헤더가 붙지 않고(Cinder 는 3.0 으로 처리) 기존 코드는 그대로 동작합니다.

```java
BlockStorageVersion v = os.blockStorage().microVersions().negotiate();   // min(3.71, 서버 최대)
os.blockStorage().microVersions().use("3.50");                            // 또는 버전 고정
os.blockStorage().microVersions().clear();                                // 다시 헤더 없음

Volume volume = os.blockStorage().volumes().get(id);
volume.getVolumeTypeId();                                                 // 3.63+
volume.getConsumesQuota();                                                // 3.65+

os.blockStorage().volumes().list(VolumeListOptions.create().status("available").withCount(true));   // 3.45+
os.blockStorage().attachments().create(volumeId, serverId, null, "rw");   // 3.27+, mode 3.54+
os.blockStorage().groups().create(GroupCreate.create("g", groupTypeId, List.of(typeId)));         // 3.13+
```

- Cinder 가 3.53 부터 거부하는 생성 본문의 `bootable`, 3.66 부터 거부하는 스냅샷 `force=false` 는 각각 3.52/3.65 로 보내므로 기존 호출이 `negotiate()` 후에도 계속 동작합니다. 그 밖의 기존 메서드는 3.71 까지 형식이 같습니다.
- 새 기능은 필요한 최소 microversion 을 요청 전에 검사하고, 세션이 그 버전을 보내지 않으면(또는 꺼져 있으면) `MicroVersionException` 을 던집니다.
- compute 와 block storage 의 microversion 상태는 서로 독립이며, 클라이언트 세션과 endpoint 별로 유지됩니다.

## Identity v3 확장

4.4.0 부터 Keystone 의 인증 방식과 확장 API(OS-*)를 지원합니다. 기존 인증 본문과 메서드는 바뀌지 않습니다.

```java
// application credential (scope 없이 — credential 자체가 project 에 묶여 있음)
OSClientV3 os = OSFactory.builderV3().endpoint(url).applicationCredential(credentialId, secret).authenticate();

// MFA(password + TOTP), system scope, trust scope
OSFactory.builderV3().endpoint(url).credentials("alice", password, Identifier.byName("Default")).passcode("123456").authenticate();
OSFactory.builderV3().endpoint(url).credentials("admin", password, Identifier.byName("Default")).scopeToSystem().authenticate();
OSFactory.builderV3().endpoint(url).token(trusteeToken).scopeToTrust(trustId).authenticate();

os.identity().applicationCredentials().create(userId, ApplicationCredentialCreate.create("ci").roleNames("member"));
os.identity().projects().addTag(projectId, "prod");
os.identity().projects().list(ProjectListOptions.create().tags("prod"));
os.identity().trusts().create(TrustCreate.create(trustorId, trusteeId, false).projectId(projectId).roleNames("member"));
os.identity().registeredLimits().create(List.of(RegisteredLimitCreate.create(computeServiceId, "cores", 20)));
os.identity().federation().identityProviders().list();
```

- 새 accessor: `applicationCredentials()`, `trusts()`, `endpointFilter()`, `endpointPolicies()`, `limits()`, `registeredLimits()`, `federation()`, `oauth1()`(HMAC-SHA1 서명 포함), `oauth2()`, `revocationEvents()`, `systemRoles()`
- 기존 서비스 보강: project tags, OS-INHERIT 상속 역할, implied roles·role inferences, domain configuration, access rules, `UserListOptions`/`ProjectListOptions`
- TOTP 세션은 passcode 가 일회용이라 토큰이 만료되면 자동 재인증할 수 없습니다. 새 passcode 로 다시 `authenticate()` 하세요.
- 같은 스레드에서 `authenticate()` 를 다시 하면 그 클라이언트가 현재 세션이 됩니다. 이전 클라이언트로 돌아가려면 `OSFactory.clientFromToken(token)` 을 쓰세요.

## Networking 확장

4.5.0 부터 Neutron 본체(in-tree)의 API 를 대부분 지원합니다. 기존 메서드와 요청 본문은 바뀌지 않습니다.

```java
os.networking().extensions().isEnabled("qos");
os.networking().qosRules().createDscpMarkingRule(policyId, QosRuleOptions.dscpMarking(26));
SubnetPool pool = os.networking().subnetPools().create(SubnetPoolOptions.create("pool", List.of("10.0.0.0/16")).defaultPrefixlen(24));
os.networking().addressGroups().addAddresses(groupId, List.of("10.0.2.100/32"));
os.networking().rbacPolicies().create(RbacPolicyOptions.create("network", networkId, "access_as_shared", projectId));
os.networking().router().addExtraRoutes(routerId, routes);
os.networking().port().listBindings(portId);
```

- 새 accessor: `extensions()`, `serviceProviders()`, `autoAllocatedTopology()`, `qosRules()`, `subnetPools()`, `addressScopes()`, `addressGroups()`, `rbacPolicies()`, `defaultSecurityGroupRules()`, `securityGroupDefaultStatefulness()`, `segments()`, `networkSegmentRanges()`, `localIps()`, `ndpProxies()`, `serviceFlavors()`, `serviceProfiles()`, `metering()`, `logging()`
- 기존 서비스 보강: router(extra routes, external gateways, conntrack helpers, l3 agents), agent(스케줄링), port(bindings), quotas(default, details), floating IP pools, port forwarding update
- 옵션 클래스(`*Options`)는 설정한 필드만 보냅니다. 값을 지우려면 `attribute("field", null)`.
- 해당 extension 이 꺼져 있거나 상위 자원이 없을 때(404): 모델이나 목록을 돌려주는 메서드(목록, 생성, 수정, prefix·주소 추가처럼 결과를 돌려주는 동작)는 404 를 예외로 던집니다. `ActionResponse` 를 돌려주는 메서드(삭제, import·stage·캐시 동작, agent 스케줄링, flavor 의 profile 연결)는 실패한 `ActionResponse`(코드 404)를 돌려주고, 단건 `get(id)` 은 없는 자원에 `null` 을 돌려줍니다. `extensions().isEnabled(alias)` 로 먼저 확인할 수 있습니다.
- VPNaaS, FWaaS v2, BGP, BGPVPN, TaaS, SFC(stadium 프로젝트)는 다음 릴리스에서 다룹니다.
- 4.5.0 에서 `NetQosPolicy.getId()` 를 추가했습니다(이전에는 구현 클래스로 캐스트해야 했습니다).

## Image 확장

4.6.0 부터 Glance v2 의 나머지 API 를 지원합니다. 기존 메서드와 요청 본문은 바뀌지 않습니다.

```java
ImageVersions versions = os.imagesV2().versions();          // GET /versions (Glance root)
if (versions.supports("2.6")) {
    os.imagesV2().stage(imageId, Payloads.create(new File("cirros.img")));
    os.imagesV2().importImage(imageId, ImageImportOptions.glanceDirect());
}
os.imagesV2().importImage(imageId, ImageImportOptions.webDownload("https://example.com/cirros.img"));
os.imagesV2().info().importMethods();                       // [glance-direct, web-download, ...]
os.imagesV2().metadefs().createNamespace(MetadefNamespaceOptions.create("OS::Example").visibility("private"));
os.imagesV2().schemas().image();                             // JSON schema as a Map
```

- 새 하위 서비스: `info()`(import methods, stores, usage), `cache()`(API 2.14), `schemas()`, `metadefs()`(namespaces, resource types, objects, properties, tags)
- `ImageService` 추가: `versions()`, `importImage`, `stage`, `listLocations`/`addLocation`(2.17), `listTasks`(2.12), `deleteFromStore`(2.10)
- Glance 는 microversion 헤더가 없습니다. 새 API 가 없는(오래된 서버, 캐시 middleware 꺼짐) Glance 는 404 를 줍니다: 모델이나 목록을 돌려주는 메서드(목록, 생성, 수정, prefix·주소 추가처럼 결과를 돌려주는 동작)는 404 를 예외로 던집니다. `ActionResponse` 를 돌려주는 메서드(삭제, import·stage·캐시 동작, agent 스케줄링, flavor 의 profile 연결)는 실패한 `ActionResponse`(코드 404)를 돌려주고, 단건 `get(id)` 은 없는 자원에 `null` 을 돌려줍니다. `versions().supports("2.x")` 로 먼저 확인할 수 있습니다.

## Octavia 확장

같은 릴리스에서 Octavia(load balancer v2)의 나머지 API 도 지원합니다.

```java
os.octavia().l7Policies().create(L7PolicyOptions.create(listenerId, "REDIRECT_TO_URL").redirectUrl("https://example.com"));
os.octavia().l7Policies().createRule(policyId, L7RuleOptions.create("PATH", "STARTS_WITH", "/api"));
os.octavia().flavors().create(OctaviaFlavorOptions.create("single", flavorProfileId));
os.octavia().quotas().update(projectId, OctaviaQuotaOptions.create().loadbalancer(20));
os.octavia().lbPoolV2().updateMembers(poolId, members, false);    // batch replace
os.octavia().loadBalancerV2().failover(lbId);
```

- 새 accessor: `l7Policies()`(rules 포함), `flavors()`, `flavorProfiles()`, `availabilityZones()`, `availabilityZoneProfiles()`, `providers()`, `quotas()`, `amphorae()`(관리자)
- 기존 서비스 보강: `listenerV2().stats`, `loadBalancerV2().failover`, `lbPoolV2().updateMembers`
- 404 규칙은 Image·Networking 확장과 같습니다. quota 의 `null` 은 기본값 적용, `-1` 은 무제한입니다.

## Heat 확장

같은 릴리스에서 Heat(orchestration)의 나머지 API 도 지원합니다.

```java
os.heat().stacks().preview(stackCreate);                          // create 미리 보기
os.heat().stacks().outputs(name, id); os.heat().stacks().output(name, id, "value");
os.heat().stacks().suspend(name, id); os.heat().stacks().resume(name, id);
os.heat().stacks().snapshot(name, id, "before-upgrade");
os.heat().resourceTypes().schema("OS::Nova::Server");
os.heat().softwareDeployments().create(SoftwareDeploymentOptions.create(serverId, configId));
```

- 새 accessor: `info()`(build info, services), `templateVersions()`, `resourceTypes()`, `softwareDeployments()`
- `stacks()` 보강: delete(name), environment, export, files, outputs, PATCH update, actions(suspend/resume/check/cancel), snapshots, preview; `events().list(stackName)`, `softwareConfig().list()`

## Bare metal (Ironic)

같은 릴리스에서 Ironic(bare metal v1)의 핵심 API 를 지원합니다. 대부분의 필드와 API 는 높은 microversion 이 필요하므로 세션마다 `negotiate()` 를 먼저 부르기를 권합니다(기본값은 꺼짐 — 헤더가 없으면 서버는 1.1 로 처리합니다).

```java
os.baremetal().microVersions().negotiate();                       // 서버 최대(1.107 까지)로
Node node = os.baremetal().nodes().create(NodeCreate.create("ipmi").name("bm-1").driverInfo(Map.of("ipmi_address", "192.0.2.1")));
os.baremetal().nodes().update("bm-1", List.of(BaremetalPatch.replace("/description", "rack 3")));   // JSON Patch
os.baremetal().nodes().setProvisionState("bm-1", NodeProvision.target("manage"));
os.baremetal().nodes().setPowerState("bm-1", "power on");
os.baremetal().ports().create(PortCreate.create(node.getUuid(), "52:54:00:12:34:56"));
```

- `nodes()`: 목록(detail, 필터)·get·create·update(JSON Patch)·delete, 상태(states, power, provision, RAID, boot mode, secure boot), console, boot device, NMI, validate, maintenance, traits, VIF
- `ports()`, `portgroups()`(node·portgroup 별 목록 포함), `chassis()`, `drivers()`(properties, RAID logical disk properties)
- `allocations()`, `deployTemplates()`, `runbooks()`, `inspectionRules()`, `volumeConnectors()`/`volumeTargets()`, `conductors()`(shards 포함); node 의 BIOS·firmware·history·inventory·children·virtual media·indicators·vendor passthru
- 이름으로 node 를 가리키려면(`nodes().get("bm-1")`) microversion 1.5 이상이 필요합니다 — `negotiate()` 를 먼저 부르세요
- 모델은 자주 쓰는 필드만 getter 가 있고, 나머지(microversion 마다 늘어나는 필드)는 `getAttributes()` 에 있습니다

## DNS(Designate) 확장

같은 릴리스에서 Designate v2 의 나머지 API 를 지원합니다.

```java
os.dns().zones().list(Map.of("name", "example.org."));
ZoneExport export = os.dns().zoneFiles().export(zoneId);           // 완료 후 getExportContent(id) 가 zone file
os.dns().zoneFiles().importZone(zoneFileText);                      // text/dns
ZoneTransferRequest offer = os.dns().zoneTransfers().createRequest(zoneId, targetProjectId, null);
os.dns().reverseFloatingIps().set("RegionOne", floatingIpId, "smtp.example.com.", null, 600);
```

- zones: 필터 목록, abandon, `transferFromMaster`(xfr), `movePool`; recordsets: 필터 목록(전체·zone 별)
- 새 accessor: `zoneFiles()`(export/import), `zoneShares()`, `zoneTransfers()`(requests/accepts), `tlds()`, `tsigKeys()`, `blacklists()`, `pools()`, `quotas()`, `serviceStatuses()`, `info()`(limits), `reverseFloatingIps()`
- 모델은 자주 쓰는 필드만 getter 가 있고, 나머지는 `getAttributes()` 에 있습니다

## 빌드

```bash
./mvnw verify
```

## 버그 신고와 기여

[GitHub Issues](https://github.com/seogineer/openstack4j/issues) 에 남겨 주세요. 기여 방법은 [CONTRIBUTING.md](CONTRIBUTING.md) 를 보세요.

## 라이선스

Apache License 2.0. 원작자 Jeremy Unruh 와 [원본 openstack4j 기여자들](https://github.com/openstack4j/openstack4j/graphs/contributors)의 작업에 기반합니다([NOTICE](NOTICE)).

Quick Usage Guide
-----------------

Below are some examples of the API usage.  Please visit [openstack4j.github.io](https://openstack4j.github.io/) for the full manual and getting started guides.


### Authenticating

OpenStack4j 3.0.0+ supports Identity (Keystone) V3 and V2.

OpenStack4j 3.0.0 introduced some breaking changes.
The legacy Identity V2 API now uses the class ```OSClientV2``` in place of the class OSClient.

##### Using Identity V2 authentication:
```java
// Identity V2 Authentication Example
OSClientV2 os = OSFactory.builderV2()
        .endpoint("http://127.0.0.1:5000/v2.0")
        .credentials("admin","sample")
        .tenantName("admin")
        .authenticate();
```

##### Using Identity V3 authentication

Creating and authenticating against OpenStack is extremely simple. Below is an example of authenticating which will
result with the authorized OSClient.  OSClient allows you to invoke Compute, Identity, Neutron operations fluently.

You can use either pass the users name or id and password in the following way
```java
.credentials("username", "secret", Identifier.byId("domain id"))
```
or
```java
.credentials("user id", "secret")
```
to provide credentials in each of the following cases.


Using Identity V3 authentication you basically have 4 options:

(1) authenticate with project-scope
```java
OSClientV3 os = OSFactory.builderV3()
        .endpoint("http://<fqdn>:5000/v3")
        .credentials("admin", "secret", Identifier.byId("user domain id"))
        .scopeToProject(Identifier.byId("project id"))
        .authenticate();
```
(2) authenticate with domain-scope
```java
OSClientV3 os = OSFactory.builderV3()
        .endpoint("http://<fqdn>:5000/v3")
        .credentials("admin", "secret", Identifier.byId("user domain id"))
        .scopeToDomain(Identifier.byId("domain id"))
        .authenticate();
```

(3) authenticate unscoped
```java
OSClientV3 os = OSFactory.builderV3()
        .endpoint("http://<fqdn>:5000/v3")
        .credentials("user id", "secret")
        .authenticate();
```

(4) authenticate with a token
```java
OSClientV3 os = OSFactory.builderV3()
        .endpoint("http://<fqdn>:5000/v3")
        .token("token id")
        .scopeToProject(Identifier.byId("project id"))
        .authenticate();
```
(5) authenticate using client certificate
```bash
openssl pkcs12 -export -out client-certificate-keystore.p12  -inkey key.pem -in cert.pem -certfile ca.pem
Enter Export Password:encrypt
Verifying - Enter Export Password:encrypt
```
```java
String encrypt =  "encrypt";
KeyStore keyStore = KeyStore.getInstance("PKCS12");
keyStore.load(new FileInputStream(new File("client-certificate-keystore.p12")), encrypt.toCharArray());
SSLContext sslContext = SSLContexts.custom()
        //ignore server verify
        .loadTrustMaterial(new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                return true;
            }
        })
        .loadKeyMaterial(keyStore,encrypt.toCharArray())
        .build();
Config config = Config.newConfig();
config.withSSLContext(sslContext);
OSClient.OSClientV3 osClient = OSFactory.builderV3()
        .endpoint("https://<fqdn>:5000/v3")
        .withConfig(config)
        .scopeToProject(Identifier.byId("project id"))
        //.scopeToDomain(Identifier.byId("domain id"))
        .authenticate();
```

#### Identity Operations (Keystone) V3

After successful v3 - authentication you can invoke any Identity (Keystone) V3 directly from the OSClientV3.

Identity Services fully cover User, Role, Project, Domain, Group,.. service operations (in progess).
The examples below are only a small fraction of the existing API so please refer to the API documentation for more details.

**NOTE**: The ```os``` used here is an instance of ```org.openstack4j.api.OSClient.OSClientV3```.

**User operations**
```java
// Create a User associated to the new Project
User user = os.identity().users().create(Builders.user()
	      .domainId("domain id")
	      .name("foobar")
	      .password("secret")
	      .email("foobar@example.com")
	      .enabled(true)
	      .build());
//or
User user = os.identity().users().create("domain id", "foobar", "secret", "foobar@example.org", true);

// Get detailed info on a user by id
User user = os.identity().users.get("user id");
//or by name and domain identifier
User user = os.identity().users.getByName("username", "domain id");

// Add a project based role to the user
os.identity().roles().grantProjectUserRole("project id","user id", "role id");

// Add a domain based role to the user
os.identity().roles().grantDomainUserRole("domain id","user id", "role id");

// Add a user to a group
os.identity().users().addUserToGroup("user id", "group id");
```

**Role operations**
```java
// Get a list of all roles
os.identity().roles().list();

// Get a role by name
os.identity().roles().getByName("role name");
```

**Project operations**

```java
// Create a project
os.identity().project().create(
    Builders.project().name("...").description("...").domainId("...").enabled(true).build()
);
```

#### Identity Operations (Keystone) V2

After successful v2 - authentication you can invoke any Identity (Keystone) V2 directly from the OSClientV2.

Identity V2 Services fully cover Tenants, Users, Roles, Services, Endpoints and Identity Extension listings.  The examples below are only a small fraction of the existing API so please refer to the API documentation for more details.

**NOTE**: The ```os``` used here is an instance of ```org.openstack4j.api.OSClient.OSClientV2```.

**Create a Tenant, User and associate a Role**
```java
// Create a Tenant (could also be created fluent within user create)
Tenant tenant = os.identity().tenants().create(
    Builders.identityV2().tenant().name("MyNewTenant").build()
);

// Create a User associated to the new Tenant
User user = os.identity().users().create(
    Builders.identityV2().user().name("jack").password("sample").tenant(tenant).build()
);

// Add a Tenant based Role to the User
os.identity().roles().addUserRole(
    tenant.getId(),
    user.getId(),
    os.identity().roles().getByName("Member").getId()
);

```

### Compute Operations (Nova)

OpenStack4j covers most the major common compute based operations.  With the simplistic API approach you can fully manage Servers, Flavors, Images, Quota-Sets, Diagnostics, Tenant Usage and more.  As the API evolves additional providers and extensions will be covered and documented within the API.

**Create a Flavor and Boot a Server/VM**
```java
// Create a Flavor for a special customer base
Flavor flavor = os.compute().flavors().create(
    Builders.flavor().name("Gold").vcpus(4).disk(80).ram(2048).build()
);

// Create and Boot a new Server (minimal builder options shown in example)
Server server = os.compute().servers().boot(
    Builders.server().name("Ubuntu 2").flavor(flavor.getId()).image("imageId").build()
);
```

**Create a new Server Snapshot**
```java
String imageId = os.compute().servers().createSnapshot(server.getId(), "Clean State Snapshot");
```

**Server Diagnostics**

Diagnostics are usage information about the server.  Usage includes CPU, Memory and IO.  Information is
dependant on the hypervisor used by the OpenStack installation.  As of right now there is no concrete diagnostic
specification which is why the information is variable and in map form (key and value)
```java
Map<String, ? extends Number> diagnostics = os.compute().servers().diagnostics("serverId");
```


### Networks (Neutron)

**Network Operations**
```java
// List the networks which the current authorized tenant has access to
List<? extends Network> networks = os.networking().network().list();

// Create a Network
Network network = os.networking().network().create(
    Builders.network().name("MyNewNet").tenantId(tenant.getId()).build()
);
```

**Subnet Operations**
```java
// List all subnets which the current authorized tenant has access to
List<? extends Subnet> subnets = os.networking().subnet().list();

// Create a Subnet
Subnet subnet = os.networking().subnet().create(Builders.subnet()
        .name("MySubnet")
        .networkId("networkId")
        .tenantId("tenantId")
        .addPool("192.168.0.1", "192.168.0.254")
        .ipVersion(IPVersionType.V4)
        .cidr("192.168.0.0/24")
        .build()
);
```

**Router Operations**
```java
// List all Routers
List<? extends Router> = os.networking().router().list();

// Create a Router
Router router = os.networking().router().create(
    Builders.router().name("ext_net").adminStateUp(true).externalGateway("networkId").build()
);
```

### Image Operations (Glance)

**Basic Operations**
```java
// List all Images
List<? extends Image> images = os.images().list();

// Get an Image by ID
Image image = os.images().get("imageId");

// Delete a Image
os.images().delete("imageId");

// Update a Image
Image image = os.images().get("imageId");

os.images().update(
   image.toBuilder().name("VM Image Name").minDisk(1024).property("personal-distro", "true")
);
```

**Download the Image Data**
```java
InputStream is = os.images().getAsStream("imageId");
```

**Create a Image**
```java
// (URL Payload in this example, File, InputStream are other payloads available)
Payload<URL> create = Payloads.create(new URL("https://launchpad.net/cirros/trunk/0.3.0/+download/cirros-0.3.0-x86_64-disk.img"))
Image req = Builders.image().name("Cirros 0.3.0 x64").isPublic(true).containerFormat(ContainerFormat.BARE).diskFormat(DiskFormat.QCOW2).build()
Image image = c.images().create(req, create)
```

License
-------
```
This software is licensed under the Apache 2 license, quoted below.

Copyright 2019 ContainX and OpenStack4j

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
```
