package org.openstack4j.openstack.identity.v3.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.identity.v3.LimitModel;

@JsonRootName("model")
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeystoneLimitModel implements LimitModel {

    private static final long serialVersionUID = 1L;

    private String name;
    private String description;

    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
}
