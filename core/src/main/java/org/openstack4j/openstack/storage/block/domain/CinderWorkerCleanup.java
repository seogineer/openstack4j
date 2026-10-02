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
import org.openstack4j.model.storage.block.WorkerCleanup;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderWorkerCleanup implements WorkerCleanup {

    private static final long serialVersionUID = 1L;

    private List<Entry> cleaning;
    private List<Entry> unavailable;

    @Override public List<Entry> getCleaning() { return cleaning == null ? Collections.emptyList() : cleaning; }
    @Override public List<Entry> getUnavailable() { return unavailable == null ? Collections.emptyList() : unavailable; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entry implements CleanedService {
        private static final long serialVersionUID = 1L;
        private String id;
        private String host;
        private String binary;
        @JsonProperty("cluster_name") private String clusterName;

        @Override public String getId() { return id; }
        @Override public String getHost() { return host; }
        @Override public String getBinary() { return binary; }
        @Override public String getClusterName() { return clusterName; }
    }
}
