package org.openstack4j.openstack.networking.domain.ext;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.network.ext.ConntrackHelper;
import org.openstack4j.openstack.common.ListResult;

@JsonRootName("conntrack_helper")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeutronConntrackHelper implements ConntrackHelper {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id") private String id;
    @JsonProperty("protocol") private String protocol;
    @JsonProperty("port") private Integer port;
    @JsonProperty("helper") private String helper;

    @Override public String getId() { return id; }
    @Override public String getProtocol() { return protocol; }
    @Override public Integer getPort() { return port; }
    @Override public String getHelper() { return helper; }

    public static class ConntrackHelpers extends ListResult<NeutronConntrackHelper> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("conntrack_helpers")
        private List<NeutronConntrackHelper> list;

        @Override
        protected List<NeutronConntrackHelper> value() {
            return list;
        }
    }
}
