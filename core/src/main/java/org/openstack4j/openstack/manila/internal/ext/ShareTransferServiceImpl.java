package org.openstack4j.openstack.manila.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.manila.ext.ShareTransferService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.ext.ShareTransfer;
import org.openstack4j.model.manila.ext.options.ShareTransferCreate;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.manila.domain.ext.ManilaShareTransfer;
import org.openstack4j.openstack.manila.domain.ext.ManilaShareTransfer.ManilaShareTransferList;
import org.openstack4j.openstack.manila.internal.ManilaMicroVersions;

public class ShareTransferServiceImpl extends BaseManilaExtService implements ShareTransferService {

    private static final MicroVersion FLOOR = ManilaMicroVersions.V(77);

    @Override
    public List<? extends ShareTransfer> list() {
        return list(null);
    }

    @Override
    public List<? extends ShareTransfer> list(Map<String, String> filters) {
        return listOf(FLOOR, ManilaShareTransferList.class, "/share-transfers/detail", filters);
    }

    @Override
    public ShareTransfer get(String id) {
        return show(FLOOR, ManilaShareTransfer.class, "/share-transfers/" + id(id));
    }

    @Override
    public ShareTransfer create(ShareTransferCreate create) {
        return create(FLOOR, ManilaShareTransfer.class, "/share-transfers", "transfer", create);
    }

    @Override
    public ActionResponse accept(String id, String authKey, boolean clearAccessRules) {
        Map<String, Object> accept = new java.util.LinkedHashMap<>();
        accept.put("auth_key", java.util.Objects.requireNonNull(authKey, "authKey"));
        accept.put("clear_access_rules", clearAccessRules);
        String path = "/share-transfers/" + id(id) + "/accept";
        return send(FLOOR, postWithResponse(path), path, Map.of("accept", accept));
    }

    @Override
    public ActionResponse delete(String id) {
        return remove(FLOOR, "/share-transfers/" + id(id));
    }
}
