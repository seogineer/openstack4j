package org.openstack4j.model.compute.actions;

import java.util.List;
import java.util.Map;

/** Rebuild request for microversion-aware sessions. Personality is not supported here (removed in 2.57). */
public class RebuildRequest {

    private final String imageRef;
    private String name;
    private String adminPass;
    private Map<String, String> metadata;
    private Boolean preserveEphemeral;
    private String accessIPv4;
    private String accessIPv6;
    private String description;
    private String keyName;
    private boolean removeKeyName;
    private String userData;
    private List<String> trustedImageCertificates;
    private String hostname;

    private RebuildRequest(String imageRef) {
        this.imageRef = imageRef;
    }

    public static RebuildRequest create(String imageRef) {
        return new RebuildRequest(imageRef);
    }

    public RebuildRequest name(String name) { this.name = name; return this; }
    public RebuildRequest adminPass(String adminPass) { this.adminPass = adminPass; return this; }
    public RebuildRequest metadata(Map<String, String> metadata) { this.metadata = metadata; return this; }
    public RebuildRequest preserveEphemeral(boolean preserveEphemeral) { this.preserveEphemeral = preserveEphemeral; return this; }
    public RebuildRequest accessIPv4(String accessIPv4) { this.accessIPv4 = accessIPv4; return this; }
    public RebuildRequest accessIPv6(String accessIPv6) { this.accessIPv6 = accessIPv6; return this; }
    /** 2.19+ */
    public RebuildRequest description(String description) { this.description = description; return this; }
    /** 2.54+ */
    public RebuildRequest keyName(String keyName) { this.keyName = keyName; this.removeKeyName = false; return this; }
    /** Removes the key pair ({@code "key_name": null}, 2.54+). */
    public RebuildRequest removeKeyName() { this.keyName = null; this.removeKeyName = true; return this; }
    /** Base64 user data (2.57+). */
    public RebuildRequest userData(String userData) { this.userData = userData; return this; }
    /** 2.63+ */
    public RebuildRequest trustedImageCertificates(List<String> ids) { this.trustedImageCertificates = ids; return this; }
    /** 2.90+ */
    public RebuildRequest hostname(String hostname) { this.hostname = hostname; return this; }

    public String getImageRef() { return imageRef; }
    public String getName() { return name; }
    public String getAdminPass() { return adminPass; }
    public Map<String, String> getMetadata() { return metadata; }
    public Boolean getPreserveEphemeral() { return preserveEphemeral; }
    public String getAccessIPv4() { return accessIPv4; }
    public String getAccessIPv6() { return accessIPv6; }
    public String getDescription() { return description; }
    public String getKeyName() { return keyName; }
    public boolean isRemoveKeyName() { return removeKeyName; }
    public String getUserData() { return userData; }
    public List<String> getTrustedImageCertificates() { return trustedImageCertificates; }
    public String getHostname() { return hostname; }
}
