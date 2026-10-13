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
import org.openstack4j.openstack.common.ListEntity;

/**
 * Represents a Port that is used during a create operation which only encapsulates the allowed properties
 *
 * @author Jeremy Unruh
 */
@JsonRootName("port")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronPortCreate implements ModelEntity {

    private static final long serialVersionUID = 1L;

    @JsonProperty
    private String name;

    @JsonProperty("admin_state_up")
    private boolean adminStateUp;

    @JsonProperty("fixed_ips")
    private Set<NeutronIP> fixedIps;

    @JsonProperty("mac_address")
    private String macAddress;
    
    @JsonProperty("allowed_address_pairs")
    private Set<NeutronAllowedAddressPair> allowedAddressPairs;
    
    @JsonProperty("network_id")
    private String networkId;

    @JsonProperty("tenant_id")
    private String tenantId;

    @JsonProperty("device_id")
    private String deviceId;

    @JsonProperty("device_owner")
    private String deviceOwner;

    @JsonProperty("security_groups")
    private List<String> securityGroups;

    @JsonProperty("port_security_enabled")
    private Boolean portSecurityEnabled;

    @JsonProperty("binding:host_id")
    private String hostId;

    @JsonProperty("binding:vif_type")
    private String vifType;

    @JsonProperty("binding:vif_details")
    private Map<String, Object> vifDetails;

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


    public NeutronPortCreate() {
    }

    /**
     * Creates a Port Create object which only encapsulates allowable fields from a port
     *
     * @param port the port (source)
     * @return the port create object
     */
    @SuppressWarnings("unchecked")
    public static NeutronPortCreate fromPort(Port port) {
        NeutronPortCreate c = new NeutronPortCreate();
        c.name = port.getName();
        c.networkId = port.getNetworkId();
        c.adminStateUp = port.isAdminStateUp();
        c.macAddress = port.getMacAddress();
        c.tenantId = port.getTenantId();
        c.deviceId = port.getDeviceId();
        c.deviceOwner = port.getDeviceOwner();
        c.securityGroups = port.getSecurityGroups();
        c.fixedIps = (Set<NeutronIP>) port.getFixedIps();
        c.allowedAddressPairs = (Set<NeutronAllowedAddressPair>) port.getAllowedAddressPairs();
        c.portSecurityEnabled = port.isPortSecurityEnabled();
        c.hostId = port.getHostId();
        c.vifType = port.getVifType();
        c.vifDetails = port.getVifDetails();
        c.vNicType = port.getvNicType();
        c.profile = port.getProfile();
        if (port instanceof NeutronPort && ((NeutronPort) port).qosPolicyIdRequested()) {
            c.qosPolicyId = port.getQosPolicyId();
            c.qosPolicyIdSet = true;
        }


        return c;
    }

    public static class NeutronPortsCreate implements ModelEntity {

        private static final long serialVersionUID = 1L;

        @JsonProperty("ports")
        private ListEntity<NeutronPortCreate> ports;

        public NeutronPortsCreate() {
            ports = new ListEntity<>();
        }

        /**
         * Creates a List of Port Create objects which only encapsulates allowable fields from a port
         *
         * @param ports the ports (source)
         * @return the port create objects
         */
        @SuppressWarnings("unchecked")
        public static NeutronPortsCreate fromPorts(List<? extends Port> ports) {
            NeutronPortsCreate c = new NeutronPortsCreate();
            for (Port port : ports) {
                c.ports.add(NeutronPortCreate.fromPort(port));
            }
            return c;
        }

    }
}
