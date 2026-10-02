package org.openstack4j.model.placement.v1;

/** Standard Placement resource class names. Custom classes start with {@link #CUSTOM_PREFIX}. */
public final class ResourceClasses {

    public static final String VCPU = "VCPU";
    public static final String PCPU = "PCPU";
    public static final String MEMORY_MB = "MEMORY_MB";
    public static final String DISK_GB = "DISK_GB";
    public static final String PCI_DEVICE = "PCI_DEVICE";
    public static final String SRIOV_NET_VF = "SRIOV_NET_VF";
    public static final String NUMA_SOCKET = "NUMA_SOCKET";
    public static final String NUMA_CORE = "NUMA_CORE";
    public static final String NUMA_THREAD = "NUMA_THREAD";
    public static final String NUMA_MEMORY_MB = "NUMA_MEMORY_MB";
    public static final String IPV4_ADDRESS = "IPV4_ADDRESS";
    public static final String VGPU = "VGPU";
    public static final String VGPU_DISPLAY_HEAD = "VGPU_DISPLAY_HEAD";
    public static final String NET_BW_EGR_KILOBIT_PER_SEC = "NET_BW_EGR_KILOBIT_PER_SEC";
    public static final String NET_BW_IGR_KILOBIT_PER_SEC = "NET_BW_IGR_KILOBIT_PER_SEC";
    public static final String NET_PACKET_RATE_KILOPACKET_PER_SEC = "NET_PACKET_RATE_KILOPACKET_PER_SEC";
    public static final String NET_PACKET_RATE_EGR_KILOPACKET_PER_SEC = "NET_PACKET_RATE_EGR_KILOPACKET_PER_SEC";
    public static final String NET_PACKET_RATE_IGR_KILOPACKET_PER_SEC = "NET_PACKET_RATE_IGR_KILOPACKET_PER_SEC";
    public static final String MEM_ENCRYPTION_CONTEXT = "MEM_ENCRYPTION_CONTEXT";
    public static final String FPGA = "FPGA";
    public static final String PGPU = "PGPU";

    public static final String CUSTOM_PREFIX = "CUSTOM_";

    private ResourceClasses() {
    }

    public static boolean isCustom(String name) {
        return name != null && name.startsWith(CUSTOM_PREFIX);
    }
}
