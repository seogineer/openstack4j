package org.openstack4j.openstack.networking.domain;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.util.ToStringHelper;
import org.openstack4j.model.network.NetworkUpdate;
import org.openstack4j.model.network.builder.NetworkUpdateBuilder;

/**
 * An entity used to update a network
 *
 * @author Jeremy Unruh
 */
@JsonRootName("network")
public class NeutronNetworkUpdate implements NetworkUpdate {

    private static final long serialVersionUID = 1L;

    @JsonProperty
    private String name;
    @JsonProperty("shared")
    private Boolean shared;
    @JsonProperty("admin_state_up")
    private Boolean adminStateUp;
    @JsonProperty("is_default")
    private Boolean isDefault;
    @JsonIgnore
    private String qosPolicyId;
    @JsonIgnore
    private boolean qosPolicyIdSet;

    public static NetworkUpdateBuilder builder() {
        return new NetworkUpdateConcreteBuilder();
    }

    @Override
    public NetworkUpdateBuilder toBuilder() {
        return new NetworkUpdateConcreteBuilder(this);
    }

    @Override
    public String getName() {
        return name;
    }

    @JsonIgnore
    @Override
    public boolean isAdminStateUp() {
        return adminStateUp == null ? false: adminStateUp;
    }

    @JsonIgnore
    @Override
    public boolean isShared() {
        return shared == null ? false: shared;
    }

    @JsonIgnore
    @Override
    public boolean isDefault() {
        return isDefault == null ? false: isDefault;
    }


    @JsonIgnore
    @Override
    public String getQosPolicyId() {
        return qosPolicyId;
    }

    /** Request-only keys set through the builder; an explicit null is sent as JSON null. */
    @JsonAnyGetter
    @JsonInclude(content = JsonInclude.Include.ALWAYS)
    Map<String, Object> requestOnlyFields() {
        return qosPolicyIdSet ? Collections.singletonMap("qos_policy_id", qosPolicyId) : Collections.emptyMap();
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
                .add("name", name).add("adminStateUp", adminStateUp).add("shared", shared).add("isDefault", isDefault).add("qosPolicyId", qosPolicyId)
                .toString();
    }

    public static class NetworkUpdateConcreteBuilder implements NetworkUpdateBuilder {

        private NeutronNetworkUpdate model;

        public NetworkUpdateConcreteBuilder() {
            this.model = new NeutronNetworkUpdate();
        }

        public NetworkUpdateConcreteBuilder(NeutronNetworkUpdate model) {
            this.model = model;
        }

        @Override
        public NetworkUpdate build() {
            return model;
        }

        @Override
        public NetworkUpdateBuilder from(NetworkUpdate in) {
            model = (NeutronNetworkUpdate) in;
            return this;
        }

        @Override
        public NetworkUpdateBuilder name(String name) {
            model.name = name;
            return this;
        }

        @Override
        public NetworkUpdateBuilder adminStateUp(boolean enabled) {
            model.adminStateUp = enabled;
            return this;
        }

        @Override
        public NetworkUpdateBuilder shared(boolean shared) {
            model.shared = shared;
            return this;
        }

        @Override
        public NetworkUpdateBuilder qosPolicyId(String qosPolicyId) {
            model.qosPolicyId = qosPolicyId;
            model.qosPolicyIdSet = true;
            return this;
        }

        @Override
        public NetworkUpdateBuilder isDefault(boolean isDefault) {
            model.isDefault = isDefault;
            return this;
        }

    }
}
