package org.openstack4j.model.placement.v1;

/** Filters for {@code GET /traits}. */
public final class TraitListOptions {

    private String name;
    private Boolean associated;

    private TraitListOptions() {
    }

    public static TraitListOptions create() {
        return new TraitListOptions();
    }

    /** Raw {@code name} filter, for example {@code startswith:HW_} or {@code in:A,B}. */
    public TraitListOptions name(String filter) {
        this.name = filter;
        return this;
    }

    public TraitListOptions nameStartsWith(String prefix) {
        return name("startswith:" + prefix);
    }

    public TraitListOptions nameIn(String... traits) {
        return name("in:" + String.join(",", traits));
    }

    /** Only traits that are associated with at least one resource provider. */
    public TraitListOptions associated(boolean associated) {
        this.associated = associated;
        return this;
    }

    public String getName() {
        return name;
    }

    public Boolean getAssociated() {
        return associated;
    }
}
