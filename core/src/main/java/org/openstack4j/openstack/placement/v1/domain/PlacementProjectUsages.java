package org.openstack4j.openstack.placement.v1.domain;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.openstack4j.model.placement.v1.ProjectUsages;

/**
 * Reads both shapes of {@code GET /usages}: {@code {"usages": {"VCPU": 3}}} (before 1.38) and
 * {@code {"usages": {"INSTANCE": {"VCPU": 3, "consumer_count": 1}}}} (1.38 and later).
 */
public class PlacementProjectUsages implements ProjectUsages {

    private static final long serialVersionUID = 1L;

    private final Map<String, Long> usages = new LinkedHashMap<>();
    private final Map<String, Usage> byConsumerType = new LinkedHashMap<>();

    @JsonCreator
    public PlacementProjectUsages(@JsonProperty("usages") JsonNode node) {
        if (node == null || !node.isObject())
            return;
        boolean grouped = false;
        for (Iterator<JsonNode> it = node.elements(); it.hasNext(); )
            if (it.next().isObject()) grouped = true;
        if (grouped) {
            node.fields().forEachRemaining(group -> {
                Usage usage = new Usage();
                group.getValue().fields().forEachRemaining(field -> {
                    if ("consumer_count".equals(field.getKey())) usage.consumerCount = field.getValue().asLong();
                    else usage.usages.put(field.getKey(), field.getValue().asLong());
                });
                byConsumerType.put(group.getKey(), usage);
                usage.usages.forEach((rc, amount) -> usages.merge(rc, amount, Long::sum));
            });
        } else {
            Usage usage = new Usage();
            node.fields().forEachRemaining(field -> usage.usages.put(field.getKey(), field.getValue().asLong()));
            byConsumerType.put(ALL_CONSUMER_TYPES, usage);
            usages.putAll(usage.usages);
        }
    }

    @Override
    public Map<String, Long> getUsages() {
        return Collections.unmodifiableMap(usages);
    }

    @Override
    public Map<String, ? extends ConsumerTypeUsage> getByConsumerType() {
        return Collections.unmodifiableMap(byConsumerType);
    }

    public static class Usage implements ConsumerTypeUsage, java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private Long consumerCount;
        private final Map<String, Long> usages = new LinkedHashMap<>();

        @Override
        public Long getConsumerCount() {
            return consumerCount;
        }

        @Override
        public Map<String, Long> getUsages() {
            return Collections.unmodifiableMap(usages);
        }
    }
}
