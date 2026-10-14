package org.openstack4j.openstack.manila.internal.ext;

import static org.openstack4j.openstack.manila.internal.ManilaMicroVersions.V;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.manila.ext.ShareNetworkExtService;
import org.openstack4j.model.manila.ext.ShareNetworkInfo;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.manila.domain.ext.ManilaShareNetworkInfo;
import org.openstack4j.openstack.manila.domain.ext.ManilaShareNetworkInfo.ManilaShareNetworkInfoList;

public class ShareNetworkExtServiceImpl extends BaseManilaExtService implements ShareNetworkExtService {

    private static final MicroVersion NETWORKS = V(7);

    @Override
    public List<? extends ShareNetworkInfo> list(Map<String, String> filters) {
        return listOf(NETWORKS, ManilaShareNetworkInfoList.class, "/share-networks", filters);
    }

    @Override
    public List<? extends ShareNetworkInfo> listDetails(Map<String, String> filters) {
        return listOf(NETWORKS, ManilaShareNetworkInfoList.class, "/share-networks/detail", filters);
    }

    @Override
    public ShareNetworkInfo get(String shareNetworkId) {
        return show(NETWORKS, ManilaShareNetworkInfo.class, "/share-networks/" + id(shareNetworkId));
    }
}
