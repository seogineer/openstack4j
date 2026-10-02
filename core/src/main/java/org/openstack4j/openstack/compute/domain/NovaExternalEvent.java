package org.openstack4j.openstack.compute.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.compute.ExternalEvent;
import org.openstack4j.openstack.common.ListResult;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NovaExternalEvent implements ExternalEvent {

    private static final long serialVersionUID = 1L;

    private String name;
    @JsonProperty("server_uuid") private String serverUuid;
    private String status;
    private String tag;
    private Integer code;

    @Override public String getName() { return name; }
    @Override public String getServerUuid() { return serverUuid; }
    @Override public String getStatus() { return status; }
    @Override public String getTag() { return tag; }
    @Override public Integer getCode() { return code; }

    public static class NovaExternalEvents extends ListResult<NovaExternalEvent> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("events")
        private List<NovaExternalEvent> events;

        @Override
        protected List<NovaExternalEvent> value() {
            return events;
        }
    }
}
