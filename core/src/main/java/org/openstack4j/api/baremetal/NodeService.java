package org.openstack4j.api.baremetal;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.baremetal.Node;

/** Bare metal nodes ({@code /v1/nodes}). */
public interface NodeService extends RestService {

    /** @return the nodes (summary fields; use {@code listDetail()} for all fields) */
    List<? extends Node> list();
}
