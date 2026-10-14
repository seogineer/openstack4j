package org.openstack4j.openstack.octavia.domain;

import java.util.List;
import java.util.Map;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.util.ToStringHelper;
import org.openstack4j.model.octavia.LbOperatingStatus;
import org.openstack4j.model.octavia.LbProvisioningStatus;
import org.openstack4j.model.octavia.ListenerProtocol;
import org.openstack4j.model.octavia.ListenerV2;
import org.openstack4j.model.octavia.builder.ListenerV2Builder;
import org.openstack4j.openstack.common.ListResult;

/**
 * lbaas v2 listener
 *
 * @author wei
 */
@JsonRootName("listener")
@JsonIgnoreProperties(ignoreUnknown = true)
public class OctaviaListenerV2 implements ListenerV2 {

    @JsonProperty(value = "created_at", access = JsonProperty.Access.WRITE_ONLY)
    private Date createdAt;

    @JsonProperty(value = "updated_at", access = JsonProperty.Access.WRITE_ONLY)
    private Date updatedAt;

    @JsonProperty(value = "insert_headers", access = JsonProperty.Access.WRITE_ONLY)
    private Map<String, String> insertHeaders;

    @JsonProperty(value = "timeout_client_data", access = JsonProperty.Access.WRITE_ONLY)
    private Integer timeoutClientData;

    @JsonProperty(value = "timeout_member_connect", access = JsonProperty.Access.WRITE_ONLY)
    private Integer timeoutMemberConnect;

    @JsonProperty(value = "timeout_member_data", access = JsonProperty.Access.WRITE_ONLY)
    private Integer timeoutMemberData;

    @JsonProperty(value = "timeout_tcp_inspect", access = JsonProperty.Access.WRITE_ONLY)
    private Integer timeoutTcpInspect;
    private static final long serialVersionUID = 1L;

    private String id;

    @JsonProperty("project_id")
    private String projectId;

    private String name;

    private String description;

    /**
     * The protocol of the VIP address. A valid value is TCP, HTTP, or HTTPS
     */
    private ListenerProtocol protocol;

    /**
     * The port on which to listen to client traffic that is associated with the
     * VIP address. A valid value is from 0 to 65535.
     */
    @JsonProperty("protocol_port")
    private Integer protocolPort;

    /**
     * The maximum number of connections allowed for the listener. Default is -1, meaning no limit.
     */
    @JsonProperty("connection_limit")
    private Integer connectionLimit;

    @JsonProperty("default_pool_id")
    private String defaultPoolId;

    @JsonProperty("loadbalancer_id")
    private String loadbalancerId;

    @JsonProperty(value = "loadbalancers", access = JsonProperty.Access.WRITE_ONLY)
    private List<ListItem> loadBalancers;

    @JsonProperty("admin_state_up")
    private boolean adminStateUp = true;

    @JsonProperty("default_tls_container_ref")
    private String defaultTlsContainerRef;

    @JsonProperty("sni_container_refs")
    private List<String> sniContainerRefs;

    @JsonProperty(value = "provisioning_status", access = JsonProperty.Access.WRITE_ONLY)
    private LbProvisioningStatus provisioningStatus;

    @JsonProperty(value = "operating_status", access = JsonProperty.Access.WRITE_ONLY)
    private LbOperatingStatus operatingStatus;

    public static ListenerV2Builder builder() {
        return new ListenerConcreteBuilder();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ListItem> getLoadBalancers() {
        return loadBalancers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAdminStateUp() {
        return adminStateUp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultPoolId() {
        return defaultPoolId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getConnectionLimit() {
        return connectionLimit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getProtocolPort() {
        return protocolPort;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ListenerProtocol getProtocol() {
        return protocol;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProjectId() {
        return projectId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultTlsContainerRef() {
        return defaultTlsContainerRef;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getSniContainerRefs() {
        return sniContainerRefs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return id;
    }

    @Override
    public LbProvisioningStatus getProvisioningStatus() {
        return provisioningStatus;
    }

    @Override
    public LbOperatingStatus getOperatingStatus() {
        return operatingStatus;
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
                .add("id", id)
                .add("adminStateUp", adminStateUp)
                .add("projectId", projectId)
                .add("description", description)
                .add("loadbalancers", loadBalancers)
                .add("name", name)
                .add("protocol", protocol)
                .add("protocolPort", protocolPort)
                .add("connectionLImit", connectionLimit)
                .add("defaultPoolId", defaultPoolId)
                .add("defaultTlsContainerRef", defaultTlsContainerRef)
                .add("sniContainerRefs", sniContainerRefs)
                .toString();
    }

    @Override
    public ListenerV2Builder toBuilder() {
        return new ListenerConcreteBuilder(this);
    }

    public static class ListenerConcreteBuilder implements ListenerV2Builder {
        private OctaviaListenerV2 m;

        public ListenerConcreteBuilder() {
            this(new OctaviaListenerV2());
        }

        public ListenerConcreteBuilder(OctaviaListenerV2 m) {
            this.m = m;
        }

        @Override
        public ListenerV2 build() {
            return m;
        }

        @Override
        public ListenerV2Builder from(ListenerV2 in) {
            m = (OctaviaListenerV2) in;
            return this;
        }

        @Override
        public ListenerV2Builder loadBalancerId(String loadbalancerId) {
            m.loadbalancerId = loadbalancerId;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ListenerV2Builder projectId(String projectId) {
            m.projectId = projectId;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ListenerV2Builder protocol(ListenerProtocol protocol) {
            m.protocol = protocol;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ListenerV2Builder protocolPort(Integer protocolPort) {
            m.protocolPort = protocolPort;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ListenerV2Builder adminStateUp(boolean adminStateUp) {
            m.adminStateUp = adminStateUp;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ListenerV2Builder name(String name) {
            m.name = name;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ListenerV2Builder description(String description) {
            m.description = description;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ListenerV2Builder connectionLimit(Integer connectionLimit) {
            m.connectionLimit = connectionLimit;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ListenerV2Builder sniContainerRefs(List<String> sniContainerRefs) {
            m.sniContainerRefs = sniContainerRefs;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ListenerV2Builder defaultTlsContainerRef(String tlsContainerRef) {
            m.defaultTlsContainerRef = tlsContainerRef;
            return this;
        }
    }

    public static class Listeners extends ListResult<OctaviaListenerV2> {
        private static final long serialVersionUID = 1L;

        @JsonProperty("listeners")
        List<OctaviaListenerV2> listeners;

        /**
         * {@inheritDoc}
         */
        @Override
        protected List<OctaviaListenerV2> value() {
            return listeners;
        }

        @Override
        public String toString() {
            return new ToStringHelper(this)
                    .add("listeners", listeners)
                    .toString();
        }
    }

    @Override
    public Date getCreatedAt() {
        return createdAt;
    }

    @Override
    public Date getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public Map<String, String> getInsertHeaders() {
        return insertHeaders;
    }

    @Override
    public Integer getTimeoutClientData() {
        return timeoutClientData;
    }

    @Override
    public Integer getTimeoutMemberConnect() {
        return timeoutMemberConnect;
    }

    @Override
    public Integer getTimeoutMemberData() {
        return timeoutMemberData;
    }

    @Override
    public Integer getTimeoutTcpInspect() {
        return timeoutTcpInspect;
    }
}
