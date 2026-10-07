package org.openstack4j.model.image.v2.ext;

import java.util.List;

import org.openstack4j.model.ModelEntity;

/** The Glance API versions document ({@code GET /versions}). */
public interface ImageVersions extends ModelEntity {

    List<? extends ImageVersion> getVersions();

    /** @return the CURRENT version without the leading "v", for example "2.17"; the highest listed version when none is CURRENT; null if the list is empty */
    String getCurrent();

    /**
     * @param version a version such as "2.6"
     * @return whether the current server version is at least that version
     */
    boolean supports(String version);
}
