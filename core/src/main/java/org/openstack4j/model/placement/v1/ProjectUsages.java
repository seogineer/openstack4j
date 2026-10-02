package org.openstack4j.model.placement.v1;

import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** Usage of a project (optionally one user) across all providers. */
public interface ProjectUsages extends ModelEntity {

    /** Key of {@link #getByConsumerType()} on servers older than 1.38, which do not group by consumer type. */
    String ALL_CONSUMER_TYPES = "all";

    /** @return used amount per resource class, summed over consumer types */
    Map<String, Long> getUsages();

    /** @return usage per consumer type (placement 1.38+); one entry {@link #ALL_CONSUMER_TYPES} on older servers */
    Map<String, ? extends ConsumerTypeUsage> getByConsumerType();

    interface ConsumerTypeUsage {
        /** @return number of consumers, or {@code null} on servers older than 1.38 */
        Long getConsumerCount();

        Map<String, Long> getUsages();
    }
}
