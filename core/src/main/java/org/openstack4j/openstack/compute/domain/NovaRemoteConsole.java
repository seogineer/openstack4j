package org.openstack4j.openstack.compute.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.compute.RemoteConsole;

@JsonRootName("remote_console")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NovaRemoteConsole implements RemoteConsole {

    private static final long serialVersionUID = 1L;

    private String protocol;
    private String type;
    private String url;

    public NovaRemoteConsole() {
    }

    public NovaRemoteConsole(String protocol, String type) {
        this.protocol = protocol;
        this.type = type;
    }

    @Override public String getProtocol() { return protocol; }
    @Override public String getType() { return type; }
    @Override public String getUrl() { return url; }
}
