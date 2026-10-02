package org.openstack4j.openstack.compute.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.compute.ServerShare;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("share")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NovaServerShare implements ServerShare {

    private static final long serialVersionUID = 1L;

    @JsonProperty("share_id") private String shareId;
    private String status;
    private String tag;
    @JsonProperty("export_location") private String exportLocation;
    private String uuid;

    @Override public String getShareId() { return shareId; }
    @Override public String getStatus() { return status; }
    @Override public String getTag() { return tag; }
    @Override public String getExportLocation() { return exportLocation; }
    @Override public String getUuid() { return uuid; }

    public static class NovaServerShares extends ListResult<NovaServerShare> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("shares")
        private List<NovaServerShare> shares;

        @Override
        protected List<NovaServerShare> value() {
            return shares;
        }
    }
}
