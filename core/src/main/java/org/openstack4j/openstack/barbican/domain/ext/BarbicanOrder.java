package org.openstack4j.openstack.barbican.domain.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.barbican.ext.Order;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BarbicanOrder implements Order {

    private static final long serialVersionUID = 1L;

    @JsonProperty("order_ref") private String orderRef;
    @JsonProperty("type") private String type;
    @JsonProperty("status") private String status;
    @JsonProperty("meta") private Map<String, Object> meta;
    @JsonProperty("secret_ref") private String secretRef;
    @JsonProperty("container_ref") private String containerRef;
    @JsonProperty("error_status_code") private String errorStatusCode;
    @JsonProperty("error_reason") private String errorReason;
    @JsonProperty("creator_id") private String creatorId;
    @JsonProperty("created") private String created;
    @JsonProperty("updated") private String updated;

    @JsonIgnore
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    @Override public String getOrderRef() { return orderRef; }
    @Override public String getType() { return type; }
    @Override public String getStatus() { return status; }
    @Override public Map<String, Object> getMeta() { return meta; }
    @Override public String getSecretRef() { return secretRef; }
    @Override public String getContainerRef() { return containerRef; }
    @Override public String getErrorStatusCode() { return errorStatusCode; }
    @Override public String getErrorReason() { return errorReason; }
    @Override public String getCreatorId() { return creatorId; }
    @Override public String getCreated() { return created; }
    @Override public String getUpdated() { return updated; }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class BarbicanOrderList extends ListResult<BarbicanOrder> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("orders")
        private List<BarbicanOrder> list;

        @Override
        protected List<BarbicanOrder> value() {
            return list;
        }
    }
}
