package org.openstack4j.model.compute;

/** One event for {@code POST /os-server-external-events} (normally sent by Neutron, Cinder or Cyborg; admin only). */
public class ExternalEventCreate {

    private final String name;
    private final String serverUuid;
    private String status;
    private String tag;

    private ExternalEventCreate(String name, String serverUuid) {
        this.name = name;
        this.serverUuid = serverUuid;
    }

    /**
     * @param name network-changed, network-vif-plugged, network-vif-unplugged, network-vif-deleted,
     *             volume-extended (2.51+), power-update (2.76+) or accelerator-request-bound (2.82+)
     */
    public static ExternalEventCreate of(String name, String serverUuid) {
        return new ExternalEventCreate(name, serverUuid);
    }

    /** failed, completed (default) or in-progress */
    public ExternalEventCreate status(String status) { this.status = status; return this; }

    public ExternalEventCreate tag(String tag) { this.tag = tag; return this; }

    public String getName() { return name; }
    public String getServerUuid() { return serverUuid; }
    public String getStatus() { return status; }
    public String getTag() { return tag; }
}
