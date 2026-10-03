package org.openstack4j.openstack.identity.v3.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.identity.v3.RoleInference;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneRoleInference implements RoleInference {

    private static final long serialVersionUID = 1L;

    @JsonProperty("prior_role") private KeystoneRole priorRole;
    @JsonProperty("implies") private List<KeystoneRole> implies;

    @Override public KeystoneRole getPriorRole() { return priorRole; }
    @Override public List<KeystoneRole> getImplies() { return implies; }

    /** {@code {"role_inference": {...}, "links": {...}}}: the sibling "links" rules out root unwrapping. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RoleInferenceResponse implements org.openstack4j.model.ModelEntity {
        private static final long serialVersionUID = 1L;
        @JsonProperty("role_inference")
        private KeystoneRoleInference inference;

        public KeystoneRoleInference get() {
            return inference;
        }
    }

    public static class RoleInferences extends ListResult<KeystoneRoleInference> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("role_inferences")
        private List<KeystoneRoleInference> list;

        @Override
        protected List<KeystoneRoleInference> value() {
            return list;
        }
    }
}
