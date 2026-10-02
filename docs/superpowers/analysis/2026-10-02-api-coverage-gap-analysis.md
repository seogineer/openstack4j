# OpenStack 서비스별 API 지원 현황 분석 (gap analysis)

- 작성일: 2026-10-02
- 기준 릴리스: **OpenStack 2026.2 Hibiscus** (2026-10 기준 최신 Maintained 릴리스)
- 기준 코드: openstack4j 포크 `main` (4.1.0 릴리스 직후, `4.2.0-SNAPSHOT`)
- 목적: 최종 목표(최신 릴리스의 모든 서비스 API 지원)를 향해 다음 하위 프로젝트(C)를 고르기 위한 현황 파악

## 1. 방법과 한계

1. Hibiscus 릴리스 페이지의 **Service Projects** 목록(34개)을 기준으로 삼았다.
2. 각 서비스 저장소의 `api-ref/source/*.inc|rst` 에서 `.. rest_method::` 선언을 모두 추출해 **공식 API 엔드포인트(메서드 + 경로)** 목록을 만들었다. api-ref 가 없는 Barbican 은 `doc/source/api/reference`, Aodh 는 API 컨트롤러 소스에서 추출했다.
3. openstack4j 구현(`openstack/<service>/**/*.java`)의 요청 경로를 추출했다(문자열 상수와 연결식을 해석). 경로의 버전 접두사(`/v2.1` 등)와 파라미터(`{id}`, `%s`)를 정규화한 뒤 **경로 단위**로 대조했다.
4. 한계: 경로가 같아도 HTTP 메서드나 microversion 별 필드 차이는 보지 않으므로 **실제 커버리지는 표의 숫자보다 낮다.** 반대로 상수 조합을 완전히 해석하지 못해 몇 개는 누락으로 잘못 잡힐 수 있다(±5% 수준). api-ref 에 "DEPRECATED" 로 표시된 엔드포인트는 제외했다.
5. 추출 스크립트와 중간 산출물은 일회용이며 저장소에 넣지 않았다.

## 2. Hibiscus 서비스 34개와 openstack4j 의 관계

| 구분 | 서비스 |
|---|---|
| openstack4j 가 **일부 지원** (17) | Nova, Neutron, Cinder, Keystone, Glance, Swift, Heat, Octavia, Magnum, Manila, Designate, Placement(4.1.0 에서 완성), Mistral, Trove, Tacker, Barbican, Aodh |
| openstack4j 에 **전혀 없음** (12) | Ironic, Zun, Cyborg, Blazar, Masakari, Watcher, Zaqar, Adjutant, CloudKitty, Freezer(-api), Skyline(apiserver/console), Storlets |
| REST API 가 아니거나 라이브러리 대상이 아님 (5) | Horizon(UI), Skyline-console(UI), Aetos(Prometheus 프록시, 2026.2 신규), Ceilometer(API 없음, 수집기만), Storlets(Swift 미들웨어) |

openstack4j 가 지원하지만 **Hibiscus 에 없는(은퇴한) 서비스**: Sahara, Senlin, Murano, Glare(artifact), Ceilometer meters/samples API(telemetry 패키지의 Aodh 외 부분), GBP(Group-Based Policy, 공식 프로젝트 아님). 이들은 유지만 하고 확장하지 않는다. Tacker 는 레거시 v1.0 API 만 있고 현재 표준인 ETSI NFV-SOL v2 API 는 없다.

## 3. 커버리지 표 (경로 단위, deprecated 제외)

| 서비스 | api-ref 메서드 | 경로 | 구현 경로 | 누락 | 커버리지 |
|---|---|---|---|---|---|
| Neutron (network) | 345 | 172 | 46 | 126 | 26% |
| Keystone (identity) | 237 | 120 | 30 | 90 | 25% |
| Manila (shared FS) | 173 | 112 | 26 | 86 | 23% |
| Ironic (bare metal) | 116 | 76 | 0 | 76 | 0% |
| Cinder (block storage) | 140 | 94 | 21 | 73 | 22% |
| Tacker (NFV) | 83 | 57 | 2 | 55 | 3% |
| Nova (compute) | 154 | 100 | 46 | 54 (410 응답 API 제외 시 31) | 46% (410 제외 시 약 59%) |
| Glance (image) | 67 | 49 | 10 | 39 | 20% |
| Zun (containers) | 46 | 37 | 0 | 37 | 0% |
| Trove (database) | 76 | 47 | 12 | 35 | 25% |
| Designate (DNS) | 63 | 37 | 6 | 31 | 16% |
| Watcher (optimization) | 38 | 29 | 0 | 29 | 0% |
| Octavia (load balancer) | 75 | 38 | 12 | 26 | 31% |
| Heat (orchestration) | 53 | 39 | 16 | 23 | 41% |
| Barbican (key manager) | 41 | 20 | 1 | 19 | 5% |
| Zaqar (messaging) | 36 | 19 | 0 | 19 | 0% |
| Adjutant (ops tasks) | 28 | 17 | 0 | 17 | 0% |
| Mistral (workflow) | 63 | 30 | 16 | 14 | 53% |
| Cyborg (accelerators) | 21 | 13 | 0 | 13 | 0% |
| Blazar (reservation) | 18 | 10 | 0 | 10 | 0% |
| Masakari (instance HA) | 17 | 10 | 0 | 10 | 0% |
| CloudKitty (rating) | 13 | 8 | 0 | 8 | 0% |
| Magnum (container infra) | 23 | 13 | 6 | 7 | 46% |
| Aodh (alarming) | 14 | 8 | 3 | 5 | 37% |
| Swift (object storage) | 17 | 5 | 3 | 2 | 60% |
| Placement | 35 | 18 | 18 | 0 | 100% |

Nova 의 누락 54개 중 21개는 Nova 가 이미 **410 Gone** 으로 응답하는 제거된 API(`os-cells`, `os-certificates`, `os-cloudpipe`, `os-fixed-ips`, `os-floating-ips-bulk`, `os-agents`, `os-fping`, `os-security-group-default-rules`, `os-virtual-interfaces`)라서 구현 대상이 아니다.

## 4. 서비스별 누락 내용

### Nova — microversion 미지원이 핵심
openstack4j 의 compute 는 **microversion 헤더를 전혀 보내지 않아** 2.1 기본 동작에 머물러 있다. 2.1 이후 추가된 응답 필드(예: 2.3 의 `OS-EXT-*` 확장, 2.26 tags, 2.47 flavor 내장, 2.63 trusted certs, 2.73 locked reason, 2.79/2.89 attachments, 2.96 pinned AZ 등)를 받을 수 없고, 아래 엔드포인트는 호출 자체가 불가능하다.

누락(구현 대상): `/flavors/detail`, `/os-aggregates/*/images`, `/os-assisted-volume-snapshots`, `/os-assisted-volume-snapshots/*`, `/os-availability-zone`, `/os-availability-zone/detail`, `/os-console-auth-tokens/*`, `/os-hypervisors`, `/os-hypervisors/*/search`, `/os-hypervisors/*/servers`, `/os-hypervisors/*/uptime`, `/os-instance_usage_audit_log`, `/os-instance_usage_audit_log/*`, `/os-quota-sets/*/defaults`, `/os-server-external-events`, `/os-services/*`, `/os-services/disable-log-reason`, `/servers/*/consoles`, `/servers/*/consoles/*`, `/servers/*/ips`, `/servers/*/ips/*`, `/servers/*/migrations`, `/servers/*/migrations/*`, `/servers/*/migrations/*/action`, `/servers/*/os-virtual-interfaces`, `/servers/*/remote-consoles`, `/servers/*/shares`, `/servers/*/shares/*`, `/servers/*/tags`, `/servers/*/tags/*`, `/servers/*/topology`

### Cinder — v3 microversion 과 그 이후 기능 전체
compute 와 같이 microversion 을 보내지 않는다(v3.0 동작). 누락: `/attachments`, `/attachments/*`, `/attachments/*/action`, `/attachments/detail`, `/backups/*/action`, `/backups/*/export_record`, `/backups/import_record`, `/capabilities/*`, `/clusters`, `/clusters/*`, `/clusters/detail`, `/clusters/disable`, `/clusters/enable`, `/extensions`, `/group_snapshots`, `/group_snapshots/*`, `/group_snapshots/detail`, `/group_types`, `/group_types/*`, `/group_types/*/group_specs`, `/group_types/default`, `/groups`, `/groups/*`, `/groups/*/action`, `/groups/action`, `/groups/detail`, `/manageable_snapshots`, `/manageable_snapshots/detail`, `/manageable_volumes`, `/manageable_volumes/detail`, `/messages`, `/messages/*`, `/os-availability-zone`, `/os-hosts`, `/os-hosts/*`, `/os-services/disable`, `/os-services/disable-log-reason`, `/os-services/enable`, `/os-services/failover_host`, `/os-services/freeze`, `/os-services/get-log`, `/os-services/set-log`, `/os-services/thaw`, `/os-volume-manage`, `/os-volume-transfer/detail`, `/qos-specs`, `/qos-specs/*`, `/qos-specs/*/associate`, `/qos-specs/*/associations`, `/qos-specs/*/delete_keys`, `/qos-specs/*/disassociate`, `/qos-specs/*/disassociate_all`, `/resource_filters`, `/snapshot/*/metadata/*`, `/snapshots/*/action`, `/snapshots/*/metadata`, `/snapshots/*/metadata/*`, `/snapshots/detail`, `/types/*/action`, `/types/*/extra_specs`, `/types/*/extra_specs/*`, `/types/default`, `/volume-transfers`, `/volume-transfers/*`, `/volume-transfers/*/accept`, `/volume-transfers/detail`, `/volumes/*/metadata`, `/volumes/*/metadata/*`, `/volumes/summary`, `/default-types`, `/default-types/*`, `v3/*/workers/cleanup`

### Neutron — 확장 API 대부분
누락 그룹: QoS(14), VPNaaS(10), BGP(8)·BGPVPN(8), FWaaS v2(8), 라우터 부가 기능(8: conntrack helpers, extra routes 등), SFC(6), TaaS(6), subnet pools(5), address groups(4), agents 스케줄링(4), service flavors(4), local IPs(4), metering(4), logging(3), address scopes, auto-allocated topology, default security group rules, floating IP pools, NDP proxies, port forwarding, segments, trunk 등.

### Keystone — 확장과 인증 방식
누락: OS-FEDERATION(12), 사용자 부가 API(10: application credentials, access rules, EC2 credentials, password change, 그룹·프로젝트 조회), OS-EP-FILTER(9), 인증(8: federation/SAML/ECP/websso, catalog/domains/projects/system 조회), 도메인 설정(8), OS-INHERIT(6), OS-OAUTH1(5), policies(5), OS-TRUST(4), system scope 역할(4), unified limits(3+2), OS-REVOKE, OS-SIMPLE-CERT, 역할 implication 등.

### Glance — v2 부가 기능
누락: `/cache`, `/cache/*`, `/cache/clean`, `/cache/prune`, `/images/*/import`, `/images/*/locations`, `/images/*/stage`, `/images/*/tasks`, `/info/import`, `/info/stores`, `/info/stores/detail`, `/info/usage`, `/metadefs/namespaces`, `/metadefs/namespaces/*`, `/metadefs/namespaces/*/objects`, `/metadefs/namespaces/*/objects/*`, `/metadefs/namespaces/*/properties`, `/metadefs/namespaces/*/resource_types`, `/metadefs/namespaces/*/tags`, `/metadefs/resource_types`, `/schemas/image`, `/schemas/images`, `/schemas/member`, `/schemas/members`, `/schemas/metadefs/namespace`, `/schemas/metadefs/namespaces`, `/schemas/metadefs/object`, `/schemas/metadefs/objects`, `/schemas/metadefs/properties`, `/schemas/metadefs/property`, `/schemas/metadefs/resource_type`, `/schemas/metadefs/resource_types`, `/schemas/metadefs/tag`, `/schemas/metadefs/tags`, `/schemas/task`, `/schemas/tasks`, `/stores/*/*`, `/versions`

### Manila — microversion 과 신규 기능 (총 86)
누락 그룹: share replicas(8), shares 부가(8), share group types(7), share group snapshots(5), share networks 부가(5), QoS types(4), services(4), access rules(4), share backups(4), share groups(4), share transfers(4), snapshot instances(4), snapshots 부가(4), quota sets(3), share servers(3), messages(2), resource locks(2), scheduler stats(2) 등.

### Octavia / Heat / Designate
- Octavia 누락: `/lbaas/availabilityzoneprofiles`, `/lbaas/availabilityzoneprofiles/*`, `/lbaas/availabilityzones`, `/lbaas/availabilityzones/*`, `/lbaas/flavorprofiles`, `/lbaas/flavorprofiles/*`, `/lbaas/flavors`, `/lbaas/flavors/*`, `/lbaas/l7policies`, `/lbaas/l7policies/*`, `/lbaas/l7policies/*/rules`, `/lbaas/l7policies/*/rules/*`, `/lbaas/listeners/*/stats`, `/lbaas/loadbalancers/*/failover`, `/lbaas/providers`, `/lbaas/providers/*/availability_zone_capabilities`, `/lbaas/providers/*/flavor_capabilities`, `/lbaas/quotas`, `/lbaas/quotas/*`, `/lbaas/quotas/defaults`, `/octavia/amphorae`, `/octavia/amphorae/*`, `/octavia/amphorae/*/config`, `/octavia/amphorae/*/failover`, `/octavia/amphorae/*/stats`
- Heat 누락: `/build_info`, `/resource_types`, `/resource_types/*`, `/resource_types/*/template`, `/services`, `/software_deployments`, `/software_deployments/*`, `/software_deployments/metadata/*`, `/stacks/*/*/actions`, `/stacks/*/*/environment`, `/stacks/*/*/export`, `/stacks/*/*/files`, `/stacks/*/*/outputs`, `/stacks/*/*/outputs/*`, `/stacks/*/*/preview`, `/stacks/*/*/snapshots`, `/stacks/*/*/snapshots/*`, `/stacks/*/*/snapshots/*/restore`, `/stacks/*/events`, `/stacks/preview`, `/template_versions`, `/template_versions/*/functions`
- Designate 누락: `/blacklists`, `/blacklists/*`, `/limits`, `/pools`, `/pools/*`, `/quotas`, `/quotas/*`, `/reverse/floatingips`, `/reverse/floatingips/*:*`, `/service_statuses`, `/service_statuses/*`, `/tlds`, `/tlds/*`, `/tsigkeys`, `/tsigkeys/*`, `/zones/*/shares`, `/zones/*/shares/*`, `/zones/*/tasks/abandon`, `/zones/*/tasks/export`, `/zones/*/tasks/pool_move`, `/zones/*/tasks/transfer_requests`, `/zones/*/tasks/xfr`, `/zones/tasks/exports`, `/zones/tasks/exports/*`, `/zones/tasks/exports/*/export`, `/zones/tasks/imports`, `/zones/tasks/imports/*`, `/zones/tasks/transfer_accepts`, `/zones/tasks/transfer_requests`, `/zones/tasks/transfer_requests/*`

### 나머지
- Trove(35), Tacker(55, v2 전체), Barbican(19), Magnum(7), Aodh(5), Swift(2), Mistral(14)은 누락 목록이 짧거나 서비스 자체가 틈새라 뒤로 미룬다.
- 전혀 없는 12개 서비스 중 **Ironic(76)** 이 가장 크고 널리 쓰이며, Masakari·Watcher·Zun·Cyborg·Blazar 는 특정 환경에서만 쓰인다.

## 5. 우선순위 기준과 제안

기준: ① 사용자 범위(핵심 서비스일수록 먼저), ② 누락이 가져오는 실질 영향(microversion 미지원은 기존 기능까지 반쪽으로 만든다), ③ 재사용(B 에서 만든 microversion 협상·기능별 버전 검사·오류 매핑 틀을 그대로 쓸 수 있는가), ④ 검증 가능성(개발용 OpenStack epoxy 에서 실환경 테스트가 가능한가).

| 순서 | 하위 프로젝트 | 이유 |
|---|---|---|
| **C** | **Nova microversion 프레임워크 + 누락 API** | 가장 많이 쓰이는 서비스인데 microversion 이 전혀 없다. B 의 틀(협상, 기능별 최소 버전, 응답 모델 nullable 필드)을 그대로 적용할 수 있고, 2.1→2.100 범위의 필드·엔드포인트를 한 번에 정리한다. epoxy 에서 바로 검증 가능. |
| D | Cinder v3 microversion + 누락 API | Nova 와 같은 문제. attachments, groups, volume transfers v3.55, messages, clusters 등 운영에 자주 쓰이는 API 가 빠져 있다. |
| E | Keystone 확장 | application credentials, trusts, federation, unified limits 등. microversion 은 없고 확장 경로만 추가하면 된다. |
| F | Neutron 확장 | QoS, trunk, port forwarding, segments, address scopes/groups, FWaaS/VPNaaS/BGP. 규모가 가장 크므로 확장 단위로 더 쪼갠다. |
| G | Manila microversion + 누락 API | Nova/Cinder 와 같은 틀. |
| H | Glance 부가 API, Octavia 나머지, Heat 나머지, Designate 나머지 | 각각 작아서 하나로 묶을 수 있다. |
| I | 신규 서비스: Ironic → Barbican 완성 → Masakari/Watcher/Zun/Cyborg/Blazar/Zaqar/Adjutant/CloudKitty | 수요 순. |
| — | Sahara, Senlin, Murano, Glare, Ceilometer API, GBP, Tacker v1 | 은퇴·레거시. 유지만 한다. |

**추천: C = Nova.** 설계 단계에서 정할 것은 ① 지원할 microversion 범위(제안: 최소 2.1 유지, 라이브러리 최고 2.100 — Hibiscus Nova 의 최대), ② 기존 `ComputeService` 를 건드리지 않고 새 기능을 어떻게 얹을지(B 처럼 `compute.v2` 하위 패키지로 분리할지, 기존 서비스에 microversion 헤더만 추가할지 — 후자는 응답 필드 추가라 하위 호환이지만 기존 모델 수정이 필요하다), ③ 410 API 의 처리(그대로 둠).

## 6. 부록: 추출 수치 요약

- api-ref 추출 총계: 24개 서비스, 2,230 메서드(deprecated 포함)
- openstack4j 추출 경로: compute 61, networking 77, identity 46, image 12, block storage 22, object storage 2, heat 17, manila 41, octavia 12, placement 18, telemetry 15, trove 14, workflow 19, magnum 14, barbican 2, tacker 6, dns 6 (+ 은퇴 서비스 artifact 3, gbp 24, murano 10, sahara 30, senlin 24)
