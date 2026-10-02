package org.openstack4j.model.compute;

import org.openstack4j.common.Buildable;
import org.openstack4j.model.compute.builder.BlockDeviceMappingBuilder;

/**
 * @author jaroslav.sovicka@oracle.com
 */
public interface BlockDeviceMappingCreate extends Buildable<BlockDeviceMappingBuilder> {

    /** @return device tag (2.42+) */
    default String getTag() { return null; }

    /** @return volume type (2.67+) */
    default String getVolumeType() { return null; }
}
