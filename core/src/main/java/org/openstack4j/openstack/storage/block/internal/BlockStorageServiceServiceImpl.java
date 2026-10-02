package org.openstack4j.openstack.storage.block.internal;

import static org.openstack4j.openstack.storage.block.internal.BlockStorageMicroVersions.V;

import java.util.List;

import org.openstack4j.api.storage.ext.BlockStorageServiceService;
import org.openstack4j.model.storage.block.ext.Service;
import org.openstack4j.openstack.storage.block.domain.ext.ExtService.Services;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.storage.block.ServiceLogLevel;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.storage.block.domain.CinderServiceLogLevels;

/**
 * Block Storage Services service provides CRUD capabilities for Cinder service(s).
 *
 * @author Taemin
 */
public class BlockStorageServiceServiceImpl extends BaseBlockStorageServices implements BlockStorageServiceService {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends Service> list() {
        return get(Services.class, uri("/os-services")).execute().getList();
    }

    @Override
    public ActionResponse enable(String host, String binary) {
        return put(ActionResponse.class, uri("/os-services/enable")).entity(JsonBody.of(hostBinary(host, binary))).execute();
    }

    @Override
    public ActionResponse disable(String host, String binary) {
        return put(ActionResponse.class, uri("/os-services/disable")).entity(JsonBody.of(hostBinary(host, binary))).execute();
    }

    @Override
    public ActionResponse disableWithReason(String host, String binary, String reason) {
        Map<String, Object> body = hostBinary(host, binary);
        body.put("disabled_reason", Objects.requireNonNull(reason));
        return put(ActionResponse.class, uri("/os-services/disable-log-reason")).entity(JsonBody.of(body)).execute();
    }

    @Override
    public ActionResponse freeze(String host) {
        return put(ActionResponse.class, uri("/os-services/freeze")).entity(JsonBody.of(Collections.singletonMap("host", Objects.requireNonNull(host)))).execute();
    }

    @Override
    public ActionResponse thaw(String host) {
        return put(ActionResponse.class, uri("/os-services/thaw")).entity(JsonBody.of(Collections.singletonMap("host", Objects.requireNonNull(host)))).execute();
    }

    @Override
    public ActionResponse failoverHost(String host, String backendId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("host", Objects.requireNonNull(host));
        if (backendId != null) body.put("backend_id", backendId);
        return put(ActionResponse.class, uri("/os-services/failover_host")).entity(JsonBody.of(body)).execute();
    }

    @Override
    public ActionResponse failover(String host, String cluster, String backendId) {
        requireMicroVersion("Service failover", V(26));
        Map<String, Object> body = new LinkedHashMap<>();
        if (host != null) body.put("host", host);
        if (cluster != null) body.put("cluster", cluster);
        if (backendId != null) body.put("backend_id", backendId);
        return put(ActionResponse.class, uri("/os-services/failover")).entity(JsonBody.of(body)).execute();
    }

    @Override
    public List<? extends ServiceLogLevel> getLog(String binary, String server, String prefix) {
        requireMicroVersion("Service log levels", V(32));
        CinderServiceLogLevels levels = put(CinderServiceLogLevels.class, uri("/os-services/get-log")).entity(JsonBody.of(logBody(null, binary, server, prefix))).execute();
        return levels == null ? Collections.emptyList() : levels.getLevels();
    }

    @Override
    public ActionResponse setLog(String level, String binary, String server, String prefix) {
        requireMicroVersion("Service log levels", V(32));
        return put(ActionResponse.class, uri("/os-services/set-log")).entity(JsonBody.of(logBody(Objects.requireNonNull(level), binary, server, prefix))).execute();
    }

    private static Map<String, Object> hostBinary(String host, String binary) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("host", Objects.requireNonNull(host));
        body.put("binary", Objects.requireNonNull(binary));
        return body;
    }

    private static Map<String, Object> logBody(String level, String binary, String server, String prefix) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (level != null) body.put("level", level);
        if (binary != null) body.put("binary", binary);
        if (server != null) body.put("server", server);
        if (prefix != null) body.put("prefix", prefix);
        return body;
    }
}
