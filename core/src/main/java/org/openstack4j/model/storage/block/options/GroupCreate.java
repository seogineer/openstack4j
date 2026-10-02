package org.openstack4j.model.storage.block.options;

import java.util.List;

/** Body of {@code POST /groups} (3.13+). */
public class GroupCreate {
    private final String name;
    private final String groupType;
    private final List<String> volumeTypes;
    private String description;
    private String availabilityZone;

    private GroupCreate(String name, String groupType, List<String> volumeTypes) { this.name = name; this.groupType = groupType; this.volumeTypes = volumeTypes; }
    public static GroupCreate create(String name, String groupType, List<String> volumeTypes) { return new GroupCreate(name, groupType, volumeTypes); }
    public GroupCreate description(String description) { this.description = description; return this; }
    public GroupCreate availabilityZone(String availabilityZone) { this.availabilityZone = availabilityZone; return this; }
    public String getName() { return name; }
    public String getGroupType() { return groupType; }
    public List<String> getVolumeTypes() { return volumeTypes; }
    public String getDescription() { return description; }
    public String getAvailabilityZone() { return availabilityZone; }
}
