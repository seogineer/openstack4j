package org.openstack4j.openstack.barbican.internal.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openstack4j.api.barbican.ext.BarbicanAclService;
import org.openstack4j.model.barbican.ext.BarbicanAcl;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.barbican.domain.ext.BarbicanReadAcl;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class BarbicanAclServiceImpl extends BaseBarbicanExtService implements BarbicanAclService {

    private static final com.fasterxml.jackson.databind.ObjectMapper PLAIN = new com.fasterxml.jackson.databind.ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public BarbicanAcl getSecretAcl(String secretId) {
        return read("/secrets/" + id(secretId) + "/acl");
    }

    @Override
    public ActionResponse setSecretAcl(String secretId, List<String> users, Boolean projectAccess) {
        return putWithResponse("/secrets/" + id(secretId) + "/acl").entity(body(users, projectAccess)).execute();
    }

    @Override
    public ActionResponse updateSecretAcl(String secretId, List<String> users, Boolean projectAccess) {
        return patchWithResponse("/secrets/" + id(secretId) + "/acl").entity(body(users, projectAccess)).execute();
    }

    @Override
    public ActionResponse deleteSecretAcl(String secretId) {
        return deleteWithResponse("/secrets/" + id(secretId) + "/acl").execute();
    }

    @Override
    public BarbicanAcl getContainerAcl(String containerId) {
        return read("/containers/" + id(containerId) + "/acl");
    }

    @Override
    public ActionResponse setContainerAcl(String containerId, List<String> users, Boolean projectAccess) {
        return putWithResponse("/containers/" + id(containerId) + "/acl").entity(body(users, projectAccess)).execute();
    }

    @Override
    public ActionResponse updateContainerAcl(String containerId, List<String> users, Boolean projectAccess) {
        return patchWithResponse("/containers/" + id(containerId) + "/acl").entity(body(users, projectAccess)).execute();
    }

    @Override
    public ActionResponse deleteContainerAcl(String containerId) {
        return deleteWithResponse("/containers/" + id(containerId) + "/acl").execute();
    }

    /** {@code {"read": {...}}}; an entity with no ACL yet answers {@code {"read": {"project-access": true}}}. */
    private BarbicanAcl read(String path) {
        Map<String, Object> body = mapOf(path);
        Object read = body == null ? null : body.get("read");
        return read == null ? new BarbicanReadAcl() : PLAIN.convertValue(read, BarbicanReadAcl.class);
    }

    private static JsonBody body(List<String> users, Boolean projectAccess) {
        // an empty read ACL makes Barbican drop the ACL entirely; deleteSecretAcl/deleteContainerAcl say that explicitly
        if (users == null && projectAccess == null)
            throw new IllegalArgumentException("Set users and/or projectAccess; use delete…Acl to remove an ACL");
        Map<String, Object> read = new LinkedHashMap<>();
        if (users != null)
            read.put("users", users);
        if (projectAccess != null)
            read.put("project-access", projectAccess);
        return JsonBody.of("read", read);
    }
}
