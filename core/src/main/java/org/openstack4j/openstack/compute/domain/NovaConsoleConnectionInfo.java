package org.openstack4j.openstack.compute.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.compute.ConsoleConnectionInfo;

@JsonRootName("console")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NovaConsoleConnectionInfo implements ConsoleConnectionInfo {

    private static final long serialVersionUID = 1L;

    @JsonProperty("instance_uuid") private String instanceUuid;
    private String host;
    private Integer port;
    @JsonProperty("tls_port") private Integer tlsPort;
    @JsonProperty("internal_access_path") private String internalAccessPath;

    @Override public String getInstanceUuid() { return instanceUuid; }
    @Override public String getHost() { return host; }
    @Override public Integer getPort() { return port; }
    @Override public Integer getTlsPort() { return tlsPort; }
    @Override public String getInternalAccessPath() { return internalAccessPath; }
}
