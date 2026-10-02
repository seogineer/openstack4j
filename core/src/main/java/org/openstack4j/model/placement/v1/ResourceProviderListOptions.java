package org.openstack4j.model.placement.v1;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Filters for {@code GET /resource_providers}. */
public final class ResourceProviderListOptions {

    private String name;
    private String uuid;
    private String inTree;
    private final List<String> memberOf = new ArrayList<>();
    private final Map<String, Long> resources = new LinkedHashMap<>();
    private final List<String> required = new ArrayList<>();

    private ResourceProviderListOptions() {
    }

    public static ResourceProviderListOptions create() {
        return new ResourceProviderListOptions();
    }

    public ResourceProviderListOptions name(String name) {
        this.name = name;
        return this;
    }

    public ResourceProviderListOptions uuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    /** Providers in the same tree as the given provider. */
    public ResourceProviderListOptions inTree(String providerUuid) {
        this.inTree = providerUuid;
        return this;
    }

    /**
     * Providers in the given aggregate(s). Each call adds one {@code member_of} parameter (AND). A value may be
     * {@code in:a,b} (any of) or start with {@code !} (not in; placement 1.32).
     */
    public ResourceProviderListOptions memberOf(String... aggregateExpressions) {
        for (String expression : aggregateExpressions) memberOf.add(expression);
        return this;
    }

    /** Providers with at least this much free capacity of the resource class. */
    public ResourceProviderListOptions resources(String resourceClass, long amount) {
        resources.put(resourceClass, amount);
        return this;
    }

    /**
     * Required traits. {@code !TRAIT} forbids a trait; {@code in:A,B} requires any of them (placement 1.39).
     * Values given in one call are joined with commas into one {@code required} parameter.
     */
    public ResourceProviderListOptions required(String... traitExpressions) {
        for (String expression : traitExpressions) required.add(expression);
        return this;
    }

    public String getName() {
        return name;
    }

    public String getUuid() {
        return uuid;
    }

    public String getInTree() {
        return inTree;
    }

    public List<String> getMemberOf() {
        return memberOf;
    }

    public Map<String, Long> getResources() {
        return resources;
    }

    public List<String> getRequired() {
        return required;
    }

    /** @return {@code VCPU:2,MEMORY_MB:1024}, or {@code null} when no resources were given */
    public String resourcesParameter() {
        return resources.isEmpty() ? null
                : resources.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue()).collect(Collectors.joining(","));
    }

    /** @return the {@code required} parameter value, or {@code null} when no traits were given */
    public String requiredParameter() {
        return required.isEmpty() ? null : String.join(",", required);
    }

    public boolean usesForbiddenAggregate() {
        return memberOf.stream().anyMatch(m -> m.startsWith("!"));
    }

    public boolean usesTraitInSyntax() {
        return required.stream().anyMatch(t -> t.startsWith("in:"));
    }
}
