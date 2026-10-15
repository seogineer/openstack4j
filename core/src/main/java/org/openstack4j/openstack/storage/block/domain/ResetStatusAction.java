package org.openstack4j.openstack.storage.block.domain;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.model.storage.block.Volume;

@JsonRootName("os-reset_status")
public class ResetStatusAction implements ModelEntity {

    private static final long serialVersionUID = 1L;

    @JsonIgnore
    private final Volume.Status status;

    public ResetStatusAction(Volume.Status status) {
        this.status = status;
    }

    public static ResetStatusAction create(Volume.Status status) {
        return new ResetStatusAction(status);
    }

    @JsonIgnore
    public Volume.Status getStatus() {
        return status;
    }

    /** The Cinder VolumeStatus literal sent as {@code status}. */
    @JsonProperty("status")
    private String statusLiteral() {
        return cinderLiteral(status);
    }

    /** error_* keeps the underscore after "error" (error_extending, error_backing-up); the rest is Status.value(). */
    static String cinderLiteral(Volume.Status status) {
        if (status == null) return null;
        String name = status.name();
        return name.startsWith("ERROR_")
                ? "error_" + name.substring(6).toLowerCase(Locale.ROOT).replace('_', '-')
                : status.value();
    }
}
