package org.openstack4j.openstack.networking.internal.ext;

import java.util.List;

import org.openstack4j.api.networking.ext.NeutronExtensionService;
import org.openstack4j.model.network.ext.NeutronExtension;
import org.openstack4j.openstack.networking.domain.ext.NeutronExtensionEntity;
import org.openstack4j.openstack.networking.domain.ext.NeutronExtensionEntity.Extensions;

public class NeutronExtensionServiceImpl extends BaseNeutronExtService implements NeutronExtensionService {

    @Override
    public List<? extends NeutronExtension> list() {
        return listOf(Extensions.class, "/extensions", null);
    }

    @Override
    public NeutronExtension get(String alias) {
        return show(NeutronExtensionEntity.class, "/extensions/" + id(alias));
    }

    @Override
    public boolean isEnabled(String alias) {
        return list().stream().anyMatch(e -> alias.equals(e.getAlias()));
    }
}
