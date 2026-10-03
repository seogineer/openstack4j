package org.openstack4j.model.network.ext;

import org.openstack4j.model.ModelEntity;

/** Usage of one Neutron quota: limit, used and reserved. */
public interface QuotaDetail extends ModelEntity {
    Integer getLimit();
    Integer getUsed();
    Integer getReserved();
}
