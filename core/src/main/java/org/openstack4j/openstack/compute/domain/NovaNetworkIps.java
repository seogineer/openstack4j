package org.openstack4j.openstack.compute.domain;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import org.openstack4j.openstack.compute.domain.NovaAddresses.NovaAddress;

/** {@code GET /servers/{id}/ips/{label}}: {@code {"<label>": [addresses]}}. */
public class NovaNetworkIps implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, List<NovaAddress>> byLabel = new HashMap<>();

    @JsonAnySetter
    void put(String label, List<NovaAddress> addresses) {
        byLabel.put(label, addresses);
    }

    public List<NovaAddress> get(String label) {
        List<NovaAddress> list = byLabel.get(label);
        return list == null ? Collections.emptyList() : list;
    }
}
