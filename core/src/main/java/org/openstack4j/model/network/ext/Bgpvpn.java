package org.openstack4j.model.network.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** A BGP VPN (networking-bgpvpn). Fields without a getter are in getAttributes(). */
public interface Bgpvpn extends ModelEntity {
    String getId();
    String getName();
    String getType();
    List<String> getRouteTargets();
    List<String> getImportTargets();
    List<String> getExportTargets();
    List<String> getRouteDistinguishers();
    List<String> getNetworks();
    List<String> getRouters();
    List<String> getPorts();
    Integer getVni();
    Integer getLocalPref();
    String getProjectId();
    /** @return the response fields that have no getter */
    Map<String, Object> getAttributes();
}
