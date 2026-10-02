package org.openstack4j.api.storage;

import java.util.List;
import org.openstack4j.model.storage.block.BlockExtension;

import org.openstack4j.common.RestService;

/** API extensions ({@code GET /extensions}). */
public interface BlockExtensionService extends RestService {

    List<? extends BlockExtension> list();
}
