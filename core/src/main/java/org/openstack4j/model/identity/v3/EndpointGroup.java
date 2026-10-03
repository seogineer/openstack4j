package org.openstack4j.model.identity.v3;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** An endpoint group (OS-EP-FILTER): a filter that selects catalog endpoints. */
public interface EndpointGroup extends ModelEntity {
    String getId();
    String getName();
    String getDescription();
    /** @return the filters, for example {@code {"interface": "public", "service_id": "..."}} */
    Map<String, Object> getFilters();
}
