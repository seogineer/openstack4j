package org.openstack4j.openstack.storage.block.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.internal.microversion.MicroVersions;
import org.openstack4j.openstack.storage.block.domain.*;

import static org.openstack4j.openstack.storage.block.internal.BlockStorageMicroVersions.V;
import org.openstack4j.api.storage.BlockExtensionService;
import org.openstack4j.model.storage.block.BlockExtension;
import org.openstack4j.openstack.storage.block.domain.CinderExtension.Extensions;

public class BlockExtensionServiceImpl extends BaseBlockStorageServices implements BlockExtensionService {

    @Override
    public List<? extends BlockExtension> list() {
        return get(Extensions.class, uri("/extensions")).execute().getList();
    }
}
