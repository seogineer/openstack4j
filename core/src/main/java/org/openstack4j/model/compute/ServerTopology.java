package org.openstack4j.model.compute;

import java.util.List;
import java.util.Map;

import org.openstack4j.model.ModelEntity;

/** NUMA topology of a server (2.78+). Admin-only fields are {@code null} for other users. */
public interface ServerTopology extends ModelEntity {

    List<? extends Node> getNodes();

    Integer getPagesizeKb();

    interface Node extends ModelEntity {
        Map<String, Integer> getCpuPinning();
        Integer getHostNode();
        Integer getMemoryMb();
        List<List<Integer>> getSiblings();
        List<Integer> getVcpuSet();
    }
}
