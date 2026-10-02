package org.openstack4j.model.compute.actions;

public class RescueRequest {

    private String adminPass;
    private String rescueImageRef;

    public static RescueRequest create() {
        return new RescueRequest();
    }

    public RescueRequest adminPass(String adminPass) { this.adminPass = adminPass; return this; }
    public RescueRequest rescueImageRef(String imageRef) { this.rescueImageRef = imageRef; return this; }

    public String getAdminPass() { return adminPass; }
    public String getRescueImageRef() { return rescueImageRef; }
}
