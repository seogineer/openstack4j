package org.openstack4j.model.storage.block.options;

import org.openstack4j.model.image.ContainerFormat;
import org.openstack4j.model.image.DiskFormat;

/**
 * Options to Upload volume to image service as image
 *
 * @author Jeremy Unruh
 */
public class UploadImageData {

    private String imageName;
    private ContainerFormat containerFormat = ContainerFormat.BARE;
    private DiskFormat diskFormat = DiskFormat.RAW;
    private boolean force;
    private String visibility;
    private Boolean protectedImage;

    private UploadImageData(String imageName) {
        this.imageName = imageName;
    }

    /**
     * Creates new UploadImageOptions with the specified image name
     *
     * @param imageName the image name for upload
     * @return UploadImageOptions
     */
    public static UploadImageData create(String imageName) {
        return new UploadImageData(imageName);
    }

    /**
     * The container format for this volume image (default is BARE)
     *
     * @param containerFormat the container format
     * @return UploadImageOptions
     */
    public UploadImageData containerFormat(ContainerFormat containerFormat) {
        this.containerFormat = containerFormat;
        return this;
    }

    /**
     * The disk format for this volume image (default is RAW)
     *
     * @param diskFormat the disk format
     * @return UploadImageOptions
     */
    public UploadImageData diskFormat(DiskFormat diskFormat) {
        this.diskFormat = diskFormat;
        return this;
    }

    public UploadImageData force(boolean isForce) {
        this.force = isForce;
        return this;
    }

    /**
     * The image name used as a display name for the upload
     *
     * @param imageName the image name
     * @return UploadImageOptions
     */
    public UploadImageData imageName(String imageName) {
        this.imageName = imageName;
        return this;
    }

    /** Image visibility such as {@code private} or {@code public} (3.1+). */
    public UploadImageData visibility(String visibility) {
        this.visibility = visibility;
        return this;
    }

    /** Whether the image is protected from deletion (3.1+). */
    public UploadImageData protectedImage(boolean protectedImage) {
        this.protectedImage = protectedImage;
        return this;
    }

    public String getVisibility() {
        return visibility;
    }

    public Boolean getProtectedImage() {
        return protectedImage;
    }

    public String getImageName() {
        return imageName;
    }

    public ContainerFormat getContainerFormat() {
        return containerFormat;
    }

    public DiskFormat getDiskFormat() {
        return diskFormat;
    }

    public boolean isForce() {
        return force;
    }


}
