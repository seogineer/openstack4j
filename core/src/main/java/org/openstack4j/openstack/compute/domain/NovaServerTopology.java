package org.openstack4j.openstack.compute.domain;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.compute.ServerTopology;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NovaServerTopology implements ServerTopology {

    private static final long serialVersionUID = 1L;

    @JsonProperty("nodes")
    private List<NovaNode> nodes;
    @JsonProperty("pagesize_kb")
    private Integer pagesizeKb;

    @Override
    public List<NovaNode> getNodes() { return nodes; }

    @Override
    public Integer getPagesizeKb() { return pagesizeKb; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NovaNode implements Node {
        private static final long serialVersionUID = 1L;
        @JsonProperty("cpu_pinning")
        private Map<String, Integer> cpuPinning;
        @JsonProperty("host_node")
        private Integer hostNode;
        @JsonProperty("memory_mb")
        private Integer memoryMb;
        @JsonProperty("siblings")
        private List<List<Integer>> siblings;
        @JsonProperty("vcpu_set")
        private List<Integer> vcpuSet;

        @Override public Map<String, Integer> getCpuPinning() { return cpuPinning; }
        @Override public Integer getHostNode() { return hostNode; }
        @Override public Integer getMemoryMb() { return memoryMb; }
        @Override public List<List<Integer>> getSiblings() { return siblings; }
        @Override public List<Integer> getVcpuSet() { return vcpuSet; }
    }
}
