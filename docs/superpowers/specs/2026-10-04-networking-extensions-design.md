# openstack4j 후속 포크 — F. Neutron(networking) in-tree 확장 설계

- 날짜: 2026-10-04
- 대상 버전: 4.5.0
- 상태: 사용자 지시("나한테 물어보지 말고 계속 진행해", "네가 권장하는대로 따를게")에 따라 권장안으로 결정하고 이 문서에 기록한다.

## 1. 배경과 목적

openstack4j 의 networking 은 Neutron api-ref(neutron-lib, 2026.2 Hibiscus) 351 메서드 중 80 개만 구현한다(메서드 단위 비교, `scratchpad/gap/neutron_missing_methods.txt`). 빠진 271 개 중 117 개는 Neutron 본체와 별도로 설치하는 stadium 프로젝트(VPNaaS, FWaaS v2, BGP dynamic routing, BGPVPN, TaaS, SFC)이고, 나머지는 Neutron 본체(in-tree 서비스 플러그인 포함)의 API 다.

이 하위 프로젝트(F)는 **Neutron 본체의 빠진 API 147 개**를 추가한다. stadium 프로젝트는 F2 로 분리한다.

### 제약
- 기존 코드는 바꾸지 않고 추가만 한다(`@Deprecated` 없음). 기존 요청 본문은 그대로다.
- Neutron 은 microversion 이 없다. 기능 유무는 extension(`GET /v2.0/extensions`)으로 알 수 있다.
- 세 connector(httpclient, okhttp, http-connector)에서 같은 결과.
- 회사 코드(openstackit-java)는 복사하지 않는다.

### 참고 수치 (개발용 Neutron, OVN, epoxy)
- extension 85 개. 켜진 것: qos(bw-limit, min-bw, dscp; pps 는 OVN 미지원), address-scope, address-group, subnet_allocation, subnetpool-prefix-ops, rbac-policies, flavors, security-groups-default-rules, stateful-security-group, extraroute, external-gateway-multihoming, binding-extended, auto-allocated-topology, floatingip-pools, quota_details, floating-ip-port-forwarding-detail.
- 꺼진 것: segment, network-segment-range, local_ip, l3-ndp-proxy, metering, logging, l3-conntrack-helper, dhcp/l3 agent 스케줄러(OVN 이라 agent 없음). 이것들은 단위 테스트만 한다.

## 2. 결정 사항

| 항목 | 결정 | 이유 |
|---|---|---|
| 범위 | Neutron 본체 147 메서드. stadium(VPNaaS 25, FWaaS v2 17, BGP 19, BGPVPN 19, TaaS 15, SFC 25)은 F2 | stadium 은 별도 설치이고 개발 환경에 없다. 한 릴리스에 묶으면 검증할 수 없는 코드가 절반이 된다 |
| 제외 | `GET /`(버전 목록) | 쓰임이 없다 |
| 서비스 배치 | 새 자원은 `os.networking()` 의 새 accessor, 기존 자원의 하위 경로는 기존 서비스에 메서드 추가 | E 와 같은 방식 |
| 모델 | 인터페이스 `org.openstack4j.model.network.ext`, 구현 `org.openstack4j.openstack.networking.domain.ext.Neutron*` | 기존 ext 와 같은 위치 |
| 생성·수정 본문 | 옵션 클래스(`*Create`, `*Update`, `org.openstack4j.model.network.options`)의 `toMap()` + `JsonBody` | E 와 같은 방식. **null 값은 넣지 않는다**(E 의 deferred minor 를 F 에서는 처음부터 피한다) |
| 응답 래핑 | 단건은 `@JsonRootName`, 목록은 `ListResult`. 루트에 다른 키가 같이 오는 응답은 wrapper 클래스 | E 의 role_inference 교훈 |
| extension 확인 | `extensions().isEnabled(alias)` 제공. 메서드가 자동으로 확인하지는 않는다 | 요청마다 확인하면 호출이 두 배가 된다. 서버의 404 를 그대로 전한다 |

## 3. 새 서비스 (`os.networking()`)

| accessor | 메서드 | 엔드포인트 | 수 |
|---|---|---|---|
| `extensions()` | `list()`, `get(alias)`, `isEnabled(alias)` | `/extensions[/{alias}]` | 2 |
| `serviceProviders()` | `list()` | `/service-providers` | 1 |
| `autoAllocatedTopology()` | `get(projectId)`, `validate(projectId)`(`?fields=dry-run`), `delete(projectId)` | `/auto-allocated-topology/{project_id}` | 2 |
| `qosRules()` | `ruleTypes()`, `ruleType(type)`; dscp marking·minimum bandwidth·minimum packet rate·packet rate limit 각 `list/get/create/update/delete(policyId, …)`; alias 규칙(bandwidth limit, dscp marking, minimum bandwidth, minimum packet rate) 각 `get/update/delete(ruleId)` | `/qos/rule-types[/{type}]`, `/qos/policies/{id}/<kind>_rules[/{id}]`, `/qos/alias_<kind>_rules/{id}` | 34 |
| `subnetPools()` | CRUD, `addPrefixes(id, List)`, `removePrefixes(id, List)`, `onboardNetworkSubnets(id, networkId)` | `/subnetpools[/{id}[/add_prefixes|remove_prefixes|onboard_network_subnets]]` | 8 |
| `addressScopes()` | CRUD | `/address-scopes[/{id}]` | 5 |
| `addressGroups()` | CRUD, `addAddresses(id, List)`, `removeAddresses(id, List)` | `/address-groups[/{id}[/add_addresses|remove_addresses]]` | 7 |
| `rbacPolicies()` | CRUD | `/rbac-policies[/{id}]` | 5 |
| `defaultSecurityGroupRules()` | `list()`, `get(id)`, `create(…)`, `delete(id)` | `/default-security-group-rules[/{id}]` | 4 |
| `securityGroupDefaultStatefulness()` | CRUD | `/security-groups-default-statefulness[/{id}]` | 5 |
| `segments()` | CRUD | `/segments[/{id}]` | 5 |
| `networkSegmentRanges()` | CRUD | `/network_segment_ranges[/{id}]` | 5 |
| `localIps()` | CRUD, `portAssociations(id)`, `associatePort(id, fixedPortId, fixedIp)`, `disassociatePort(id, fixedPortId)` | `/local_ips[/{id}[/port_associations[/{port}]]]` | 8 |
| `ndpProxies()` | CRUD | `/ndp_proxies[/{id}]` | 5 |
| `serviceFlavors()` | CRUD, `associateProfile(flavorId, profileId)`, `disassociateProfile(flavorId, profileId)` | `/flavors[/{id}[/service_profiles[/{id}]]]` | 7 |
| `serviceProfiles()` | CRUD | `/service_profiles[/{id}]` | 5 |
| `metering()` | label `list/get/create/delete`, rule `list/get/create/delete` | `/metering/metering-labels[/{id}]`, `/metering/metering-label-rules[/{id}]` | 8 |
| `logging()` | CRUD, `loggableResources()` | `/log/logs[/{id}]`, `/log/loggable-resources` | 6 |

CRUD = `list()`·`list(Map<String, String> filters)`·`get(id)`·`create(…)`·`update(id, …)`·`delete(id)`(api-ref 에 update 가 없는 자원은 뺀다). 이름 `serviceFlavors()` 는 compute flavor 와 헷갈리지 않게 붙였다.

## 4. 기존 서비스에 메서드 추가

| 서비스 | 메서드 | 엔드포인트 | 수 |
|---|---|---|---|
| `router()` | `addExtraRoutes(routerId, List<HostRoute>)`, `removeExtraRoutes(…)`, `addExternalGateways(routerId, List<…>)`, `updateExternalGateways`, `removeExternalGateways`; conntrack helper `listConntrackHelpers/getConntrackHelper/createConntrackHelper/updateConntrackHelper/deleteConntrackHelper`; `listL3Agents(routerId)` | `/routers/{id}/…` | 11 |
| `agent()` | `delete(agentId)`, `listDhcpNetworks(agentId)`, `listL3Routers(agentId)`, `addRouterToL3Agent(agentId, routerId)`, `removeRouterFromL3Agent(agentId, routerId)`, `listDhcpAgentsHostingNetwork(networkId)` | `/agents/{id}[/dhcp-networks|/l3-routers[/{id}]]`, `/networks/{id}/dhcp-agents` | 6 |
| `port()` | `listBindings(portId)`, `createBinding(portId, host, …)`, `activateBinding(portId, host)`, `deleteBinding(portId, host)` | `/ports/{id}/bindings[/{host}[/activate]]` | 4 |
| `quotas()` | `getDefault(projectId)`, `getDetails(projectId)` | `/quotas/{id}/default`, `/quotas/{id}/details.json` | 2 |
| `floatingip()` | `listPools()` | `/floatingip_pools` | 1 |
| `floatingip().portForwarding()` | `update(floatingIpId, id, …)` | `PUT /floatingips/{id}/port_forwardings/{id}` | 1 |

`Router` 모델에 `default List<…> getExternalGateways()`(external-gateway-multihoming)를 추가한다. 이 필드는 응답 전용(WRITE_ONLY)이라 기존 router 생성·수정 본문은 바뀌지 않는다.

## 5. 테스트

### 5.1 단위 테스트 (core-test)
- 기존 networking 테스트는 바꾸지 않고 통과해야 한다.
- 새 테스트는 `core-test/src/main/java/org/openstack4j/api/network/ext2/` 에 둔다(기존 `api/network` 와 구분). 기반 클래스 `AbstractNetworkingExtTest`(Service.NETWORK).
- fixture 는 개발용 Neutron 의 실제 응답(`scratchpad/neutron/live/*.json`)과 api-ref 예시(`scratchpad/neutron/api-ref/source/v2/samples/`)로 만든다.
- 요청 경로는 URL-decoded 로 비교하고, 각 테스트는 모든 요청을 `takeRequest()` 한다(D·E 교훈).
- 경로·본문 형태가 connector 마다 다를 수 있는 항목(필터 query, 빈 본문 PUT)은 세 connector 에서 실행한다.

### 5.2 실환경 통합 테스트 (`NetworkingExtensionsLiveTests`, `OS_AUTH_URL` 이 있을 때만; `OS_TOKEN` 또는 password)
extension 목록·`isEnabled`, service providers, floating IP pools, quota default·details, QoS rule types 와 임시 정책의 dscp·minimum bandwidth 규칙, address scope → subnet pool(+prefix 추가·삭제), address group(+주소 추가·삭제), 임시 network 의 RBAC policy, default security group rules 조회, 임시 router 의 extra routes 추가·삭제, 기존 port 의 binding 조회, service profile·flavor 생성·연결·삭제, auto-allocated topology dry-run(기본 외부망이 없어 오류가 정상). 임시 자원은 `finally` 에서 지우고 삭제 성공을 확인한다. 개발 환경에 없는 extension 의 테스트는 `isEnabled` 가 false 면 건너뛴다.

## 6. 작업 순서

| # | 작업 |
|---|---|
| 1 | 테스트 기반, `extensions()`·`serviceProviders()`·`autoAllocatedTopology()`, quota default/details, floating IP pools, port forwarding update |
| 2 | QoS 규칙(rule types, dscp, minimum bandwidth, minimum packet rate, packet rate limit, alias) |
| 3 | subnet pools(+prefix ops, onboard), address scopes |
| 4 | address groups, RBAC policies |
| 5 | default security group rules, security group default statefulness |
| 6 | router 보강(extra routes, external gateways, conntrack helpers, l3 agents), `Router.getExternalGateways()` |
| 7 | agent 스케줄링, port bindings |
| 8 | segments, network segment ranges |
| 9 | local IPs(+port associations), NDP proxies |
| 10 | service flavors, service profiles |
| 11 | metering, logging |
| 12 | 실환경 통합 테스트, README networking 절, MIGRATION/CHANGELOG |
| 13 | 전체 리뷰 → 4.5.0 릴리스 → 4.6.0-SNAPSHOT |

## 7. 완료 기준

1. 기존 networking 테스트가 전부 변경 없이 통과한다.
2. 3·4장의 147 메서드가 구현되고 단위 테스트가 세 connector 에서 통과한다.
3. 실환경 테스트가 개발용 Neutron 에서 통과하고 임시 자원이 남지 않는다.
4. `io.github.seogineer:openstack4j:4.5.0` 이 Maven Central 에 배포된다.
