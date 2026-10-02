package org.openstack4j.openstack.compute.domain;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.node.BinaryNode;
import org.openstack4j.model.compute.*;
import org.openstack4j.model.compute.Server.DiskConfig;
import org.openstack4j.model.compute.builder.ServerCreateBuilder;

@JsonRootName("server")
public class NovaServerCreate implements ServerCreate {

    private static final long serialVersionUID = 1L;

    private String name;
    private String adminPass;
    private String imageRef;
    private String flavorRef;
    private String accessIPv4;
    private String accessIPv6;
    private Integer min;
    private Integer max;
    private DiskConfig diskConfig;
    @JsonProperty("metadata")
    private Map<String, String> metadata;
    @JsonProperty("user_data")
    private String userData;
    @JsonProperty("key_name")
    private String keyName;
    @JsonProperty("availability_zone")
    private String availabilityZone;
    private String host;
    @JsonProperty("hypervisor_hostname")
    private String hypervisorHostName;
    @JsonProperty("config_drive")
    private Boolean configDrive;

    @JsonIgnore
    private transient Map<String, Object> schedulerHints;

    @JsonProperty("security_groups")
    private List<SecurityGroup> securityGroups;

    @JsonProperty("networks")
    private List<NovaNetworkCreate> networks;

    private List<Personality> personality;

    @JsonProperty("block_device_mapping_v2")
    private List<BlockDeviceMappingCreate> blockDeviceMapping;

    @JsonProperty("tags")
    private List<String> tags;
    @JsonProperty("trusted_image_certificates")
    private List<String> trustedImageCertificates;
    @JsonProperty("hostname")
    private String hostname;
    @JsonProperty("description")
    private String description;
    @JsonIgnore
    private String networksMode;

    public static ServerCreateBuilder builder() {
        return new ServerCreateConcreteBuilder();
    }

    @Override
    public ServerCreateBuilder toBuilder() {
        return new ServerCreateConcreteBuilder(this);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getAdminPass() {
        return adminPass;
    }

    @Override
    public String getImageRef() {
        return imageRef;
    }

    @Override
    public String getFlavorRef() {
        return flavorRef;
    }

    @Override
    public String getAccessIPv4() {
        return accessIPv4;
    }

    @Override
    public String getAccessIPv6() {
        return accessIPv6;
    }

    @Override
    public Integer getMin() {
        return min;
    }

    @Override
    public Integer getMax() {
        return max;
    }

    @Override
    public DiskConfig getDiskConfig() {
        return diskConfig;
    }

    @Override
    public String getKeyName() {
        return keyName;
    }

    @Override
    public String getUserData() {
        return userData;
    }

    @JsonIgnore
    @Override
    public Map<String, String> getMetaData() {
        return metadata;
    }

    @Override
    public List<? extends SecurityGroup> getSecurityGroups() {
        return securityGroups;
    }

    @Override
    public String getAvailabilityZone() {
        return availabilityZone;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getHypervisorHostName() {
        return hypervisorHostName;
    }

    @JsonIgnore
    @Override
    public Map<String, Object> getSchedulerHints() {
        return schedulerHints;
    }

    @Override
    @JsonIgnore
    public boolean isConfigDrive() {
        return configDrive != null && configDrive;
    }

    @SuppressWarnings("unchecked")
    @JsonIgnore
    @Override
    public List<? extends NetworkCreate> getNetworks() {
        return (List<? extends NetworkCreate>) (networks != null ? networks: Collections.emptyList());
    }

    @Override
    public List<Personality> getPersonality() {
        return personality;
    }

    @Override
    public void addPersonality(String path, String contents) {
        if (personality == null)
            personality = new ArrayList<>();
        personality.add(new Personality(path, contents));
    }

    @Override
    public void addSecurityGroup(String name) {
        if (securityGroups == null)
            securityGroups = new ArrayList<>();
        securityGroups.add(new SecurityGroupInternal(name));
    }

    @Override
    public void addNetwork(String id, String fixedIP) {
        initNetworks();
        networks.add(new NovaNetworkCreate(id, fixedIP));
    }

    @Override
    public void addNetworkPort(String id) {
        initNetworks();
        networks.add(new NovaNetworkCreate(null, null, id));
    }

    @JsonIgnore
    @Override
    public List<? extends BlockDeviceMappingCreate> getBlockDeviceMapping() {
        return blockDeviceMapping;
    }

    @Override
    public List<String> getTags() {
        return tags;
    }

    @Override
    public List<String> getTrustedImageCertificates() {
        return trustedImageCertificates;
    }

    @Override
    public String getHostname() {
        return hostname;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @JsonIgnore
    @Override
    public String getNetworksMode() {
        return networksMode;
    }

    /** Sends {@code "networks": "auto" | "none"} (2.37+); the networks list is null in that case. */
    @JsonAnyGetter
    Map<String, Object> networksModeProperty() {
        return networksMode == null ? Collections.emptyMap() : Collections.singletonMap("networks", networksMode);
    }

    public void addTaggedNetwork(String id, String tag) {
        initNetworks();
        networks.add(new NovaNetworkCreate(id, null, null, tag));
    }

    private void initNetworks() {
        if (networks == null)
            networks = new ArrayList<>();
    }

    static class SecurityGroupInternal implements SecurityGroup {

        private static final long serialVersionUID = 1L;
        private String name;

        SecurityGroupInternal() {
        }

        SecurityGroupInternal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public static class ServerCreateConcreteBuilder implements ServerCreateBuilder {

        NovaServerCreate m;

        ServerCreateConcreteBuilder() {
            this(new NovaServerCreate());
        }

        ServerCreateConcreteBuilder(NovaServerCreate m) {
            this.m = m;
        }

        @Override
        public ServerCreateBuilder tags(List<String> tags) {
            m.tags = tags;
            return this;
        }

        @Override
        public ServerCreateBuilder trustedImageCertificates(List<String> certificateIds) {
            m.trustedImageCertificates = certificateIds;
            return this;
        }

        @Override
        public ServerCreateBuilder hostname(String hostname) {
            m.hostname = hostname;
            return this;
        }

        @Override
        public ServerCreateBuilder description(String description) {
            m.description = description;
            return this;
        }

        @Override
        public ServerCreateBuilder autoAllocateNetwork() {
            m.networks = null;
            m.networksMode = "auto";
            return this;
        }

        @Override
        public ServerCreateBuilder noNetwork() {
            m.networks = null;
            m.networksMode = "none";
            return this;
        }

        @Override
        public ServerCreateBuilder addTaggedNetwork(String networkId, String tag) {
            m.networksMode = null;
            m.addTaggedNetwork(networkId, tag);
            return this;
        }

        @Override
        public ServerCreateConcreteBuilder name(String name) {
            m.name = name;
            return this;
        }

        @Override
        public ServerCreateConcreteBuilder flavor(String flavorId) {
            m.flavorRef = flavorId;
            return this;
        }

        @Override
        public ServerCreateConcreteBuilder flavor(Flavor flavor) {
            m.flavorRef = flavor.getId();
            return this;
        }

        @Override
        public ServerCreateConcreteBuilder image(String imageId) {
            m.imageRef = imageId;
            return this;
        }

        @Override
        public ServerCreateConcreteBuilder image(Image image) {
            m.imageRef = image.getId();
            return this;
        }

        public ServerCreateConcreteBuilder zone(String availabilityZone) {
            m.availabilityZone = availabilityZone;
            return this;
        }

        @Override
        public ServerCreateConcreteBuilder networks(List<String> idList) {
            if (idList != null) {
                for (String id : idList) {
                    m.addNetwork(id, null);
                }
            }
            return this;
        }

        @Override
        public ServerCreateBuilder addNetworkPort(String portId) {
            m.addNetworkPort(portId);
            return this;
        }


        @Override
        public ServerCreateBuilder addSecurityGroup(String name) {
            if (name != null && !name.isEmpty())
                m.addSecurityGroup(name);
            return this;
        }

        @Override
        public ServerCreateBuilder addPersonality(String path, String contents) {
            if (path == null || contents == null) return this;

            if (m.personality == null)
                m.personality = new ArrayList<>();
            m.personality.add(new Personality(path, new BinaryNode(contents.getBytes()).asText()));
            return this;
        }

        @Override
        public ServerCreateBuilder keypairName(String name) {
            m.keyName = name;
            return this;
        }

        @Override
        public ServerCreateBuilder availabilityZone(String availabilityZone) {
            m.availabilityZone = availabilityZone;
            return this;
        }

        @Override
        public ServerCreateBuilder host(String host) {
            m.host = host;
            return this;
        }

        @Override
        public ServerCreateBuilder hypervisorHostName(String hypervisorHostName) {
            m.hypervisorHostName = hypervisorHostName;
            return this;
        }

        @Override
        public ServerCreate build() {
            return m;
        }

        @Override
        public ServerCreateConcreteBuilder from(ServerCreate in) {
            m = (NovaServerCreate) in;
            return this;
        }


        @Override
        public ServerCreateBuilder blockDevice(BlockDeviceMappingCreate blockDevice) {
            if (blockDevice != null && m.blockDeviceMapping == null) {
                m.blockDeviceMapping = new ArrayList<>();
            }
            m.blockDeviceMapping.add(blockDevice);
            return this;
        }

        @Override
        public ServerCreateBuilder userData(String userData) {
            m.userData = userData;
            return this;
        }

        @Override
        public ServerCreateBuilder addMetadataItem(String key, String value) {
            if (m.metadata == null)
                m.metadata = new HashMap<>();

            m.metadata.put(key, value);
            return this;
        }

        @Override
        public ServerCreateBuilder addMetadata(Map<String, String> metadata) {
            m.metadata = metadata;
            return this;
        }

        @Override
        public ServerCreateBuilder addSchedulerHint(String key, String value) {
            return addSchedulerHintItem(key, value);
        }

        @Override
        public ServerCreateBuilder addSchedulerHint(String key, List<String> value) {
            return addSchedulerHintItem(key, value);
        }

        private ServerCreateBuilder addSchedulerHintItem(String key, Object value) {
            if (m.schedulerHints == null)
                m.schedulerHints = new HashMap<>();

            m.schedulerHints.put(key, value);
            return this;
        }

        @Override
        public ServerCreateBuilder addSchedulerHints(Map<String, Object> schedulerHints) {
            m.schedulerHints = schedulerHints;
            return this;
        }

        @Override
        public ServerCreateBuilder addAdminPass(String adminPass) {
            m.adminPass = adminPass;
            return this;
        }

        @Override
        public ServerCreateBuilder configDrive(boolean configDrive) {
            m.configDrive = configDrive;
            return this;
        }

    }
}
