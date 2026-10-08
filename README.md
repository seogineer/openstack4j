OpenStack4j (seogineer fork)
============================

[![CI](https://github.com/seogineer/openstack4j/actions/workflows/ci.yaml/badge.svg)](https://github.com/seogineer/openstack4j/actions/workflows/ci.yaml)
[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](LICENSE)

**English** | [한국어](README.ko.md)

> **This repository is a successor fork of [openstack4j/openstack4j](https://github.com/openstack4j/openstack4j).**
> Upstream development stopped after 3.12 (2024-05). This fork targets JDK 17+ / Spring Boot 3 and current dependencies,
> and aims to cover every API of the latest OpenStack release. The Java packages (`org.openstack4j`) are unchanged;
> only the Maven coordinates move to `io.github.seogineer`. See [MIGRATION.md](MIGRATION.md) for moving from 3.x.

OpenStack4j is a fluent OpenStack client that allows provisioning and control of an OpenStack deployment.   This includes support for Identity, Compute, Image, Network, Block Storage, Telemetry, Data Processing as well as many extensions (LBaaS, FWaaS, Quota-Sets, etc)

## Installation

```xml
<dependency>
    <groupId>io.github.seogineer</groupId>
    <artifactId>openstack4j</artifactId>
    <version>4.5.0</version>
</dependency>
```

`openstack4j` pulls in the core together with the Apache HttpClient 5 connector. To use another connector, see [connectors/README.md](connectors/README.md).

## Supported environments

| Component | JDK 17 | JDK 21 | JDK 25 |
|---|---|---|---|
| core | 🟢 | 🟢 | 🟢 |
| httpclient (Apache HttpClient 5) | 🟢 | 🟢 | 🟢 |
| okhttp (OkHttp 4) | 🟢 | 🟢 | 🟢 |
| http-connector (JDK HttpClient) | 🟢 | 🟢 | 🟢 |

CI also checks a build together with Spring Boot 3.5 (`examples/spring-boot-smoke`).

## Documentation

The upstream documentation ([openstack4j.github.io](https://openstack4j.github.io/)) and the usage examples below still apply; just use the Maven coordinates above.
See [MIGRATION.md](MIGRATION.md) for moving from 3.x and [CHANGELOG.md](CHANGELOG.md) for the changes.

## Placement

Since 4.1.0 the whole Placement API (microversions 1.28–1.39) is supported. The client negotiates the server's microversion range automatically, and you can pin a version if you want.

```java
PlacementService placement = os.placement();
placement.versions().get();                      // server range and negotiated version
placement.useMicroVersion("1.36");               // optional: pin a version

// capacity
Map<String, ResourceCapacity> capacity = placement.usages().capacity(rpUuid);

// update an inventory (retry on a generation conflict)
Placement.retryOnConcurrentUpdate(3, () -> {
    ResourceProviderInventories inv = placement.inventories().list(rpUuid);
    return placement.inventories().update(rpUuid, inv.getResourceProviderGeneration(), "VCPU",
            Inventory.builder().total(64).allocationRatio(4.0f).build());
});

// scheduling candidates
placement.allocationCandidates().list(AllocationCandidatesQuery.builder()
        .resources("VCPU", 2).resources("MEMORY_MB", 2048).required("HW_CPU_X86_AVX2").limit(10).build());
```

The existing `placement().resourceProviders()` keeps working. On servers older than 1.28 the new APIs throw `PlacementMicroVersionException`.

## Compute microversions

Since 4.2.0 Nova microversions (2.1–2.104) can be turned on per session. They are off by default: without them no microversion header is sent (Nova answers as 2.1) and existing code behaves as before.

```java
ComputeVersion v = os.compute().microVersions().negotiate();   // min(2.104, server max)
os.compute().microVersions().use("2.79");                      // or pin a version
os.compute().microVersions().clear();                          // back to no header

Server server = os.compute().servers().get(id);
server.getFlavorSummary().getOriginalName();                   // 2.47+ embedded flavor
server.getTags();                                              // 2.26+

os.compute().servers().lock(id, "maintenance");                // 2.73+
os.compute().servers().list(ServerListOptions.create().locked(true).tags("web"));
os.compute().servers().boot(Builders.server().name("vm").flavor(f).image(i)
        .autoAllocateNetwork().hostname("vm-01").build());     // 2.37+, 2.90+
```

- Existing methods that use APIs Nova removed in later microversions (the proxy APIs such as `floatingIps()`, `securityGroups()`, `images()`, `host()`, the old `getVNCConsole`, `diagnostics`, creating a keypair without a public key, …) are sent at the highest microversion that still supports them, so they keep working after `negotiate()`.
- New features check the microversion they need before the request and throw `MicroVersionException` when the session does not send it (or microversions are off).
- The chosen version is kept per client session and compute endpoint.

## Block storage microversions

Since 4.3.0 Cinder v3 microversions (3.0–3.71) can be turned on per session. They are off by default: without them no microversion header is sent (Cinder answers as 3.0) and existing code behaves as before.

```java
BlockStorageVersion v = os.blockStorage().microVersions().negotiate();   // min(3.71, server max)
os.blockStorage().microVersions().use("3.50");                            // or pin a version
os.blockStorage().microVersions().clear();                                // back to no header

Volume volume = os.blockStorage().volumes().get(id);
volume.getVolumeTypeId();                                                 // 3.63+
volume.getConsumesQuota();                                                // 3.65+

os.blockStorage().volumes().list(VolumeListOptions.create().status("available").withCount(true));   // 3.45+
os.blockStorage().attachments().create(volumeId, serverId, null, "rw");   // 3.27+, mode 3.54+
os.blockStorage().groups().create(GroupCreate.create("g", groupTypeId, List.of(typeId)));         // 3.13+
```

- A volume create body with `bootable` (rejected from 3.53) and a snapshot create with `force=false` (rejected from 3.66) are sent at 3.52 and 3.65, so existing calls keep working after `negotiate()`. The other existing methods have the same format up to 3.71.
- New features check the microversion they need before the request and throw `MicroVersionException` when the session does not send it (or microversions are off).
- Compute and block storage microversion state are independent and kept per client session and endpoint.

## Identity v3 extensions

Since 4.4.0 the Keystone authentication methods and extension APIs (OS-*) are supported. Existing authentication bodies and methods are unchanged.

```java
// application credential (no scope — the credential itself is bound to a project)
OSClientV3 os = OSFactory.builderV3().endpoint(url).applicationCredential(credentialId, secret).authenticate();

// MFA (password + TOTP), system scope, trust scope
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

- New accessors: `applicationCredentials()`, `trusts()`, `endpointFilter()`, `endpointPolicies()`, `limits()`, `registeredLimits()`, `federation()`, `oauth1()` (with HMAC-SHA1 signing), `oauth2()`, `revocationEvents()`, `systemRoles()`
- Existing services extended: project tags, OS-INHERIT inherited roles, implied roles and role inferences, domain configuration, access rules, `UserListOptions`/`ProjectListOptions`
- A TOTP passcode is single-use, so a TOTP session cannot re-authenticate by itself when its token expires. Call `authenticate()` again with a new passcode.
- Calling `authenticate()` again on the same thread makes that client the current session. Use `OSFactory.clientFromToken(token)` to go back to an earlier client.

## Networking extensions

Since 4.5.0 most of the Neutron (in-tree) API is supported. Existing methods and request bodies are unchanged.

```java
os.networking().extensions().isEnabled("qos");
os.networking().qosRules().createDscpMarkingRule(policyId, QosRuleOptions.dscpMarking(26));
SubnetPool pool = os.networking().subnetPools().create(SubnetPoolOptions.create("pool", List.of("10.0.0.0/16")).defaultPrefixlen(24));
os.networking().addressGroups().addAddresses(groupId, List.of("10.0.2.100/32"));
os.networking().rbacPolicies().create(RbacPolicyOptions.create("network", networkId, "access_as_shared", projectId));
os.networking().router().addExtraRoutes(routerId, routes);
os.networking().port().listBindings(portId);
```

- New accessors: `extensions()`, `serviceProviders()`, `autoAllocatedTopology()`, `qosRules()`, `subnetPools()`, `addressScopes()`, `addressGroups()`, `rbacPolicies()`, `defaultSecurityGroupRules()`, `securityGroupDefaultStatefulness()`, `segments()`, `networkSegmentRanges()`, `localIps()`, `ndpProxies()`, `serviceFlavors()`, `serviceProfiles()`, `metering()`, `logging()`
- Existing services extended: router (extra routes, external gateways, conntrack helpers, l3 agents), agent (scheduling), port (bindings), quotas (default, details), floating IP pools, port forwarding update
- Option classes (`*Options`) send only the fields you set. Use `attribute("field", null)` to clear a value.
- When the extension is off or a parent resource is missing (404): methods that return a model or a list (lists, create, update, and actions that return a result such as adding prefixes or addresses) throw the 404 as an exception. Methods that return an `ActionResponse` (delete, import/stage/cache actions, agent scheduling, linking a profile to a flavor) return a failed `ActionResponse` (code 404), and a single `get(id)` returns `null` for a missing resource. Check first with `extensions().isEnabled(alias)`.
- The stadium projects (VPNaaS, FWaaS v2, BGP, BGPVPN, TaaS, SFC service graphs) are supported from 4.6.0 — see below.
- 4.5.0 added `NetQosPolicy.getId()` (before, you had to cast to the implementation class).

## Neutron stadium projects

The same release supports the separately installed Neutron projects. Each needs its plugin on the server; check with `extensions().isEnabled(alias)` (`vpnaas`, `fwaas_v2`, `bgp`, `bgpvpn`, `taas`, `tap-mirror`, `service_graph`).

```java
VpnService vpn = os.networking().vpnServices().create(VpnServiceOptions.create(routerId).name("vpn"));
os.networking().ipsecSiteConnections().create(IpsecSiteConnectionOptions.create(vpn.getId(), ikeId, ipsecId, peerIp, peerId, psk)
        .localEpGroupId(localGroupId).peerEpGroupId(peerGroupId));
os.networking().firewallPoliciesV2().insertRule(policyId, ruleId, null, null);
os.networking().bgpSpeakers().addPeer(speakerId, peerId);
os.networking().bgpvpnAssociations().associateNetwork(bgpvpnId, networkId);
os.networking().tapFlows().create(TapFlowOptions.create(tapServiceId, sourcePortId, "BOTH"));
```

- VPNaaS: `vpnServices()`, `ikePolicies()`, `ipsecPolicies()`, `ipsecSiteConnections()`, `vpnEndpointGroups()`
- FWaaS v2: `firewallGroups()`, `firewallPoliciesV2()` (insert/remove rule), `firewallRulesV2()` — the FWaaS v1 `firewalls()` methods are unchanged
- BGP dynamic routing: `bgpSpeakers()` (peers, gateway networks, advertised routes, dynamic routing agents), `bgpPeers()`; BGPVPN: `bgpvpns()`, `bgpvpnAssociations()` (network, router, port)
- TaaS: `tapServices()`, `tapFlows()`, `tapMirrors()`; SFC: `sfcServiceGraphs()`
- Models have getters for the common fields; the rest are in `getAttributes()`. The 404 rules are those of the networking extensions.

## Image extensions

Since 4.6.0 the rest of the Glance v2 API is supported. Existing methods and request bodies are unchanged.

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

- New sub-services: `info()` (import methods, stores, usage), `cache()` (API 2.14), `schemas()`, `metadefs()` (namespaces, resource types, objects, properties, tags)
- Added to `ImageService`: `versions()`, `importImage`, `stage`, `listLocations`/`addLocation` (2.17), `listTasks` (2.12), `deleteFromStore` (2.10)
- Glance has no microversion header. A Glance without the new API (an older server, the cache middleware turned off) answers 404, handled as in the networking extensions: model- and list-returning methods throw, `ActionResponse` methods return a failed response (code 404), and a single `get(id)` returns `null`. Check first with `versions().supports("2.x")`.

## Octavia extensions

The same release supports the rest of the Octavia (load balancer v2) API.

```java
os.octavia().l7Policies().create(L7PolicyOptions.create(listenerId, "REDIRECT_TO_URL").redirectUrl("https://example.com"));
os.octavia().l7Policies().createRule(policyId, L7RuleOptions.create("PATH", "STARTS_WITH", "/api"));
os.octavia().flavors().create(OctaviaFlavorOptions.create("single", flavorProfileId));
os.octavia().quotas().update(projectId, OctaviaQuotaOptions.create().loadbalancer(20));
os.octavia().lbPoolV2().updateMembers(poolId, members, false);    // batch replace
os.octavia().loadBalancerV2().failover(lbId);
```

- New accessors: `l7Policies()` (with rules), `flavors()`, `flavorProfiles()`, `availabilityZones()`, `availabilityZoneProfiles()`, `providers()`, `quotas()`, `amphorae()` (admin)
- Existing services extended: `listenerV2().stats`, `loadBalancerV2().failover`, `lbPoolV2().updateMembers`
- The 404 rules are the same as for the image and networking extensions. A quota of `null` means the default, `-1` means unlimited.

## Heat extensions

The same release supports the rest of the Heat (orchestration) API.

```java
os.heat().stacks().preview(stackCreate);                          // preview a create
os.heat().stacks().outputs(name, id); os.heat().stacks().output(name, id, "value");
os.heat().stacks().suspend(name, id); os.heat().stacks().resume(name, id);
os.heat().stacks().snapshot(name, id, "before-upgrade");
os.heat().resourceTypes().schema("OS::Nova::Server");
os.heat().softwareDeployments().create(SoftwareDeploymentOptions.create(serverId, configId));
```

- New accessors: `info()` (build info, services), `templateVersions()`, `resourceTypes()`, `softwareDeployments()`
- `stacks()` extended: delete(name), environment, export, files, outputs, PATCH update, actions (suspend/resume/check/cancel), snapshots, preview; `events().list(stackName)`, `softwareConfig().list()`

## Bare metal (Ironic)

The same release supports the Ironic (bare metal v1) API. Most fields and APIs need a higher microversion, so call `negotiate()` first in each session (microversions are off by default — without a header the server answers as 1.1).

```java
os.baremetal().microVersions().negotiate();                       // server max (up to 1.107)
Node node = os.baremetal().nodes().create(NodeCreate.create("ipmi").name("bm-1").driverInfo(Map.of("ipmi_address", "192.0.2.1")));
os.baremetal().nodes().update("bm-1", List.of(BaremetalPatch.replace("/description", "rack 3")));   // JSON Patch
os.baremetal().nodes().setProvisionState("bm-1", NodeProvision.target("manage"));
os.baremetal().nodes().setPowerState("bm-1", "power on");
os.baremetal().ports().create(PortCreate.create(node.getUuid(), "52:54:00:12:34:56"));
```

- `nodes()`: list (detail, filters), get, create, update (JSON Patch), delete, states (power, provision, RAID, boot mode, secure boot), console, boot device, NMI, validate, maintenance, traits, VIFs
- `ports()`, `portgroups()` (including per-node and per-portgroup lists), `chassis()`, `drivers()` (properties, RAID logical disk properties)
- `allocations()`, `deployTemplates()`, `runbooks()`, `inspectionRules()`, `volumeConnectors()`/`volumeTargets()`, `conductors()` (with shards); node BIOS, firmware, history, inventory, children, virtual media, indicators and vendor passthru
- Addressing a node by name (`nodes().get("bm-1")`) needs microversion 1.5 or later — call `negotiate()` first
- Models have getters for the common fields; the rest (fields that grow with each microversion) are in `getAttributes()`

## DNS (Designate) extensions

The same release supports the rest of the Designate v2 API.

```java
os.dns().zones().list(Map.of("name", "example.org."));
ZoneExport export = os.dns().zoneFiles().export(zoneId);           // when complete, getExportContent(id) is the zone file
os.dns().zoneFiles().importZone(zoneFileText);                      // text/dns
ZoneTransferRequest offer = os.dns().zoneTransfers().createRequest(zoneId, targetProjectId, null);
os.dns().reverseFloatingIps().set("RegionOne", floatingIpId, "smtp.example.com.", null, 600);
```

- zones: filtered list, abandon, `transferFromMaster` (xfr), `movePool`; recordsets: filtered list (all zones or one zone)
- New accessors: `zoneFiles()` (export/import), `zoneShares()`, `zoneTransfers()` (requests/accepts), `tlds()`, `tsigKeys()`, `blacklists()`, `pools()`, `quotas()`, `serviceStatuses()`, `info()` (limits), `reverseFloatingIps()`
- Models have getters for the common fields; the rest are in `getAttributes()`
- To act on other projects as an admin (zones of all projects, another project's quotas) Designate needs the header `X-Auth-All-Projects: true` or `X-Auth-Sudo-Project-ID: <id>`. For now pass it as a session header: `os.headers(Map.of("X-Auth-All-Projects", "true"))` — it is added to every request of the session and replaces other session headers, so clear it with `os.headers(null)` after the calls that need it

## Shared file systems (Manila) microversions and extensions

The same release supports the Manila v2 APIs from 2.7 on. The existing `os.share()` methods keep sending 2.6 (their paths and action names exist only up to 2.6). The new methods are sent at their own minimum microversion, or at the session version (server max, up to 2.99) after `negotiate()`.

```java
os.share().microVersions().negotiate();                            // affects only the new methods
os.share().sharesExt().listExportLocations(shareId);               // 2.9
os.share().shareReplicas().create(ShareReplicaCreate.create(shareId).availabilityZone("az2"));
os.share().shareGroups().create(ShareGroupCreate.create().shareTypes(List.of(typeId)));
os.share().shareBackups().create(ShareBackupCreate.create(shareId)); // experimental header is added for you
```

- New accessors: `messages()`, `administration()` (the 2.7+ paths of availability zones, services, quotas and type access), `sharesExt()` (export locations, instances, manage, revert, soft delete, access rules, migration), `snapshotsExt()`, `shareReplicas()`, `shareGroups()`/`shareGroupSnapshots()`/`shareGroupTypes()`, `shareNetworkSubnets()`, `shareServersExt()`, `shareBackups()`, `shareTransfers()`, `resourceLocks()`, `qosTypes()`
- When the session version is lower than a method's minimum, `MicroVersionException` is thrown before the request
- Experimental APIs (migration below 2.96, share server migration, share backups) send `X-OpenStack-Manila-API-Experimental: True` for you
- The new models keep status as text; the other fields are in `getAttributes()`

## Key manager (Barbican) extensions

The same release supports the rest of the Barbican v1 API. The existing `secrets()` and `containers()` methods are unchanged.

```java
os.barbican().acls().setSecretAcl(secretId, List.of(userId), false);      // read ACL: only these users
os.barbican().secretsExt().storeTextPayload(secretId, "s3cr3t");          // two-step secret creation
String payload = os.barbican().secretsExt().getTextPayload(secretId);
os.barbican().secretsExt().addMetadataItem(secretId, "owner", "team-a");
String orderRef = os.barbican().orders().create("key", Map.of("name", "aes", "algorithm", "AES", "bit_length", 256));
os.barbican().quotas().setProjectQuotas(projectId, Map.of("secrets", 50));
```

- New accessors: `acls()` (secret and container read ACLs), `secretsExt()` (payload, user metadata, consumers — API 1.1 header added for you), `containersExt()` (secrets, consumers), `orders()`, `quotas()` (effective and project quotas), `secretStores()` (multiple back ends, preferred store)
- Methods take an id or the `*_ref` URL Barbican returns (its last path segment is used)
- Models have getters for the common fields; the rest are in `getAttributes()`

## Database (Trove) extensions

The same release supports the rest of the Trove v1.0 API. The existing `instanceService()`, `databaseService()`, `databaseUsersService()`, `datastoreService()` and `flavorService()` are unchanged.

```java
os.trove().instancesExt().resizeVolume(instanceId, 20);
os.trove().instancesExt().attachConfiguration(instanceId, configurationId);
os.trove().instancesExt().enableLog(instanceId, "slow_query");
Backup backup = os.trove().backups().create(BackupOptions.create("nightly").instance(instanceId));
os.trove().configurations().create(ConfigurationOptions.create("tuned", Map.of("max_connections", 500)));
os.trove().troveAdmin().getQuotas(projectId);                     // admin
```

- New accessors: `instancesExt()` (detail list, rename, configuration attach/detach, datastore upgrade, replica detach, access, restart/resize/promote/eject/reset status, instance backups, configuration defaults, logs, SSL, root), `backups()`, `backupStrategies()`, `configurations()`, `datastoresExt()` (version by id, configuration parameters, delete datastore, limits), `troveAdmin()` (`/mgmt` instances, actions, root history, datastore versions, parameters, quotas)
- Responses whose shape varies by datastore (logs, SSL, parameters, admin views) are returned as `Map`s

## Object storage, alarming, container infra and workflow additions

```java
os.objectStorage().info();                                         // GET /info (cluster capabilities)
os.objectStorage().listEndpoints("photos", "cat.jpg");             // list_endpoints middleware
os.telemetry().alarmsExt().history(alarmId, null);                 // Aodh: history, state, query, quotas
os.telemetry().alarmsExt().setState(alarmId, "alarm");
os.magnum().extensions().resizeCluster(clusterId, 5, null, null);  // container-infra 1.7 header added for you
os.magnum().extensions().upgradeCluster(clusterId, templateId, 1, null);
```

- Swift: `info()`, `listEndpoints(container, object)`
- Aodh: `alarmsExt()` — alarm history, state get/set, complex queries of alarms and history, quotas
- Magnum: `extensions()` — cluster resize (1.7) and upgrade (1.8), CA certificate by type, quotas, stats
- Mistral: `workflow().extensions()` — workflow/workbook/action validation, code sources, dynamic actions, event triggers, sub-executions, execution reports, workflow sharing (members)

## Instance HA (Masakari)

New service `os.instanceHa()` (Masakari v1): failover segments, their hosts, failure notifications and the instance moves of a recovery.

```java
Segment segment = os.instanceHa().segments().create(SegmentOptions.create("rack-a", "COMPUTE", "auto"));
os.instanceHa().hosts().create(segment.getUuid(), HostOptions.create("compute-01", "COMPUTE", "SSH"));
os.instanceHa().notifications().listVMoves(notificationId, null);    // instance-ha 1.3 header added for you
```

## Reservation (Blazar)

New service `os.reservation()` (Blazar v1): leases and the host and floating IP pools that leases reserve from.

```java
Lease lease = os.reservation().leases().create(Map.of("name", "gpu-week", "start_date", "now", "end_date", "2026-10-15 12:00",
        "reservations", List.of(Map.of("resource_type", "physical:host", "min", 1, "max", 2, "hypervisor_properties", "", "resource_properties", "")),
        "events", List.of()));
os.reservation().hosts().create(Map.of("name", "compute-07", "gpu", "a100"));   // admin
```

## Rating (CloudKitty) and accelerators (Cyborg)

New services `os.rating()` (CloudKitty v2: rated dataframes, rating modules, scopes, summaries, reprocessing) and `os.accelerator()` (Cyborg v2: accelerator requests, device profiles, devices, deployables, attributes). Results are `Map`s, as their shapes depend on the deployment's collectors and drivers.

```java
os.rating().summary(Map.of("groupby", "project_id", "begin", "2026-10-01T00:00:00Z"));
os.accelerator().getDeviceProfile("fpga-profile");                   // by name: accelerator 2.2 header added for you
```

## Messaging (Zaqar)

New service `os.messaging()` (Zaqar v2): queues (metadata, JSON Patch, stats, pre-signed share, purge), messages (post, list, by ids, pop, delete), claims, subscriptions (incl. confirm), pools, flavors and health. Every request carries a `Client-ID` header: the one in the client's `headers(...)`, else the one set with `useClientId`, else a UUID generated per client. Zaqar hides a client's own messages from listings unless `echo=true`.

```java
os.messaging().useClientId("3381af92-2b9e-11e3-b191-71861300734c");
os.messaging().postMessages("demo", List.of(Map.of("body", Map.of("event", "BackupStarted"), "ttl", 300)));
os.messaging().claimMessages("demo", 300, 300, 5);                   // claim_id + messages (no messages when none are free)
```

## Registration (Adjutant)

New service `os.registration()` (Adjutant v1): admin tasks (list with filters, approve, update, cancel), tokens (reissue, submit, purge expired) and notifications (acknowledge), plus project self-service: users (invite, cancel invite), user roles, grantable roles, password reset, email update, sign-up and quota sizes.

```java
os.registration().inviteUser("alice@example.com", List.of("member"), null, null);
os.registration().listTasks(Map.of("tasks_per_page", "25"), Map.of("approved", Map.of("exact", false)));
os.registration().submitToken(token, Map.of("password", newPassword));
```

## Resource optimization (Watcher)

New service `os.optimization()` (Watcher v1): audit templates, audits, action plans (start), actions (skip), goals, strategies (state), scoring engines, services, the compute data model and webhooks. Lists return their page (`next` holds the following page's URL). Methods that need a newer API send `OpenStack-API-Version: infra-optim <version>` themselves; `useApiVersion("1.7")` (or `latest`) opts every request into a version so newer fields such as `status_message` come back.

```java
os.optimization().createAudit(Map.of("goal", "server_consolidation", "audit_type", "ONESHOT"));
os.optimization().startActionPlan(actionPlanUuid);
os.optimization().getDataModel("compute", null);                     // infra-optim 1.3 added for you
```

## Application containers (Zun)

New service `os.containerApp()` (Zun v1): containers (create/run, update, delete, start/stop/reboot/pause/unpause/kill, rebuild, resize, execute, logs, top, stats, attach, archives, commit, networks, security groups, actions), images (pull, search), hosts, services, capsules, quotas, quota classes, availability zones, networks and registries. Without a header Zun answers at 1.1, so methods and create fields that need more send `OpenStack-API-Version: container <version>` themselves; `useApiVersion` opts every request in.

```java
os.containerApp().createContainer(Map.of("image", "nginx", "command", List.of("nginx", "-g", "daemon off;")), true);  // container 1.20
os.containerApp().executeContainer("web", "uname -a", true, false);
```

## NFV (Tacker ETSI NFV-SOL)

`os.tacker()` gains the ETSI NFV-SOL APIs next to the legacy v1.0 ones: `vnfPackages()` (`/vnfpkgm/v1`: create, CSAR upload or upload from URI, download content/VNFD/artifacts), `vnfLcm()` (`/vnflcm/v2`) and `vnfLcmV1()` (`/vnflcm/v1`): VNF instances, instantiate/terminate/heal/scale/change_ext_conn/change_vnfpkg, operation occurrences (retry, rollback, fail, cancel), subscriptions; `vnfFaults()` (`/vnffm/v1`) and `vnfPerformance()` (`/vnfpm/v2`). The required `Version` header is sent for you; lifecycle operations return the id of their operation occurrence; lists return `items` and `next` (the `nextpage_opaque_marker`). `vim().update(...)` is added too.

```java
String instanceId = (String) os.tacker().vnfLcm().createVnfInstance(Map.of("vnfdId", vnfdId)).get("id");
String opOccId = os.tacker().vnfLcm().instantiate(instanceId, Map.of("flavourId", "simple"));
os.tacker().vnfLcm().getLcmOpOcc(opOccId).get("operationState");
```

## Build

```bash
./mvnw verify
```

## Bugs and contributing

Please open an issue on [GitHub Issues](https://github.com/seogineer/openstack4j/issues). See [CONTRIBUTING.md](CONTRIBUTING.md) for how to contribute.

## License

Apache License 2.0. Based on the work of the original author Jeremy Unruh and the [upstream openstack4j contributors](https://github.com/openstack4j/openstack4j/graphs/contributors) ([NOTICE](NOTICE)).

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
