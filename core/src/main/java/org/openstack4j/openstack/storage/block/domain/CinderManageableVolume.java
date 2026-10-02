package org.openstack4j.openstack.storage.block.domain;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.openstack.common.ListResult;
import org.openstack4j.model.storage.block.ManageableVolume;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderManageableVolume implements ManageableVolume {

    private static final long serialVersionUID = 1L;

    private Map<String, String> reference;
    private Integer size;
    @JsonProperty("safe_to_manage") private Boolean safeToManage;
    @JsonProperty("reason_not_safe") private String reasonNotSafe;
    @JsonProperty("cinder_id") private String cinderId;
    @JsonProperty("extra_info") private Map<String, Object> extraInfo;

    @Override public Map<String, String> getReference() { return reference; }
    @Override public Integer getSize() { return size; }
    @Override public Boolean getSafeToManage() { return safeToManage; }
    @Override public String getReasonNotSafe() { return reasonNotSafe; }
    @Override public String getCinderId() { return cinderId; }
    @Override public Map<String, Object> getExtraInfo() { return extraInfo; }

    public static class ManageableVolumes extends ListResult<CinderManageableVolume> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("manageable-volumes")
        private List<CinderManageableVolume> items;

        @Override
        protected List<CinderManageableVolume> value() {
            return items;
        }
    }
}
