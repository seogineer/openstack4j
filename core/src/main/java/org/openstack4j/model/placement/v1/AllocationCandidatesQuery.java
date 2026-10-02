package org.openstack4j.model.placement.v1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Query of {@code GET /allocation_candidates}. The unsuffixed group is set directly on the builder; granular
 * groups (placement 1.25+) are added with {@link Builder#group(String, Consumer)}.
 */
public final class AllocationCandidatesQuery {

    public enum GroupPolicy {
        NONE, ISOLATE;

        public String parameterValue() {
            return name().toLowerCase();
        }
    }

    /** One request group: resources plus optional trait, aggregate and tree constraints. */
    public static final class RequestGroup {
        private final Map<String, Long> resources = new LinkedHashMap<>();
        private final List<String> required = new ArrayList<>();
        private final List<String> memberOf = new ArrayList<>();
        private String inTree;

        public RequestGroup resources(String resourceClass, long amount) {
            resources.put(resourceClass, amount);
            return this;
        }

        /** {@code !TRAIT} forbids, {@code in:A,B} requires any (placement 1.39). */
        public RequestGroup required(String... traitExpressions) {
            Collections.addAll(required, traitExpressions);
            return this;
        }

        /** Each call adds one {@code member_of} parameter; {@code !uuid} forbids (placement 1.32). */
        public RequestGroup memberOf(String... aggregateExpressions) {
            Collections.addAll(memberOf, aggregateExpressions);
            return this;
        }

        /** Restrict the group to the tree of this provider (placement 1.31). */
        public RequestGroup inTree(String providerUuid) {
            this.inTree = providerUuid;
            return this;
        }

        public Map<String, Long> getResources() {
            return Collections.unmodifiableMap(resources);
        }

        public List<String> getRequired() {
            return Collections.unmodifiableList(required);
        }

        public List<String> getMemberOf() {
            return Collections.unmodifiableList(memberOf);
        }

        public String getInTree() {
            return inTree;
        }

        public String resourcesParameter() {
            return resources.isEmpty() ? null
                    : resources.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue()).collect(Collectors.joining(","));
        }

        public String requiredParameter() {
            return required.isEmpty() ? null : String.join(",", required);
        }
    }

    private final Map<String, RequestGroup> groups;
    private final GroupPolicy groupPolicy;
    private final Integer limit;
    private final List<String> rootRequired;
    private final List<String> sameSubtree;

    private AllocationCandidatesQuery(Builder b) {
        this.groups = Collections.unmodifiableMap(new LinkedHashMap<>(b.groups));
        this.groupPolicy = b.groupPolicy;
        this.limit = b.limit;
        this.rootRequired = Collections.unmodifiableList(new ArrayList<>(b.rootRequired));
        this.sameSubtree = Collections.unmodifiableList(new ArrayList<>(b.sameSubtree));
    }

    public static Builder builder() {
        return new Builder();
    }

    /** @return request groups keyed by suffix; the unsuffixed group has key {@code ""} */
    public Map<String, RequestGroup> getGroups() {
        return groups;
    }

    public GroupPolicy getGroupPolicy() {
        return groupPolicy;
    }

    public Integer getLimit() {
        return limit;
    }

    public List<String> getRootRequired() {
        return rootRequired;
    }

    public List<String> getSameSubtree() {
        return sameSubtree;
    }

    public static final class Builder {
        private final Map<String, RequestGroup> groups = new LinkedHashMap<>();
        private GroupPolicy groupPolicy;
        private Integer limit;
        private final List<String> rootRequired = new ArrayList<>();
        private final List<String> sameSubtree = new ArrayList<>();

        private RequestGroup unsuffixed() {
            return groups.computeIfAbsent("", k -> new RequestGroup());
        }

        public Builder resources(String resourceClass, long amount) {
            unsuffixed().resources(resourceClass, amount);
            return this;
        }

        public Builder required(String... traitExpressions) {
            unsuffixed().required(traitExpressions);
            return this;
        }

        public Builder memberOf(String... aggregateExpressions) {
            unsuffixed().memberOf(aggregateExpressions);
            return this;
        }

        public Builder inTree(String providerUuid) {
            unsuffixed().inTree(providerUuid);
            return this;
        }

        /**
         * Adds a granular request group. Suffixes that are not plain numbers need placement 1.33.
         */
        public Builder group(String suffix, Consumer<RequestGroup> configure) {
            if (suffix == null || suffix.isEmpty())
                throw new IllegalArgumentException("a granular group needs a non-empty suffix");
            RequestGroup group = groups.computeIfAbsent(suffix, k -> new RequestGroup());
            configure.accept(group);
            return this;
        }

        /** Required when more than one granular group is present. */
        public Builder groupPolicy(GroupPolicy groupPolicy) {
            this.groupPolicy = groupPolicy;
            return this;
        }

        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        /** Traits the root provider must (or, with {@code !}, must not) have (placement 1.35). */
        public Builder rootRequired(String... traitExpressions) {
            Collections.addAll(rootRequired, traitExpressions);
            return this;
        }

        /** Group suffixes whose providers must share a subtree (placement 1.36). */
        public Builder sameSubtree(String... suffixes) {
            Collections.addAll(sameSubtree, suffixes);
            return this;
        }

        public AllocationCandidatesQuery build() {
            boolean anyResources = groups.values().stream().anyMatch(g -> !g.resources.isEmpty());
            if (!anyResources)
                throw new IllegalArgumentException("an allocation candidates query needs at least one resources entry");
            long granular = groups.keySet().stream().filter(k -> !k.isEmpty()).count();
            if (granular > 1 && groupPolicy == null)
                throw new IllegalArgumentException("group_policy is required when more than one granular group is requested");
            return new AllocationCandidatesQuery(this);
        }
    }
}
