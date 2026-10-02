package org.openstack4j.model.compute.actions;

/** Unshelve request with a destination (2.77+). */
public class UnshelveRequest {

    private String availabilityZone;
    private boolean unpinAvailabilityZone;
    private String host;

    public static UnshelveRequest create() {
        return new UnshelveRequest();
    }

    /** 2.77+ */
    public UnshelveRequest availabilityZone(String availabilityZone) { this.availabilityZone = availabilityZone; this.unpinAvailabilityZone = false; return this; }
    /** {@code "availability_zone": null} (2.91+). */
    public UnshelveRequest unpinAvailabilityZone() { this.availabilityZone = null; this.unpinAvailabilityZone = true; return this; }
    /** 2.91+ */
    public UnshelveRequest host(String host) { this.host = host; return this; }

    public String getAvailabilityZone() { return availabilityZone; }
    public boolean isUnpinAvailabilityZone() { return unpinAvailabilityZone; }
    public String getHost() { return host; }
}
