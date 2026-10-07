package org.openstack4j.openstack.manila.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.manila.ext.ShareMessageService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.ext.ShareMessage;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.manila.domain.ext.ManilaShareMessage;
import org.openstack4j.openstack.manila.domain.ext.ManilaShareMessage.ManilaShareMessageList;
import org.openstack4j.openstack.manila.internal.ManilaMicroVersions;

public class ShareMessageServiceImpl extends BaseManilaExtService implements ShareMessageService {

    private static final MicroVersion FLOOR = ManilaMicroVersions.V(37);

    @Override
    public List<? extends ShareMessage> list() {
        return list(null);
    }

    @Override
    public List<? extends ShareMessage> list(Map<String, String> filters) {
        return listOf(FLOOR, ManilaShareMessageList.class, "/messages", filters);
    }

    @Override
    public ShareMessage get(String id) {
        return show(FLOOR, ManilaShareMessage.class, "/messages/" + id(id));
    }

    @Override
    public ActionResponse delete(String id) {
        return remove(FLOOR, "/messages/" + id(id));
    }
}
