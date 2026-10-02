package org.openstack4j.model.storage.block;

import java.util.Map;

/** A backend snapshot Cinder could manage ({@code GET /manageable_snapshots[/detail]}, 3.8+). */
public interface ManageableSnapshot extends ManageableVolume {
    /** @return the reference of the snapshot's source volume */
    Map<String, String> getSourceReference();
}
