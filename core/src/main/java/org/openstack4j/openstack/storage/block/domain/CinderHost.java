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
import org.openstack4j.model.storage.block.StorageHost;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderHost implements StorageHost {

    private static final long serialVersionUID = 1L;

    @JsonProperty("host_name") private String hostName;
    private String service;
    private String zone;
    @JsonProperty("service-status") private String serviceStatus;
    @JsonProperty("service-state") private String serviceState;
    @JsonProperty("last-update") private Date lastUpdate;

    @Override public String getHostName() { return hostName; }
    @Override public String getService() { return service; }
    @Override public String getZone() { return zone; }
    @Override public String getServiceStatus() { return serviceStatus; }
    @Override public String getServiceState() { return serviceState; }
    @Override public Date getLastUpdate() { return lastUpdate; }

    public static class Hosts extends ListResult<CinderHost> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("hosts")
        private List<CinderHost> items;

        @Override
        protected List<CinderHost> value() {
            return items;
        }
    }
}
