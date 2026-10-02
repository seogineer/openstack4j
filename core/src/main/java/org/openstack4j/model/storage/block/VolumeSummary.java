package org.openstack4j.model.storage.block;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** {@code GET /volumes/summary} (3.12+). */
public interface VolumeSummary extends ModelEntity {
    Long getTotalCount();
    Long getTotalSize();
    /** @return metadata key to its distinct values (3.36+) */
    Map<String, List<String>> getMetadata();
}
