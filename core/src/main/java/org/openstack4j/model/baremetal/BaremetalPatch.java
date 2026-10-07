package org.openstack4j.model.baremetal;

import java.util.Objects;

/**
 * One JSON Patch (RFC 6902) operation of an Ironic update, e.g. {@code replace("/description", "rack 3")} or
 * {@code add("/properties/memory_mb", 4096)}.
 */
public final class BaremetalPatch {

    private final String op;
    private final String path;
    private final Object value;

    private BaremetalPatch(String op, String path, Object value) {
        this.op = op;
        this.path = Objects.requireNonNull(path, "path");
        this.value = value;
    }

    public static BaremetalPatch add(String path, Object value) {
        return new BaremetalPatch("add", path, value);
    }

    public static BaremetalPatch replace(String path, Object value) {
        return new BaremetalPatch("replace", path, value);
    }

    public static BaremetalPatch remove(String path) {
        return new BaremetalPatch("remove", path, null);
    }

    public String getOp() {
        return op;
    }

    public String getPath() {
        return path;
    }

    /** @return the value; always {@code null} for {@code remove} */
    public Object getValue() {
        return value;
    }
}
