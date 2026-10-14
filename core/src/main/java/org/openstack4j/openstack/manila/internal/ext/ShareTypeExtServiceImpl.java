package org.openstack4j.openstack.manila.internal.ext;

import static org.openstack4j.openstack.manila.internal.ManilaMicroVersions.V;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.manila.ext.ShareTypeExtService;
import org.openstack4j.model.manila.ext.ShareTypeInfo;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.manila.domain.ext.ManilaShareTypeInfo.ManilaShareTypeInfoList;
import org.openstack4j.openstack.manila.domain.ext.ManilaShareTypeInfo.ManilaShareTypeInfoWrapper;

public class ShareTypeExtServiceImpl extends BaseManilaExtService implements ShareTypeExtService {

    private static final MicroVersion TYPES = V(7);

    @Override
    public List<? extends ShareTypeInfo> list(Map<String, String> filters) {
        return listOf(TYPES, ManilaShareTypeInfoList.class, "/types", filters);
    }

    @Override
    public ShareTypeInfo get(String shareTypeId) {
        ManilaShareTypeInfoWrapper wrapper = show(TYPES, ManilaShareTypeInfoWrapper.class, "/types/" + id(shareTypeId));
        return wrapper == null ? null : wrapper.getShareType();
    }
}
