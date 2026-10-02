package org.openstack4j.openstack.storage.block.domain;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.openstack.common.ListResult;
import org.openstack4j.model.storage.block.QosAssociation;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderQosAssociation implements QosAssociation {

    private static final long serialVersionUID = 1L;

    @JsonProperty("association_type") private String associationType;
    private String name;
    private String id;

    @Override public String getAssociationType() { return associationType; }
    @Override public String getName() { return name; }
    @Override public String getId() { return id; }

    public static class QosAssociations extends ListResult<CinderQosAssociation> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("qos_associations")
        private List<CinderQosAssociation> associations;

        @Override
        protected List<CinderQosAssociation> value() {
            return associations;
        }
    }
}
