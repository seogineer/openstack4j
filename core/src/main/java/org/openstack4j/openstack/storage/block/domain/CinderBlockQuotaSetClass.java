package org.openstack4j.openstack.storage.block.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openstack4j.model.storage.block.BlockQuotaSet;

/** A quota class ({@code /os-quota-class-sets/{class}}): the same fields as a quota set under another root name. */
@JsonRootName("quota_class_set")
public class CinderBlockQuotaSetClass extends CinderBlockQuotaSet {

    private static final long serialVersionUID = 1L;
    private static final ObjectMapper PLAIN = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /** Copies a quota set (for example from {@code Builders.blockQuotaSet()}) into the quota class shape. */
    public static CinderBlockQuotaSetClass from(BlockQuotaSet quota) {
        return quota instanceof CinderBlockQuotaSetClass ? (CinderBlockQuotaSetClass) quota : PLAIN.convertValue(quota, CinderBlockQuotaSetClass.class);
    }
}
