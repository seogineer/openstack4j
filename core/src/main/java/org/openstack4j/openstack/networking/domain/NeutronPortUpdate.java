package org.openstack4j.openstack.networking.domain;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.model.network.Port;

/**
 * The body of a port update: only the attributes Neutron accepts on PUT /ports/{id}; built as a copy so the caller's port is untouched.
 */
@JsonRootName("port")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronPortUpdate implements ModelEntity {

    private static final long serialVersionUID = 1L;

    @JsonProperty
    private String name;

    @JsonProperty("admin_state_up")
    private boolean adminStateUp;

    @JsonProperty("device_id")
    private String deviceId;

    @JsonProperty("device_owner")
    private String deviceOwner;

    @JsonProperty("fixed_ips")
    private Set<NeutronIP> fixedIps;

    @JsonProperty("allowed_address_pairs")
    private Set<NeutronAllowedAddressPair> allowedAddressPairs;

    @JsonProperty("trunkport:type")
    private String trunkPortType;

    @JsonProperty("trunkport:parent_id")
    private String trunkPortParentId;

    @JsonProperty("trunkport:vid")
    private String trunkPortVlanId;

    @JsonProperty("security_groups")
    private List<String> securityGroups;

    @JsonProperty("extra_dhcp_opts")
    private List<NeutronExtraDhcpOptCreate> extraDhcpOpts;

    @JsonProperty("port_security_enabled")
    private Boolean portSecurityEnabled;

    @JsonProperty("binding:host_id")
    private String hostId;

    @JsonProperty("binding:vnic_type")
    private String vNicType;

    @JsonProperty("binding:profile")
    private Map<String, Object> profile;

    @JsonIgnore
    private String qosPolicyId;

    @JsonIgnore
    private boolean qosPolicyIdSet;

    /** Request-only keys set through the builder; an explicit null is sent as JSON null. */
    @JsonAnyGetter
    @JsonInclude(content = JsonInclude.Include.ALWAYS)
    Map<String, Object> requestOnlyFields() {
        return qosPolicyIdSet ? Collections.singletonMap("qos_policy_id", qosPolicyId) : Collections.emptyMap();
    }

    public NeutronPortUpdate() {
    }

    /**
     * Creates a port update body which only encapsulates the updatable fields of a port
     *
     * @param port the port (source)
     * @return the port update object
     */
    @SuppressWarnings("unchecked")
    public static NeutronPortUpdate fromPort(Port port) {
        NeutronPortUpdate u = new NeutronPortUpdate();
        u.name = port.getName();
        u.adminStateUp = port.isAdminStateUp();
        u.deviceId = port.getDeviceId();
        u.deviceOwner = port.getDeviceOwner();
        u.fixedIps = (Set<NeutronIP>) port.getFixedIps();
        u.allowedAddressPairs = (Set<NeutronAllowedAddressPair>) port.getAllowedAddressPairs();
        u.trunkPortType = port.getTrunkPortType();
        u.trunkPortParentId = port.getTrunkPortParentId();
        u.trunkPortVlanId = port.getTrunkPortVlanId();
        u.securityGroups = port.getSecurityGroups();
        u.portSecurityEnabled = port.isPortSecurityEnabled();
        u.hostId = port.getHostId();
        u.vNicType = port.getvNicType();
        if (!(port instanceof NeutronPort)) {
            u.profile = port.getProfile();
        }
        if (port instanceof NeutronPort) {
            NeutronPort np = (NeutronPort) port;
            // binding:profile read from a response is not sent back: recent Neutron policy limits updating it to the service role
            if (np.profileRequested()) {
                u.profile = port.getProfile();
            }
            u.extraDhcpOpts = np.extraDhcpOpts();
            if (np.qosPolicyIdRequested()) {
                u.qosPolicyId = port.getQosPolicyId();
                u.qosPolicyIdSet = true;
            }
        }
        return u;
    }
}
