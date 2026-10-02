package org.openstack4j.openstack.compute.internal;

import java.io.IOException;
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
}
