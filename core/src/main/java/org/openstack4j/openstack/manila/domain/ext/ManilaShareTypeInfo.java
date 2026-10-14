package org.openstack4j.openstack.manila.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.model.manila.ext.ShareTypeInfo;
import org.openstack4j.openstack.common.ListResult;

/**
 * No {@code @JsonRootName}: a show response has two roots ({@code share_type} and {@code volume_type}); see
 * {@link ManilaShareTypeInfoWrapper}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManilaShareTypeInfo implements ShareTypeInfo {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("description") private String description;
    @JsonProperty("share_type_access:is_public")
    @JsonAlias("os-share-type-access:is_public")
    private Boolean isPublic;
    @JsonProperty("is_default") private Boolean isDefault;
    @JsonProperty("extra_specs") private Map<String, String> extraSpecs;
    @JsonProperty("required_extra_specs") private Map<String, String> requiredExtraSpecs;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }
    @Override public Boolean getIsPublic() { return isPublic; }
    @Override public Boolean getIsDefault() { return isDefault; }
    @Override public Map<String, String> getExtraSpecs() { return extraSpecs; }
    @Override public Map<String, String> getRequiredExtraSpecs() { return requiredExtraSpecs; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /** Reads {@code {"share_type": {...}, "volume_type": {...}}}; the deprecated {@code volume_type} is ignored. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ManilaShareTypeInfoWrapper implements ModelEntity {
        private static final long serialVersionUID = 1L;
        @JsonProperty("share_type")
        private ManilaShareTypeInfo shareType;

        public ManilaShareTypeInfo getShareType() {
            return shareType;
        }
    }

    /** {@code volume_types} in the same response is ignored. */
    public static class ManilaShareTypeInfoList extends ListResult<ManilaShareTypeInfo> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("share_types")
        private List<ManilaShareTypeInfo> list;

        @Override
        protected List<ManilaShareTypeInfo> value() {
            return list;
        }
    }
}
