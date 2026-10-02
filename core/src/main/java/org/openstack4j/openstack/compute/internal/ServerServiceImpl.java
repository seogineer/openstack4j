package org.openstack4j.openstack.compute.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.openstack4j.api.Apis;
import org.openstack4j.api.compute.ServerService;
import org.openstack4j.api.compute.ext.InstanceActionsService;
import org.openstack4j.api.compute.ext.InterfaceService;
import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.HttpResponse;
import org.openstack4j.core.transport.propagation.PropagateOnStatus;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.*;
import org.openstack4j.model.compute.Server.Status;
import org.openstack4j.model.compute.VNCConsole.Type;
import org.openstack4j.model.compute.actions.BackupOptions;
import org.openstack4j.model.compute.actions.EvacuateRequest;
import org.openstack4j.model.compute.actions.LiveMigrateRequest;
import org.openstack4j.model.compute.actions.RebuildRequest;
import org.openstack4j.model.compute.actions.RescueRequest;
import org.openstack4j.model.compute.actions.UnshelveRequest;
import org.openstack4j.model.compute.actions.EvacuateOptions;
import org.openstack4j.model.compute.actions.LiveMigrateOptions;
import org.openstack4j.model.compute.actions.RebuildOptions;
import org.openstack4j.model.compute.builder.ServerCreateBuilder;
import org.openstack4j.openstack.common.Metadata;
import org.openstack4j.openstack.compute.domain.*;
import org.openstack4j.openstack.compute.domain.NovaServer.Servers;
import org.openstack4j.openstack.compute.domain.actions.*;
import org.openstack4j.openstack.compute.domain.actions.ServerAction;
import org.openstack4j.openstack.compute.domain.actions.BasicActions.*;
import org.openstack4j.openstack.compute.functions.ToActionResponseFunction;
import org.openstack4j.openstack.compute.functions.WrapServerIfApplicableFunction;
import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.microversion.MicroVersions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.openstack4j.openstack.compute.domain.actions.CreateSnapshotAction.create;
import static org.openstack4j.openstack.compute.internal.ComputeMicroVersions.V;

/**
 * Server Operation API implementation
 *
 * @author Jeremy Unruh
 */
public class ServerServiceImpl extends BaseComputeServices implements ServerService {

    private static final Logger LOG = LoggerFactory.getLogger(ServerServiceImpl.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends Server> list() {
        return list(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends Server> list(boolean detail) {
        return list(detail, Boolean.FALSE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends Server> listAll(boolean detail) {
        return list(detail, Boolean.TRUE);
    }

    private List<? extends Server> list(boolean detail, boolean allTenants) {
        Invocation<Servers> req = get(Servers.class, uri("/servers" + ((detail) ? "/detail": "")));
        if (allTenants)
            req.param("all_tenants", 1);
        return req.execute().getList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends Server> list(Map<String, String> filteringParams) {
        Invocation<Servers> serverInvocation = get(Servers.class, "/servers/detail");
        if (filteringParams != null) {
            for (Map.Entry<String, String> entry : filteringParams.entrySet()) {
                serverInvocation = serverInvocation.param(entry.getKey(), entry.getValue());
            }
        }
        return serverInvocation.execute().getList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Server get(String serverId) {
        Objects.requireNonNull(serverId);
        return get(NovaServer.class, uri("/servers/%s", serverId)).execute();
    }

    /**
     * {@inheritDoc}
     */
@Override
    public Server boot(ServerCreate server) {
        Objects.requireNonNull(server);
        return capped(post(NovaServer.class, uri("/servers")), bootCeiling(server))
                .entity(WrapServerIfApplicableFunction.INSTANCE.apply(server))
                .execute();
    }

    /** Checks the floors of the options used and returns the highest microversion the request can be sent at. */
    private MicroVersion bootCeiling(ServerCreate server) {
        MicroVersion floor = null;
        String floorFeature = null;
        Map<String, MicroVersion> used = new LinkedHashMap<>();
        if (server.getDescription() != null) used.put("description", V(19));
        if (server.getNetworksMode() != null) used.put("networks \"" + server.getNetworksMode() + "\"", V(37));
        if (server.getNetworks() != null && server.getNetworks().stream().anyMatch(n -> n.getTag() != null)) used.put("network tag", V(42));
        if (server.getBlockDeviceMapping() != null && server.getBlockDeviceMapping().stream().anyMatch(b -> b.getTag() != null)) used.put("block device tag", V(42));
        if (server.getTags() != null) used.put("tags", V(52));
        if (server.getTrustedImageCertificates() != null) used.put("trusted image certificates", V(63));
        if (server.getHostname() != null) used.put("hostname", V(90));
        if (effectiveMicroVersion(null) != null) {
            if (server.getHost() != null || server.getHypervisorHostName() != null) used.put("host / hypervisor_hostname", V(74));
            if (server.getBlockDeviceMapping() != null && server.getBlockDeviceMapping().stream().anyMatch(b -> b.getVolumeType() != null)) used.put("block device volume_type", V(67));
        }
        for (Map.Entry<String, MicroVersion> e : used.entrySet()) {
            requireMicroVersion("Server create option " + e.getKey(), e.getValue());
            if (floor == null || e.getValue().compareTo(floor) > 0) {
                floor = e.getValue();
                floorFeature = e.getKey();
            }
        }
        MicroVersion ceiling = null;
        String ceilingReason = null;
        if (server.getPersonality() != null && !server.getPersonality().isEmpty()) {
            ceiling = V(56);
            ceilingReason = "personality (removed in 2.57)";
        }
        if (server.getNetworksMode() == null && (server.getNetworks() == null || server.getNetworks().isEmpty())
                && (ceiling == null || V(36).compareTo(ceiling) < 0)) {
            ceiling = V(36);
            ceilingReason = "no networks (required from 2.37; use autoAllocateNetwork() or noNetwork())";
        }
        if (floor != null && ceiling != null && floor.compareTo(ceiling) > 0)
            throw new MicroVersionException("Server create option " + floorFeature + " needs " + floor + " and cannot be combined with "
                    + ceilingReason + ", which needs " + ceiling + " or lower");
        return ceiling;
    }

    @Override
    public List<? extends Server> list(ServerListOptions options) {
        Objects.requireNonNull(options);
        if (options.getRequiredMicroVersion() != null)
            requireMicroVersion("Server list filters " + options.toQueryParams().keySet(), MicroVersions.parse(options.getRequiredMicroVersion()));
        Invocation<Servers> req = get(Servers.class, uri("/servers/detail"));
        options.toQueryParams().forEach(req::param);
        return req.execute().getList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Server bootAndWaitActive(ServerCreate server, int maxWaitTime) {
        return waitForServerStatus(boot(server).getId(), Status.ACTIVE, maxWaitTime, TimeUnit.MILLISECONDS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse delete(String serverId) {
        Objects.requireNonNull(serverId);
        return ToActionResponseFunction.INSTANCE.apply(
                delete(Void.class, uri("/servers/%s", serverId)).executeWithResponse()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse action(String serverId, Action action) {
        Objects.requireNonNull(serverId);

        ServerAction instance = BasicActions.actionInstanceFor(action);
        if (instance == null)
            return ActionResponse.actionFailed(String.format("Action %s was not found in the list of invokable actions", action), 412);

        return invokeAction(serverId, instance);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createSnapshot(String serverId, String snapshotName) {
        return invokeCreateSnapshotAction(serverId, snapshotName, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createSnapshot(String serverId, String snapshotName, Map<String, String> metadata) {
        return invokeCreateSnapshotAction(serverId, snapshotName, metadata);
    }

    private String invokeCreateSnapshotAction(String serverId, String snapshotName, Map<String, String> metadata) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(snapshotName);
        CreateSnapshotAction createSnapshotAction = metadata != null && !metadata.isEmpty() ? create(snapshotName, metadata): create(snapshotName);
        return imageIdFrom(invokeActionWithResponse(serverId, createSnapshotAction));
    }

    /** Image id of a createImage/createBackup response: the Location header (before 2.45) or {"image_id"} (2.45+). */
    private static String imageIdFrom(HttpResponse response) {
        if (response.getStatus() != 202) {
            response.getEntity(Void.class);    // propagates errors as before
            return null;
        }
        try {
            String location = response.header("location");
            if (location != null && location.contains("/")) {
                String[] s = location.split("/");
                return s[s.length - 1];
            }
            try {
                Map<?, ?> body = response.readEntity(HashMap.class);
                return body == null || body.get("image_id") == null ? null : String.valueOf(body.get("image_id"));
            } catch (RuntimeException e) {
                return null;                   // empty or non-JSON body
            }
        } finally {
            try {
                response.close();
            } catch (IOException ignored) {
                // the connection is released either way
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse reboot(String serverId, RebootType type) {
        Objects.requireNonNull(serverId);
        return invokeAction(serverId, new Reboot(type));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse rebuild(String serverId, RebuildOptions options) {
        Objects.requireNonNull(serverId);
        return invokeAction(serverId, RebuildAction.create(options), V(56));    // personality removed in 2.57
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse resize(String serverId, String flavorId) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(flavorId);

        return invokeAction(serverId, new Resize(flavorId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse addSecurityGroup(String serverId, String secGroupName) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(secGroupName);
        return invokeAction(serverId, SecurityGroupActions.add(secGroupName));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse removeSecurityGroup(String serverId, String secGroupName) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(secGroupName);
        return invokeAction(serverId, SecurityGroupActions.remove(secGroupName));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse confirmResize(String serverId) {
        Objects.requireNonNull(serverId);
        return invokeAction(serverId, BasicActions.instanceFor(ConfirmResize.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse revertResize(String serverId) {
        Objects.requireNonNull(serverId);
        return invokeAction(serverId, BasicActions.instanceFor(RevertResize.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getConsoleOutput(String serverId, int numLines) {
        Objects.requireNonNull(serverId);

        // Build options with the given numLines or default to full output
        ConsoleOutputOptions consoleOutputOptions;
        if (numLines <= 0)
            consoleOutputOptions = new ConsoleOutputOptions();
        else
            consoleOutputOptions = new ConsoleOutputOptions(numLines);

        ConsoleOutput c = post(ConsoleOutput.class, uri("/servers/%s/action", serverId))
                .entity(consoleOutputOptions).execute();
        return (c != null) ? c.getOutput(): null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VNCConsole getVNCConsole(String serverId, Type type) {
        Objects.requireNonNull(serverId);
        if (type == null)
            type = Type.NOVNC;

        return capped(post(NovaVNCConsole.class, uri("/servers/%s/action", serverId)), V(5))
                .entity(NovaVNCConsole.getConsoleForType(type))
                .execute();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, ? extends Number> diagnostics(String serverId) {
        // 2.48 returns a structured document; see diagnosticsStandard
        return capped(get(HashMap.class, uri("/servers/%s/diagnostics", serverId)), V(47)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServerCreateBuilder serverBuilder() {
        return NovaServerCreate.builder();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VolumeAttachment attachVolume(String serverId, String volumeId, String device) {
        // 2.101 answers 202 without a body; see attachVolumeAsync
        return capped(post(NovaVolumeAttachment.class, uri("/servers/%s/os-volume_attachments", serverId)), V(100))
                .entity(NovaVolumeAttachment.create(volumeId, device))
                .execute(ExecutionOptions.<NovaVolumeAttachment>create(PropagateOnStatus.on(404)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse detachVolume(String serverId, String attachmentId) {
        return ToActionResponseFunction.INSTANCE.apply(
                delete(Void.class, uri("/servers/%s/os-volume_attachments/%s", serverId, attachmentId)).executeWithResponse()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse migrateServer(String serverId) {
        Objects.requireNonNull(serverId);
        return invokeAction(serverId, BasicActions.instanceFor(Migrate.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse liveMigrate(String serverId, LiveMigrateOptions options) {
        Objects.requireNonNull(serverId);
        if (options == null)
            options = LiveMigrateOptions.create();
        return invokeAction(serverId, LiveMigrationAction.create(options), V(24));    // disk_over_commit removed in 2.25
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse resetState(String serverId, Status state) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(state);
        return invokeAction(serverId, ResetStateAction.create(state));
    }

    /**
     * {{@link #invokeAction(String, String)}
     */
    @Override
    public ActionResponse backupServer(String serverId, BackupOptions options) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(options);
        return invokeAction(serverId, BackupAction.create(options));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse changeAdminPassword(String serverId, String adminPassword) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(adminPassword);
        return invokeAction(serverId, new ChangePassword(adminPassword));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Server waitForServerStatus(String serverId, Status status, int maxWait, TimeUnit maxWaitUnit) {
        Objects.requireNonNull(serverId);
        Server server = null;
        long duration = 0;
        long maxTime = maxWaitUnit.toMillis(maxWait);
        while (duration < maxTime) {
            server = get(serverId);

            if (server == null || server.getStatus() == status || server.getStatus() == Status.ERROR)
                break;

            duration += sleep(1000);
        }
        return server;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getMetadata(String serverId) {
        Objects.requireNonNull(serverId);
        return get(Metadata.class, uri("/servers/%s/metadata", serverId)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> updateMetadata(String serverId, Map<String, String> metadata) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(metadata);
        return put(Metadata.class, uri("/servers/%s/metadata", serverId)).entity(Metadata.toMetadata(metadata)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse deleteMetadataItem(String serverId, String key) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(key);
        return ToActionResponseFunction.INSTANCE.apply(
                delete(Void.class, uri("/servers/%s/metadata/%s", serverId, key)).executeWithResponse()
        );
    }

    private int sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            LOG.error(e.getMessage(), e);
        }
        return ms;
    }

    @Override
    public Server update(String serverId, ServerUpdateOptions options) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(options);

        return put(NovaServer.class, uri("/servers/%s", serverId)).entity(NovaServerUpdate.fromOptions(options)).execute();
    }

    @Override
    public InterfaceService interfaces() {
        return Apis.get(InterfaceService.class);
    }

    @Override
    public InstanceActionsService instanceActions() {
        return Apis.get(InstanceActionsService.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServerPassword getPassword(String serverId) {
        Objects.requireNonNull(serverId);
        return get(NovaPassword.class, uri("/servers/%s/os-server-password", serverId)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServerPassword evacuate(String serverId, EvacuateOptions options) {
        Objects.requireNonNull(serverId);

        // onSharedStorage removed and adminPass no longer returned from 2.14
        return capped(post(AdminPass.class, uri("/servers/%s/action", serverId)), V(13))
                .entity(EvacuateAction.create(options))
                .execute();
    }

    private ActionResponse invokeMapAction(String serverId, String action, Map<String, ?> body, MicroVersion ceiling) {
        HttpResponse response = capped(post(Void.class, uri("/servers/%s/action", serverId)), ceiling)
                .entity(JsonBody.of(action, body))
                .executeWithResponse();
        return ToActionResponseFunction.INSTANCE.apply(response, action);
    }

    private static MicroVersion lower(MicroVersion a, MicroVersion b) {
        return a == null ? b : b == null ? a : MicroVersions.min(a, b);
    }

    @Override
    public ActionResponse lock(String serverId, String reason) {
        Objects.requireNonNull(serverId);
        requireMicroVersion("Lock with a reason", V(73));
        return invokeMapAction(serverId, "lock", Collections.singletonMap("locked_reason", reason), null);
    }

    @Override
    public ActionResponse migrateServer(String serverId, String host) {
        Objects.requireNonNull(serverId);
        requireMicroVersion("Cold migration to a host", V(56));
        return invokeMapAction(serverId, "migrate", Collections.singletonMap("host", host), null);
    }

    @Override
    public ActionResponse liveMigrate(String serverId, LiveMigrateRequest request) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(request);
        MicroVersion ceiling = null;
        if (request.isBlockMigrationAuto())
            requireMicroVersion("block_migration \"auto\"", V(25));
        if (Boolean.TRUE.equals(request.getForce())) {
            requireMicroVersion("Forced live migration", V(30));
            ceiling = V(67);
        }
        if (request.getDiskOverCommit() != null) {
            if (request.isBlockMigrationAuto() || Boolean.TRUE.equals(request.getForce()))
                throw new MicroVersionException("disk_over_commit (2.24 or lower) cannot be combined with block_migration \"auto\" or force");
            ceiling = lower(ceiling, V(24));
        }
        MicroVersion effective = effectiveMicroVersion(ceiling);
        boolean modern = effective != null && effective.compareTo(V(25)) >= 0;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("host", request.getHost());
        if (request.isBlockMigrationAuto())
            body.put("block_migration", "auto");
        else if (request.getBlockMigration() != null)
            body.put("block_migration", request.getBlockMigration());
        else
            body.put("block_migration", modern ? "auto" : false);
        if (!modern)
            body.put("disk_over_commit", request.getDiskOverCommit() != null ? request.getDiskOverCommit() : false);
        if (request.getForce() != null)
            body.put("force", request.getForce());
        return invokeMapAction(serverId, "os-migrateLive", body, ceiling);
    }

    @Override
    public ActionResponse evacuate(String serverId, EvacuateRequest request) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(request);
        MicroVersion ceiling = null;
        if (Boolean.TRUE.equals(request.getForce())) {
            requireMicroVersion("Forced evacuate", V(29));
            ceiling = V(67);
        }
        if (request.getOnSharedStorage() != null) {
            if (request.getForce() != null)
                throw new MicroVersionException("onSharedStorage (2.13 or lower) cannot be combined with force (2.29+)");
            ceiling = lower(ceiling, V(13));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        if (request.getHost() != null) body.put("host", request.getHost());
        if (request.getAdminPass() != null) body.put("adminPass", request.getAdminPass());
        if (request.getOnSharedStorage() != null) body.put("onSharedStorage", request.getOnSharedStorage());
        if (request.getForce() != null) body.put("force", request.getForce());
        return invokeMapAction(serverId, "evacuate", body, ceiling);
    }

    @Override
    public ActionResponse rebuild(String serverId, RebuildRequest request) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("imageRef", request.getImageRef());
        if (request.getName() != null) body.put("name", request.getName());
        if (request.getAdminPass() != null) body.put("adminPass", request.getAdminPass());
        if (request.getMetadata() != null) body.put("metadata", request.getMetadata());
        if (request.getPreserveEphemeral() != null) body.put("preserve_ephemeral", request.getPreserveEphemeral());
        if (request.getAccessIPv4() != null) body.put("accessIPv4", request.getAccessIPv4());
        if (request.getAccessIPv6() != null) body.put("accessIPv6", request.getAccessIPv6());
        if (request.getDescription() != null) {
            requireMicroVersion("Rebuild description", V(19));
            body.put("description", request.getDescription());
        }
        if (request.getKeyName() != null || request.isRemoveKeyName()) {
            requireMicroVersion("Rebuild key_name", V(54));
            body.put("key_name", request.getKeyName());
        }
        if (request.getUserData() != null) {
            requireMicroVersion("Rebuild user_data", V(57));
            body.put("user_data", request.getUserData());
        }
        if (request.getTrustedImageCertificates() != null) {
            requireMicroVersion("Rebuild trusted_image_certificates", V(63));
            body.put("trusted_image_certificates", request.getTrustedImageCertificates());
        }
        if (request.getHostname() != null) {
            requireMicroVersion("Rebuild hostname", V(90));
            body.put("hostname", request.getHostname());
        }
        return invokeMapAction(serverId, "rebuild", body, null);
    }

    @Override
    public ActionResponse unshelve(String serverId, UnshelveRequest request) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(request);
        Map<String, Object> body = new LinkedHashMap<>();
        if (request.getAvailabilityZone() != null) {
            requireMicroVersion("Unshelve to an availability zone", V(77));
            body.put("availability_zone", request.getAvailabilityZone());
        }
        if (request.isUnpinAvailabilityZone()) {
            requireMicroVersion("Unshelve unpinning the availability zone", V(91));
            body.put("availability_zone", null);
        }
        if (request.getHost() != null) {
            requireMicroVersion("Unshelve to a host", V(91));
            body.put("host", request.getHost());
        }
        if (body.isEmpty())
            requireMicroVersion("Unshelve with a request body", V(77));
        return invokeMapAction(serverId, "unshelve", body, null);
    }

    @Override
    public ActionResponse rescue(String serverId, RescueRequest request) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(request);
        Map<String, Object> body = new LinkedHashMap<>();
        if (request.getAdminPass() != null) body.put("adminPass", request.getAdminPass());
        if (request.getRescueImageRef() != null) body.put("rescue_image_ref", request.getRescueImageRef());
        return invokeMapAction(serverId, "rescue", body, null);
    }

    @Override
    public String createBackup(String serverId, BackupOptions options) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(options);
        return imageIdFrom(invokeActionWithResponse(serverId, BackupAction.create(options)));
    }

    @Override
    public ServerTopology topology(String serverId) {
        Objects.requireNonNull(serverId);
        requireMicroVersion("Server topology", V(78));
        return get(NovaServerTopology.class, uri("/servers/%s/topology", serverId)).execute();
    }

    @Override
    public Addresses ips(String serverId) {
        Objects.requireNonNull(serverId);
        return get(NovaAddresses.class, uri("/servers/%s/ips", serverId)).execute();
    }

    @Override
    public List<? extends Address> ips(String serverId, String networkLabel) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(networkLabel);
        NovaNetworkIps ips = get(NovaNetworkIps.class, uri("/servers/%s/ips/%s", serverId, networkLabel)).execute();
        return ips == null ? Collections.emptyList() : ips.get(networkLabel);
    }

    @Override
    public RemoteConsole remoteConsole(String serverId, String protocol, String type) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(protocol);
        Objects.requireNonNull(type);
        requireMicroVersion("Remote consoles", V(6));
        if ("mks".equals(protocol))
            requireMicroVersion("MKS remote console", V(8));
        if ("spice-direct".equals(type))
            requireMicroVersion("spice-direct remote console", V(99));
        return post(NovaRemoteConsole.class, uri("/servers/%s/remote-consoles", serverId))
                .entity(new NovaRemoteConsole(protocol, type))
                .execute();
    }

    @Override
    public ServerDiagnosticsStandard diagnosticsStandard(String serverId) {
        Objects.requireNonNull(serverId);
        requireMicroVersion("Standard server diagnostics", V(48));
        return get(NovaServerDiagnosticsStandard.class, uri("/servers/%s/diagnostics", serverId)).execute();
    }

    @Override
    public ActionResponse attachVolumeAsync(String serverId, String volumeId, String device) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(volumeId);
        requireMicroVersion("Asynchronous volume attach", V(101));
        Map<String, Object> attachment = new LinkedHashMap<>();
        attachment.put("volumeId", volumeId);
        if (device != null)
            attachment.put("device", device);
        return ToActionResponseFunction.INSTANCE.apply(
                post(Void.class, uri("/servers/%s/os-volume_attachments", serverId))
                        .entity(JsonBody.of("volumeAttachment", attachment))
                        .executeWithResponse());
    }

    @Override
    public Server updatePinnedAvailabilityZone(String serverId, String availabilityZone) {
        Objects.requireNonNull(serverId);
        requireMicroVersion("Updating pinned_availability_zone", V(104));
        Map<String, Object> server = new HashMap<>();
        server.put("pinned_availability_zone", availabilityZone);
        return put(NovaServer.class, uri("/servers/%s", serverId))
                .entity(JsonBody.of("server", server))
                .execute();
    }

    @Override
    public List<? extends ServerMigration> migrations(String serverId) {
        Objects.requireNonNull(serverId);
        requireMicroVersion("Listing server migrations", V(23));
        return get(NovaServerMigration.NovaServerMigrations.class, uri("/servers/%s/migrations", serverId)).execute().getList();
    }

    @Override
    public ServerMigration migration(String serverId, String migrationId) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(migrationId);
        requireMicroVersion("Showing a server migration", V(23));
        return get(NovaServerMigration.class, uri("/servers/%s/migrations/%s", serverId, migrationId)).execute();
    }

    @Override
    public ActionResponse forceCompleteMigration(String serverId, String migrationId) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(migrationId);
        requireMicroVersion("Force-completing a live migration", V(22));
        return ToActionResponseFunction.INSTANCE.apply(
                post(Void.class, uri("/servers/%s/migrations/%s/action", serverId, migrationId))
                        .entity(JsonBody.of(Collections.singletonMap("force_complete", null)))
                        .executeWithResponse());
    }

    @Override
    public ActionResponse abortMigration(String serverId, String migrationId) {
        Objects.requireNonNull(serverId);
        Objects.requireNonNull(migrationId);
        requireMicroVersion("Aborting a live migration", V(24));
        return ToActionResponseFunction.INSTANCE.apply(delete(Void.class, uri("/servers/%s/migrations/%s", serverId, migrationId)).executeWithResponse());
    }
}
