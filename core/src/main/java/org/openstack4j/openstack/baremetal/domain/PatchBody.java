package org.openstack4j.openstack.baremetal.domain;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.model.baremetal.BaremetalPatch;

/** A JSON Patch request body. {@code add} and {@code replace} always carry {@code value}, even a null one. */
public final class PatchBody implements ModelEntity {

    private static final long serialVersionUID = 1L;
    private static final ObjectMapper PLAIN = new ObjectMapper();

    private final ArrayNode node;

    private PatchBody(ArrayNode node) {
        this.node = node;
    }

    public static PatchBody of(List<BaremetalPatch> patches) {
        Objects.requireNonNull(patches, "patches");
        if (patches.isEmpty())
            throw new IllegalArgumentException("An update needs at least one patch operation");
        ArrayNode array = PLAIN.createArrayNode();
        for (BaremetalPatch patch : patches) {
            ObjectNode op = array.addObject().put("op", patch.getOp()).put("path", patch.getPath());
            if (!"remove".equals(patch.getOp()))
                op.set("value", PLAIN.valueToTree(patch.getValue()));
        }
        return new PatchBody(array);
    }

    @JsonValue
    public ArrayNode toJson() {
        return node;
    }
}
