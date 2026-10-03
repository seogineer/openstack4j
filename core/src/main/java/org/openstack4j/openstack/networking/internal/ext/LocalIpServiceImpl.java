package org.openstack4j.openstack.networking.internal.ext;

import org.openstack4j.openstack.networking.domain.ext.NeutronLocalIpPortAssociation.Associations;
import org.openstack4j.openstack.networking.domain.ext.NeutronLocalIpPortAssociation;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.model.network.ext.LocalIpPortAssociation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openstack4j.api.networking.ext.LocalIpService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.LocalIp;
import org.openstack4j.model.network.options.LocalIpOptions;
import org.openstack4j.openstack.networking.domain.ext.NeutronLocalIp;
import org.openstack4j.openstack.networking.domain.ext.NeutronLocalIp.LocalIps;

public class LocalIpServiceImpl extends BaseNeutronExtService implements LocalIpService {

    private static final String PATH = "/local_ips";
    private static final String ROOT = "local_ip";

    @Override public List<? extends LocalIp> list() { return listOf(LocalIps.class, PATH, null); }
    @Override public List<? extends LocalIp> list(Map<String, String> filters) { return listOf(LocalIps.class, PATH, filters); }
    @Override public LocalIp get(String id) { return show(NeutronLocalIp.class, PATH + "/" + id(id)); }
    @Override public LocalIp create(LocalIpOptions options) { return create(NeutronLocalIp.class, PATH, ROOT, options); }
    @Override public LocalIp update(String id, LocalIpOptions options) { return update(NeutronLocalIp.class, PATH + "/" + id(id), ROOT, options); }
    @Override public ActionResponse delete(String id) { return remove(PATH + "/" + id(id)); }

    @Override
    public List<? extends LocalIpPortAssociation> portAssociations(String localIpId) {
        return listOf(Associations.class, PATH + "/" + id(localIpId) + "/port_associations", null);
    }

    @Override
    public LocalIpPortAssociation associatePort(String localIpId, String fixedPortId, String fixedIp) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fixed_port_id", id(fixedPortId));
        if (fixedIp != null)
            body.put("fixed_ip", fixedIp);
        return post(NeutronLocalIpPortAssociation.class, PATH + "/" + id(localIpId) + "/port_associations").entity(JsonBody.of("port_association", body)).execute(NeutronExecution.propagate404());
    }

    @Override
    public ActionResponse disassociatePort(String localIpId, String fixedPortId) {
        return remove(PATH + "/" + id(localIpId) + "/port_associations/" + id(fixedPortId));
    }
}
