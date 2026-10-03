package org.openstack4j.openstack.networking.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.LocalIpPortAssociation;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("port_association")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronLocalIpPortAssociation implements LocalIpPortAssociation {

    private static final long serialVersionUID = 1L;

    @JsonProperty("local_ip_id") private String localIpId;
    @JsonProperty("local_ip_address") private String localIpAddress;
    @JsonProperty("fixed_port_id") private String fixedPortId;
    @JsonProperty("fixed_ip") private String fixedIp;
    @JsonProperty("host") private String host;

    @Override public String getLocalIpId() { return localIpId; }
    @Override public String getLocalIpAddress() { return localIpAddress; }
    @Override public String getFixedPortId() { return fixedPortId; }
    @Override public String getFixedIp() { return fixedIp; }
    @Override public String getHost() { return host; }

    public static class Associations extends ListResult<NeutronLocalIpPortAssociation> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("port_associations")
        private List<NeutronLocalIpPortAssociation> list;

        @Override
        protected List<NeutronLocalIpPortAssociation> value() {
            return list;
        }
    }
}
