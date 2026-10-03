## 4.5.0

- Neutron 본체(in-tree) 확장 API 147 개: `extensions()`, `serviceProviders()`, `autoAllocatedTopology()`, `qosRules()`(rule types, DSCP, minimum bandwidth/packet rate, packet rate limit, alias), `subnetPools()`(prefix ops, onboard), `addressScopes()`, `addressGroups()`, `rbacPolicies()`, `defaultSecurityGroupRules()`, `securityGroupDefaultStatefulness()`, `segments()`, `networkSegmentRanges()`, `localIps()`, `ndpProxies()`, `serviceFlavors()`, `serviceProfiles()`, `metering()`, `logging()`
- 기존 서비스 보강: router(extra routes, external gateways, conntrack helpers, l3 agents), agent(dhcp/l3 스케줄링, 삭제), port(bindings), quotas(default, details), floating IP pools, port forwarding update; `Router.getExternalGateways()`, `NetQosPolicy.getId()`
- 공용 기반 `BaseNeutronExtService`(내부), `NeutronAttributes`(설정한 필드만 보내는 옵션 기반)
- 테스트: `NetworkingExtensionsLiveTests`(개발용 Neutron/OVN 에서 8 통과, extraroute-atomic 미지원으로 1 skip)

## 4.4.0

- 인증: application credential(id 또는 name+user), TOTP(`passcode`, password+TOTP MFA), `scopeToSystem()`, `scopeToTrust()`; 토큰 `getSystem()`/`getApplicationCredential()`/`getTrust()`; application credential 세션의 재인증; trust/system scope 토큰의 세션 컨텍스트 NPE 수정
- 모델 필드: user options/password_expires_at/federated, project is_domain, domain tags, role description; `UserListOptions`, `ProjectListOptions`(tags, tags-any, not-tags, not-tags-any)
- 새 서비스: `applicationCredentials()`, `trusts()`, `endpointFilter()`, `endpointPolicies()`, `limits()`/`registeredLimits()`(unified limits), `federation()`(IdP·protocol·mapping·service provider, SAML2 metadata/assertion/ECP, federated token), `oauth1()`(RFC 5849 HMAC-SHA1 서명), `oauth2()`(client credentials), `revocationEvents()`, `systemRoles()`
- 기존 서비스 보강: users access rules, project tags, `tokens().getSystemScopes()`, OS-INHERIT 14개, implied roles·role inferences, domain configuration 13개
- 테스트: `IdentityExtensionsLiveTests`(개발용 Keystone 3.14 에서 9/9, `OS_TOKEN` 또는 password)

## 4.3.0

- 공용 microversion 프레임워크: `MicroVersionSupport`(내부), 공개 `MicroVersionService<V>`/`MicroVersionInfo`. compute 는 내부만 위임(공개 API 불변)
- Cinder v3 microversion 선택적 지원: `os.blockStorage().microVersions()` 의 `negotiate()`/`use()`/`clear()`, 3.0~3.71. 기본값은 꺼짐(요청 불변)
- 레거시 호출 보호: `bootable` 을 설정한 볼륨 생성은 3.52, `force=false` 스냅샷 생성은 3.65 로 전송
- 응답 모델 필드: volume(user_id, updated_at, group_id 3.13, provider_id 3.21, shared_targets 3.48/3.69, service_uuid 3.48, cluster_name 3.61, volume_type_id 3.63, encryption_key_id 3.64, consumes_quota 3.65 등), snapshot(3.14/3.41/3.65), backup(3.18/3.43/3.56/3.64), type(description/is_public/qos_specs_id), transfer(3.55/3.57), service(3.7/3.26/3.49), quota(backups/backup_gigabytes/per_volume_gigabytes/groups)
- 목록 옵션 `VolumeListOptions`/`SnapshotListOptions`/`BackupListOptions`(3.4/3.10/3.22/3.34/3.45/3.60/3.65), `volumes().summary()`(3.12), `update(VolumeUpdateOptions)`
- 생성 옵션: volume `groupId`(3.13)/`backupId`(3.47)/`imageId`/`consistencyGroupId`/`schedulerHints`, backup `metadata`(3.43)/`availabilityZone`(3.51), upload image `visibility`/`protected`(3.1)
- volume 액션: metadata CRUD, image metadata, revert(3.40), reimage(3.68), extend completion(3.71), retype, migrate(host/cluster 3.16), migration completion, unmanage, reserve/unreserve, begin/roll detaching, initialize/terminate connection, reset status
- snapshots: metadata CRUD, reset/force-delete/update status, unmanage; backups: update(3.9, metadata 3.43), export/import record, force-delete, reset status
- 새 서비스: `attachments()`(3.27; complete 3.44, mode 3.54), `messages()`(3.3), `volumeTypes()`(extra specs, access, encryption, default), `defaultTypes()`(3.62), `qosSpecs()`, `groups()`(3.13; replication 3.38), `groupTypes()`(3.11), `groupSnapshots()`(3.14), `clusters()`(3.7), `workers()`(3.24), `hosts()`, `capabilities()`, `resourceFilters()`(3.33), `extensions()`, `volumeTransfers()`(3.55), `manageableVolumes()`/`manageableSnapshots()`(3.8)
- `services()` 액션(enable/disable/disable-log-reason/freeze/thaw/failover_host/failover 3.26/get-log·set-log 3.32), `getLimits(projectId)`(3.39), quota classes, pools 옵션(3.28/3.35), `Builders.volumeTypeEncryption()`
- 테스트: `AbstractTest.adjustTokenJson` hook, `BlockStorageLiveTests`(개발용 Cinder 3.71 에서 세 connector 통과)

## 4.2.0

- Nova microversion 선택적 지원: `os.compute().microVersions()` 의 `negotiate()`/`use()`/`clear()`, 2.1~2.104. 기본값은 꺼짐(요청 불변)
- Nova 가 제거한 API 를 쓰는 기존 메서드에 상한 적용(프록시 API 2.35, os-hosts 2.42, VNC 콘솔 2.5, diagnostics 2.47, 서비스 enable/disable 2.52, 공개키 없는 키페어 2.91, 하이퍼바이저 통계 2.87, attachVolume 2.100, 플레이버 생성 2.101 등)
- 서버 응답 필드(2.3~2.100), 2.47 내장 flavor(`getFlavorSummary()`), 2.98 image properties, 2.89 볼륨 attachment 필드
- 서버 생성 옵션(tags 2.52, trusted certs 2.63, hostname 2.90, description 2.19, networks auto/none 2.37, 네트워크·BDM tag 2.42, volume_type 2.67)과 `ServerListOptions`(2.26/2.66/2.73/2.83 필터)
- 서버 액션: lock 사유(2.73), migrate host(2.56), live-migrate(2.25/2.30), evacuate(2.29), rebuild(2.54/2.57/2.63/2.90), unshelve(2.77/2.91), rescue, `createBackup` 이미지 id(2.45)
- 서버 하위 리소스: topology(2.78), ips, remote-consoles(2.6/2.8/2.99), 표준 diagnostics(2.48), 서버 마이그레이션(2.22~2.24), shares(2.97), 비동기 볼륨 연결(2.101), pinned AZ 수정(2.104)
- 하이퍼바이저·서비스·집계·quota·키페어·서버 그룹 보강(2.2/2.10/2.33/2.35/2.53/2.64/2.81), `MigrationListOptions`(2.59/2.66/2.80)
- 새 서비스: `serverExternalEvents()`, `assistedVolumeSnapshots()`, `consoleAuthTokens()`, `instanceUsageAuditLogs()`
- 공용 `MicroVersionException` 추가, `PlacementMicroVersionException` 이 이를 상속

## 4.1.0

- Placement API 전체 지원 (`os.placement().providers()/inventories()/resourceClasses()/traits()/aggregates()/allocations()/allocationCandidates()/usages()/reshaper()/versions()`), microversion 1.28~1.39 자동 협상과 `useMicroVersion` 고정
- Placement 오류를 `PlacementException`/`PlacementConcurrentUpdateException` 으로 매핑, `Placement.retryOnConcurrentUpdate` 도우미
- `usages().capacity()` 로 provider 별 자원 용량·사용량·여유량 계산
- 기존 `placement().resourceProviders()` 는 변경 없음

## 4.0.0 (seogineer fork)

첫 후속 포크 릴리스. 이전 방법은 MIGRATION.md 참고.

- BREAKING: groupId `io.github.seogineer`, JDK 17 이상 필요
- BREAKING: jersey2, resteasy connector 제거
- BREAKING: httpclient connector 가 Apache HttpClient 5 사용
- BREAKING: `openstack4j`(distribution) 가 core 클래스를 합치지 않고 core + httpclient 를 의존성으로 가짐
- http-connector 를 JDK HttpClient 로 재구현 (JDK 17+ 에서 PATCH 동작)
- okhttp connector 를 OkHttp 4.12 로 갱신, `Config.withSSLContext` 가 JDK 9+ 에서 예외를 던지던 문제 수정
- 서버가 닫은 keep-alive 연결 때문에 다음 요청이 실패하던 문제 수정 (httpclient: 재사용 전 연결 검증, http-connector: 1회 재시도)
- Jackson 2.22, Guava 33, SnakeYAML 2.7, SLF4J 2.0, json-patch 1.13 으로 갱신
- POM 라이선스 표기를 Apache 2.0 으로 수정
- Maven Central 배포를 Central Portal 로 전환, CI 를 JDK 17/21/25 와 Spring Boot 3 스모크 테스트로 확장

# Change Log

**Starting with 3.3, changelog has moved to [Releases page](https://github.com/openstack4j/openstack4j/releases)**

## Old releases

### 3.2.0 (Latest / Stable)

- Implement listCachedImages() for Glance V2 Images #1226
- Make all keystone v3 services (ProjectService, RoleService,..) work #1222
- Add support for “tags” of project which introduced in KeyStone v3.9 #1219
- Solves the issue #1198: "Identity service not working with OS4j". #1199
- neutron attach and detach network to agent dhcp #1082
- aodh-gnocchi alarm support #1130
- Fix for issue #979: Disable Gateway problem #1149
- Fix for issue #1124:openstack4j mistaken heat-cfn service as heat ser… #1142
- Heat service, adding support for stack abandon and adopt #1136
- Alarm test case fix #1209
- aodh-gnocchi alarm  support #1134
- Consider the SSLContext of an HttpRequest's Config object when using the Resteasy connector #1162
- Fix issue #1165. #1166
- Make sure the number of okhttp CLOSE_WAIT connections is always manageable #1151
- add loadbalancer_id to LbPoolV2Builder #1143
- Added SFC Service, all calls. #1141
- Better format used in toString method #1135
- Fix JSON deserialize bug cause by 'sessionPersistence' is abstract type #1127
- Add octavia error msg to ActionResponse #1140
- add vip_network_id and vip_port_id for lb post #1133
- Heat, add support for stack abandon and adopt #1135
- pull master #14
- aodh gnocchi driver support #13
- merge #12
- Fix parsing error when Glance V2 image contains optional locations #1096
- Add cascade delete load balancer, it will delete all child objects of… #1120
- Adding device_owner and device_id support for Port #1119
- add detach method to blockstorage api #1106
- merge #11
- Fix octavia pool update session_persistence with value "null" #1099
- Update volume bootable status #1098
- [issue 1094] Fix NPEs caused by okhttp HttpExecutorServiceImpl #1095
- add rescue server status #1100
- fix(secret): fix wrong representation of Secret values & add payload #1088
- Switch to standalone snakeyaml #1092
- Support Availability zones API in Neutron. #1086

### 3.1.0

- Issue : #[1070](https://github.com/ContainX/openstack4j/issues/1070) #1072
- fix  Firewall policy param cannot be updated #1074
- Added API for Octavia #1081
- Feature/barbican secret #1080
- Support multiattach property in volume #1079
- Wrong return type of disable/enable service actions #1076
- Fix in ServiceTypeEnum #1078
- Allow identity V3 to lookup service by name #1077
- Update javadoc for BlockDeviceMappingBuilder #1075
- Allow value to be object type in ServiceConfig #1061
- fix unscoped token authentication #1066
- Fix ETag header to match documented name: 'Etag' #1062
- Added a new method createSnapshot() to invoke create image action API with metadata properties #1056
- Adding limit to SampleCriteria for Ceilometer APIs #1052
- Update builder to set old deprecated fields to support volume creation (preserving name and description) for cinder API V1 #1058
- Mistral: add workflow environment and cron trigger endpoint #1055
- 928 ceilometer statistics date parsing #1049
- Renamed CinderVolume properties to be compatible with block storage API v2, fixed broken VolumeTests #1046
- Mistral: add task and action execution endpoints #1048
- Mistral: adding workflow executions endpoint #1043
- Mistral: implement endpoints for action definitions and workbook definitions #1039
- Mistral client: workflow definitions #1009
- Added missing fields in Magnum bay #1034
- Fix cpu info in hypervisor #1028
- Fix cinder volume snapshot #1031
- Add additional properties of the availability zone hint for the network. #1030
- merge #10
- Fix NeutronExtraDhcpOptCreate #1029
- Fix UntrustedSSL SSLVerificationDisabled may cause NullPointerException #1026
- merge #8
- Add http headers for supporting keystone tokenless #1002
- Api for glance image cache, list cached #1020
- update guava version to 20.0 #1021
- Override equal and hashcode method of some Neutron classes #1015
- cherry-picking #932 #1017
- 3.0.4 release #999
- designate v2 service #1011
- SenlinNodeService: details about physical object that backs the node #1001
- Add additional properties of block device mapping api v2 #989
- Allow Port Creation in Bulk #988
- Allow passing additional properties for Glance V2 #986
- Fix for correcting return result type in Magnum Container APIs #984
- add security group and rules filterable list method #981
- Implement Security Group Update API & Support Enable DHCP #978
- support ipv6 properties in subnet again #976
- support Blockstorage Service List #968
- Allow set ID when creating image #972
- Change date-time format in Sahara API to ISO 8601 #961
- Extended OS4J to support OpenStack Magnum Service APIs #969
- Adding neutron port binding data support. #967
- support ipv6 properties in subnet #964
- Fixing port name list option. #965
- add public host property getter #962
- issue 957: run as non-root user in docker #959
- Issue 944: etag support in os.objectStorage().objects().get() #958
- Fix get console output not working with Resteasy connector (#795) #949
- add missed 'os-vol-host-attr:host' property on CinderVolume #952
- fix domain null pointer exception #947
- added unit test readme #945
- OS4J client for Openstack Glare #926
- add Evacuate Server (evacuate Action) #931
- use Long insead of Integer in class GlanceImage #876
- Bug #913: add SSLContext and HostnameVerifier to hashCode and equals #923
- Issue 903 remove last route #917
- Add api for nova service list #914
- Trove Update #910
- Openstack telemetry aodh support #900
- Kept the portsecurityenabled property as undefined #904

### 3.0.0 

This is a major milestone release which was driven and lead by @auhlig and @dhague. Special thanks for their major contributions and making this release possible.

As a result we have added 2.0-maintenance branch which is the path to EOL for OpenStack4j 2.0.X series.

##### Breaking Changes

* Java 8 as minimum - therefore dropping support for Jersey2-jdk16 connector
* Upgraded Jackson to 2.7.3
* SLF4j 1.7.21 as sole logging implementation

##### Misc
* Introduced Groovy tests written in Groovy using Betamax to record and replay client-server communication
* Clean split of the implementation by Identity version:
* Package names reflect different  versions (*.identity.v2.* and *.identity.v3.* )
* Instead of the old version-mixed OSClient a OSClientV2 for only Identity V2 API and OSClientV3 for only Identity V3 API are available
* Same refactoring for the OSClientSession: introducing new OSClientSessionV2 and OSClientV3

### 2.20

This marks the last of the 2.X OpenStack4j versioning scheme.  Going forward only critical or specific pull requests will justify future release.  It is strongly encourage to consider moving to OpenStack4j 3.0.X which offers the full Identity V3 Support.

### 2.0.9

##### Enhancements/Improvements

* Issue #503 - domain scoped authentication with tests - Thank you - @auhlig  

##### Fixes

* Issue #454 - Tenant and user id not in Volume - Thank you - @iviireczech 



### 2.0.8

##### Enhancements/Improvements

* Issue #492 - Telemetry Events API - Thank you - @iviireczech  
* Issue #500 - Use Enums instead of String for sourceType and destinationType in block mappings

##### Fixes

* Issue #489 - Race Condition in DefaultEndpointURLResolver - Thank you - @rvesselinov
* Issue #501 - Avoid using printStackTrace() for exception logging

### 2.0.7 

##### Enhancements/Improvements

* Issue #479 - Support for Identity V3 - Thank you - @auhlig 
* Issue #474 - NetQuotaService does not accept/return security_group and security_group_rule quota - Thank you @abareghi
* Issue #390 - Support for newest Service Versions

##### Fixes

* Issue #485 - Not returning sub-directories under a directory in Swift - Thank you - @sbcd90
* Issue #470 - Create (boot) server call fails against DevStack due to optional parameters in JSON payload in the request 
* Issue #458 - java.lang.NullPointerException when getting servers list - Thank you @symcssn 
* Issue #448 - RACKSPACE: Unable to Find Supported Services

### 2.0.6

##### Enhancements/Improvements

* Issue #450 - Make configDrive() accessible from Server Create builder - Thank you - @imclem 

##### Fixes

* Issue #459 - NPE from keystone v3 re-auths - Thank you - @symcssn 
* Issue #446 - DLPayload does not return proper response to allow proper handling - Thank you @symcssn 
* Issue #444 - OSAuthenticator.authenticateV3 does not reauthenticate properly - Thank you @symcssn 

### 2.0.5 

##### Enhancements/Improvements

* Issue #439 - Add query parameters to delete and put options for large objects - Thank you - @frsyuki
* Issue #438 - Add support for RAX API key authentication extension - Thank you - @frsyuki
* Issue #437 - Add support to list available zone list - Thank you - @csm1sh
* Issue #436 - New commands for blockVolume - Thank you - @pompinis
* Issue #431 - Added docker container format - Thank you - @maseev

##### Fixes

* Issue #424 - Create server error through blockDeviceMappingBuilder
* Issue #410 - Create instance boot from image（create a new volume）

### 2.0.4

##### Enhancements/Improvements

* Issue #392 - Allow HttpClient interceptor to allow for custom configuration prior to client creation
* Issue #366 - Identity v3 scoped auth token
* Issue #358 - Create OSClient from auth_token
* Issue #352 - Ability to set Proxy information within OpenStack4j Config
* Issue #350 - Add support to determine if Ceilometer is installed
* Issue #349 - Update LBaaS to use Enums instead of String values
* Issue #348 - Neutron (FwaaS) Firewall Policy API & Test Cases (Completed API)
* Issue #343 - Add filter support when listing Volume and Volume Snapshot
* Issue #340 - Support for Network Extension: Firewall As a Service (FwaaS)
* Issue #337 - Enhancement Request: ability to fetch ALL metadata for objects/containers
* Issue #332 - Refactor HttpClient Connector to use singleton of the Apache HttpClient 

##### Fixes

* Issue #420 - Bug fix for the get_files. The key should be the file name
* Issue #418 - Bugs regarding updating object metadata
* Issue #414 - FlavorServiceImpl.listExtraSpecs NullPointerException
* Issue #413 - ObjectStorageObjectServiceImpl.get(ObjectLocation) throws NPE 
* Issue #412 - httpclient connector leaks connections on failed authentication attempts and eventually hangs
* Issue #411 - [Partial Fix] Connection leak from usage of BaseOpenStackService.Invocation.executeWithResponse()
* Issue #403 - The KeystoneServiceEndpoint miss @JsonRootName("endpoint")
* Issue #401 - NullPointerException from listing operation and authentication operation
* Issue #397 - FloatingIPServiceImpl.create don't need floatingIp.getPortId()
* Issue #394 - HttpClient connection leak when processing Server Errors
* Issue #393 - The FlavorServiceImpl.removeTenantAccess method error
* Issue #391 - FlavorServiceImpl.getSpec /flavors/%s/os-extra_specs/%s/ uri error
* Issue #376 - Deserialization error with HttpURL connector 
* Issue #373 - Connection leak when using HttpClient connector
* Issue #365 - Compute: Add/Remove SecGroup 500 exception not caught.
* Issue #360 - Connection leak when using HttpClient connector
* Issue #357 - ObjectService.list(containerDoesNotExist) throws NullPointerException
* Issue #356 - Requests to Swift hang after 2nd request
* Issue #336 - Exception when deleting an unexisting Glance image
* Issue #333 - Default value set to 0 for NetSecurityGroupRules : port range
* Issue #327 - SwiftAccount account = os.objectStorage().account().get()
* Issue #314 - EofSensorInputStream throws JsonParseException for 404s. Also, inconsistent behavior of how 404s are handled

### 2.0.3

##### Enhancements/Improvements

* Issue #322 - Add MIGRATING to the Server Status Enumeration
* Issue #321 - Option to add headers in requests, esp. 'X-newest' in GET requests in swift
* Issue #316 - Expose error code in ActionResponse
* Issue #308 - Add PENDING_CREATE to Neutron Network State Enumeration
* Issue #294 - NetQuota : updateForTenant support request
* Issue #272 - Quota-Set for Block Storage 
* Issue #267 - Configuring connection pooling feature for underlying client connectors

##### Fixes

* Issue #326 - BlockStorageVolumeBuilder : allow availability_zone to be set
* Issue #329 - Update default quotaSet shows exception
* Issue #325 - Volumes metadata is not set due to wrong parameter name sent from OS4j 
* Issue #323 - Setting external gateway to router in api returns not authorized, but the same is allowed via CLI
* Issue #307 - Unable to catch assign floating IP error
* Issue #295 - Exception while updating quota for tenant
* Issue #292 - CreatePort command not working properly

### 2.0.2

##### Enhancements/Improvements

* Issue #290 - Update OKHttp Connector to version 2.3.0
* Issue #289 - AbsoluteLimit: `maxServerGroups` and `maxServerGroupMembers` are missing
* Issue #284 - Extending ceilometer alarms functionality (thank you @e3ky)
* Issue #278 - Filtering of Attached Interfaces
* Issue #264 - Keystone v3 Authentication for Project Scope
* Issue #258 - Reset VM State support
* Issue #255 - HttpURLConnection connector option (thank you @krishnabrucelee)
* Issue #247 - Support to update Quotas for Tenant and Class
* Issue #233 - Ability to set criteria on Ceilometer Statistics
* Issue #217 - Support to build a shared network (thank you @octupszhang)
* Issue #216 - Telemetry API doesn't support alarms (thank you @e3ky)
* Issue #216 - Add support for Sahara data processing (thank you @ekasitk)
* Issue #208 - Support for LBaaS (Load-balancer as a service) (thank you @liujunpengwork)
* Issue #203 - Support Host aggregates (thank you @liujunpengwork)
* Issue #187 - Support for Volume Transfer
* Issue #183 - Heat: Nested templates (Files & Env) support for a Stack (thank you @magixyu)

##### Fixes

* Issue #286 - Error while downloading the object from container
* Issue #285 - ResetAction not working against a Server
* Issue #282 - Quota set volume & gigabytes is always 0 and instances and do not update
* Issue #281 - NeutronError while detaching interface is not caught
* Issue #277 - Option to clear gateway once set
* Issue #268 - RouterExternal flag missing in NetworkBuilder interface
* Issue #263 - Not closing response when 404 happens
* Issue #262 - Creating a Port by using an existing as a template (cloning) fails
* Issue #261 - Unable to pass SampleCriteria as a parameter in sample() method
* Issue #257 - VM Migration Exception (ActionResponse fault)
* Issue #249 - Unable to capture Create Subnet Error
* Issue #241 - HttpClient Connector: Assign/Remove floating IP to the server throws JsonMappingException
* Issue #237 - Rackspace URL for images are incorrectly determined
* Issue #236 - HP Cloud: Failed to create network port
* Issue #234 - Volumes cannot be used on HP Cloud
* Issue #227 - OSFactory.clientFromAccess(access) doesn't fetch admin scoped tenant/users
* Issue #226 - Creating Network on HP Cloud fails
* Issue #225 - NullPointerException in HttpExecutor when ServiceLoader finds no HttpExecutorService
* Issue #222 - ObjectStorage: file download
* Issue #220 - NetworkType enum doesn't support "vxlan" value
* Issue #211 - NullPointerException: Find Tenant By Name
* Issue #201 - Find a specific Tenant using getByName function raise exception

### 2.0.1

##### Enhancements/Improvements

* Issue #182 - os-availability-zone support
* Issue #177 - Heat: Nested template support - Thanks @magixyu 
* Issue #175 - Implement os-floating-ip-dns resource
* Issue #174 - Neutron: Ability to set DNS Nameserver and Host Route within SubnetBuilder  
* Issue #173 - Ceilometer: Ability to specify 'project' in obtaining samples using SampleCriteria
* Issue #171 - Upload volume to image  
* Issue #169 - Update a neutron network
* Issue #165 - volume migrate and force delete support  
* Issue #164 - Interface attachment support

##### Fixes

* Issue #181 - Can't get VNCConsoleURL using RestEasy connector
* Issue #172 - ClassCastException with HttpClient-2.0.1 connecter while downloading image
* Issue #163  - State vs Status in the NeutronPort
* Issue #161  - Issue with attaching a Volume
* Issue #160 - Adding users to a tenant is not working

### 2.0.0

##### Enhancements/Improvements

* Issue #155 - Add flag to allow for Legacy endpoint handling (has broken some users)
* Issue #154 - Ceilometer - Support query criteria when retrieving samples
* Issue #152 - Add ability to dynamically change Endpoint URL for NAT Firewalls
* Issue #150 - Heat - ability to retreive raw JSON/Yaml template for a Stack
* Issue #145 - Enabled wire logging for HttpClient based connectors
* Issue #128 - Heat - ability to update a Stack
* Issue #125 - Compute - update a server (instance) name
* Issue #120 - Compute - flavor extra spec support for scheduler hints
* Issue #114 - Support for SPICE Html5 console
* PR    #107 - Server group support - Thank you @octupszhang
* Issue #105 - Full server metadata support
* Issue #102 - Support for backup server action
* Issue #101 - Limits for Cinder and Neutron
* Issue #98 - API Support for Object Store (Swift)
* Issue #87 - Software Configuration support for HEAT
* Issue #85 - Support for os-migrations extension
* Issue #83 - Live server migration
* Issue #79 - Support for OKHttp connector
* Issue #77 - Log4j and SLF4j Plugin support
* Issue #71 - Support for other connector choices (Jersey2, Resteasy, HttpClient)
* Issue #70 - Upgrade Jersey from 2.0 to 2.10.1
* Issue #69 - Break out our current connector (Jersey) into a sub-module to offer various connector choices
* Issue #67 - Upgrade Jackson 1 to Jackson 2
* Issue #33 - Ability to specify a custom socket factory and host name verifier

##### Fixes

* Issue #148 - Compute: Creating a keypair without specifying public key does not return auto-created private key
* Issue #147 - API version is not set in service URL when using HTTPS
* Issue #146 - Bad request when using HTTPS
* Issue #142 - Creating a volume from ImageRef returns volume object without populating original reference
* Issue #135 - Deleting Non-existent KeyPair yields: Can not deserialize instance of java.lang.String out of START_OBJECT token
* Issue #134 - JDK 1.6 compatibility support option with Jersey 2 connector
* Issue #131 - Regression: Error adding a floating IP
* Issue #130 - NPE when creating a new Network
* Issue #129 - Unable to add a user to a tenant using RestEasy connector
* Issue #127 - Strange return value for Server.getOsExtendedVolumesAttached
* Issue #117 - SecGroupExtension.Rule.IPProtocol can be null instead of ANY or UNRECOGNIZED
* Issue #116 - Some methods in Service classes return void instead of ActionResponse
* Issue #115 - Method waitForServerStatus throws NPE
* Issue #113 - Return value for deallocateIP is void instead of ActionResponse
* Issue #111 - Writer errors when using the resteasy-connector
* Issue #106 - ActionResponse status is incorrect for addFloatingIP
* Issue #103 - Issue when updating a port in network
* Issue #97 - Yaml based template ignored in Stack creation
* Issue #95 - Easy determination within an Image to determine if it's a snapshot of a volume
* Issue #93 - Error listing images (Error reading entity from input stream)
* Issue #92 - Problems in deserializing an object (Heat Stack)
* Issue #91 - Implementation of the method "getHosts()" in the class availabilityZoneList
* Issue #90 - Volume Status "downloading" not recognized
* Issue #75 - NPE in NeutronNetwork
* Issue #20 - Glance - creating a large image throws out of memory error

### 1.0.2

##### Enhancements/Improvements

* Heat Orchestration Support
* Issue #65 - Instance rebuild with options support
* Issue #56 - Set userdata when creating a server
* Issue #34, Issue #27 - Support for HP Cloud / Identity V3 authentication
* Issue #45 - Seamless retry/auto-renew support when a token has expired. Will re-execute original request after renewal
* Issue #15 - Ability to specify a Region to call against
* Issue #26 - Ability to set Availibility Zone to a Server
* Issue #19 - Boot and Wait for Server to become ACTIVE based upon max wait time
 

##### Fixes
* Issue #58 - Server status "SHUTOFF" showing as UNRECOGNIZED enum value on query
* Issue #50 - List of SecurityGroupRule returns null
* Issue #49 - Listing of servers error in some environments
* Issue #18 - Network Facing Perspectives to determine endpoint resolution
* Issue #5, #47 PR - Availability Zones listing

### 1.0.1

* Block Storage (Cinder) support
* Floating IP/Pools support in Compute
* VNC and Console Output support in Compute -> Servers
* Personality support on Compute -> Server -> Create
* Security Group & Rules in Compute
* Keypair support
* Floating IP in Network
* Security Groups & Rules in Network
* Telemetry (Ceilometer) support

### 1.0.0

* Initial public release
