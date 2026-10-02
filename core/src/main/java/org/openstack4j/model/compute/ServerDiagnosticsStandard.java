package org.openstack4j.model.compute;

import java.util.List;

import org.openstack4j.model.ModelEntity;

/** Standardised server diagnostics (2.48+). */
public interface ServerDiagnosticsStandard extends ModelEntity {

    String getState();
    String getDriver();
    String getHypervisor();
    String getHypervisorOs();
    Long getUptime();
    Boolean getConfigDrive();
    Integer getNumCpus();
    Integer getNumNics();
    Integer getNumDisks();
    List<? extends CpuDetail> getCpuDetails();
    List<? extends DiskDetail> getDiskDetails();
    List<? extends NicDetail> getNicDetails();
    MemoryDetails getMemoryDetails();

    interface CpuDetail extends ModelEntity {
        Integer getId();
        Long getTime();
        Integer getUtilisation();
    }

    interface DiskDetail extends ModelEntity {
        Long getReadBytes();
        Long getReadRequests();
        Long getWriteBytes();
        Long getWriteRequests();
        Long getErrorsCount();
    }

    interface NicDetail extends ModelEntity {
        String getMacAddress();
        Long getRxOctets();
        Long getRxErrors();
        Long getRxDrop();
        Long getRxPackets();
        Long getRxRate();
        Long getTxOctets();
        Long getTxErrors();
        Long getTxDrop();
        Long getTxPackets();
        Long getTxRate();
    }

    interface MemoryDetails extends ModelEntity {
        Long getMaximum();
        Long getUsed();
    }
}
