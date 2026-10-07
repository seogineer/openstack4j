package org.openstack4j.openstack.baremetal.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.api.baremetal.ConductorService;
import org.openstack4j.model.baremetal.Conductor;
import org.openstack4j.openstack.baremetal.domain.IronicConductor;
import org.openstack4j.openstack.baremetal.domain.IronicConductor.IronicConductorList;

public class ConductorServiceImpl extends BaseBaremetalServices implements ConductorService {

    @Override
    public List<? extends Conductor> list() {
        return list(null);
    }

    @Override
    public List<? extends Conductor> list(Map<String, String> filters) {
        Map<String, String> query = filters == null ? new HashMap<>() : new HashMap<>(filters);
        query.putIfAbsent("detail", "true");
        return listOf(IronicConductorList.class, "/conductors", query);
    }

    @Override
    public Conductor get(String hostname) {
        return show(IronicConductor.class, "/conductors/" + id(hostname));
    }

    @Override
    public Map<String, Integer> shards() {
        Map<String, Integer> shards = new LinkedHashMap<>();
        for (Shard shard : showStrict(Shards.class, "/shards").shards)
            shards.put(shard.name, shard.count);
        return shards;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class Shards {
        @JsonProperty("shards")
        private List<Shard> shards = Collections.emptyList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class Shard {
        @JsonProperty("name")
        private String name;
        @JsonProperty("count")
        private Integer count;
    }
}
