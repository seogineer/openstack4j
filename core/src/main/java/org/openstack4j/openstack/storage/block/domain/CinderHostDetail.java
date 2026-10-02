package org.openstack4j.openstack.storage.block.domain;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.openstack.common.ListResult;
import java.util.ArrayList;

import org.openstack4j.model.storage.block.StorageHostResource;

/** {@code {"host": [{"resource": {...}}, ...]}} from {@code GET /os-hosts/{host}}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderHostDetail implements ModelEntity {

    private static final long serialVersionUID = 1L;

    @JsonProperty("host")
    private List<Entry> host;

    /** @return the resources, flattened */
    public List<Resource> resources() {
        List<Resource> out = new ArrayList<>();
        if (host != null)
            for (Entry e : host)
                if (e.resource != null)
                    out.add(e.resource);
        return out;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entry implements ModelEntity {
        private static final long serialVersionUID = 1L;
        @JsonProperty("resource")
        public Resource resource;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Resource implements StorageHostResource {
        private static final long serialVersionUID = 1L;
        private String project;
        private String host;
        @JsonProperty("volume_count") private String volumeCount;
        @JsonProperty("total_volume_gb") private String totalVolumeGb;
        @JsonProperty("snapshot_count") private String snapshotCount;
        @JsonProperty("total_snapshot_gb") private String totalSnapshotGb;

        @Override public String getProject() { return project; }
        @Override public String getHost() { return host; }
        @Override public String getVolumeCount() { return volumeCount; }
        @Override public String getTotalVolumeGb() { return totalVolumeGb; }
        @Override public String getSnapshotCount() { return snapshotCount; }
        @Override public String getTotalSnapshotGb() { return totalSnapshotGb; }
    }
}
