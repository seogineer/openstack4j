package org.openstack4j.openstack.compute.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.compute.ServerDiagnosticsStandard;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NovaServerDiagnosticsStandard implements ServerDiagnosticsStandard {

    private static final long serialVersionUID = 1L;

    private String state;
    private String driver;
    private String hypervisor;
    @JsonProperty("hypervisor_os")
    private String hypervisorOs;
    private Long uptime;
    @JsonProperty("config_drive")
    private Boolean configDrive;
    @JsonProperty("num_cpus")
    private Integer numCpus;
    @JsonProperty("num_nics")
    private Integer numNics;
    @JsonProperty("num_disks")
    private Integer numDisks;
    @JsonProperty("cpu_details")
    private List<Cpu> cpuDetails;
    @JsonProperty("disk_details")
    private List<Disk> diskDetails;
    @JsonProperty("nic_details")
    private List<Nic> nicDetails;
    @JsonProperty("memory_details")
    private Memory memoryDetails;

    @Override public String getState() { return state; }
    @Override public String getDriver() { return driver; }
    @Override public String getHypervisor() { return hypervisor; }
    @Override public String getHypervisorOs() { return hypervisorOs; }
    @Override public Long getUptime() { return uptime; }
    @Override public Boolean getConfigDrive() { return configDrive; }
    @Override public Integer getNumCpus() { return numCpus; }
    @Override public Integer getNumNics() { return numNics; }
    @Override public Integer getNumDisks() { return numDisks; }
    @Override public List<Cpu> getCpuDetails() { return cpuDetails; }
    @Override public List<Disk> getDiskDetails() { return diskDetails; }
    @Override public List<Nic> getNicDetails() { return nicDetails; }
    @Override public Memory getMemoryDetails() { return memoryDetails; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Cpu implements CpuDetail {
        private static final long serialVersionUID = 1L;
        private Integer id;
        private Long time;
        private Integer utilisation;
        @Override public Integer getId() { return id; }
        @Override public Long getTime() { return time; }
        @Override public Integer getUtilisation() { return utilisation; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Disk implements DiskDetail {
        private static final long serialVersionUID = 1L;
        @JsonProperty("read_bytes") private Long readBytes;
        @JsonProperty("read_requests") private Long readRequests;
        @JsonProperty("write_bytes") private Long writeBytes;
        @JsonProperty("write_requests") private Long writeRequests;
        @JsonProperty("errors_count") private Long errorsCount;
        @Override public Long getReadBytes() { return readBytes; }
        @Override public Long getReadRequests() { return readRequests; }
        @Override public Long getWriteBytes() { return writeBytes; }
        @Override public Long getWriteRequests() { return writeRequests; }
        @Override public Long getErrorsCount() { return errorsCount; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Nic implements NicDetail {
        private static final long serialVersionUID = 1L;
        @JsonProperty("mac_address") private String macAddress;
        @JsonProperty("rx_octets") private Long rxOctets;
        @JsonProperty("rx_errors") private Long rxErrors;
        @JsonProperty("rx_drop") private Long rxDrop;
        @JsonProperty("rx_packets") private Long rxPackets;
        @JsonProperty("rx_rate") private Long rxRate;
        @JsonProperty("tx_octets") private Long txOctets;
        @JsonProperty("tx_errors") private Long txErrors;
        @JsonProperty("tx_drop") private Long txDrop;
        @JsonProperty("tx_packets") private Long txPackets;
        @JsonProperty("tx_rate") private Long txRate;
        @Override public String getMacAddress() { return macAddress; }
        @Override public Long getRxOctets() { return rxOctets; }
        @Override public Long getRxErrors() { return rxErrors; }
        @Override public Long getRxDrop() { return rxDrop; }
        @Override public Long getRxPackets() { return rxPackets; }
        @Override public Long getRxRate() { return rxRate; }
        @Override public Long getTxOctets() { return txOctets; }
        @Override public Long getTxErrors() { return txErrors; }
        @Override public Long getTxDrop() { return txDrop; }
        @Override public Long getTxPackets() { return txPackets; }
        @Override public Long getTxRate() { return txRate; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Memory implements MemoryDetails {
        private static final long serialVersionUID = 1L;
        private Long maximum;
        private Long used;
        @Override public Long getMaximum() { return maximum; }
        @Override public Long getUsed() { return used; }
    }
}
