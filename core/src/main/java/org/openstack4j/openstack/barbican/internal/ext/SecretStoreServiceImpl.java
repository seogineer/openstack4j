package org.openstack4j.openstack.barbican.internal.ext;

import java.util.Collections;
import java.util.List;

import org.openstack4j.api.barbican.ext.SecretStoreService;
import org.openstack4j.model.barbican.ext.SecretStore;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.barbican.domain.ext.BarbicanSecretStore;
import org.openstack4j.openstack.barbican.domain.ext.BarbicanSecretStore.BarbicanSecretStoreList;

public class SecretStoreServiceImpl extends BaseBarbicanExtService implements SecretStoreService {

    @Override
    public List<? extends SecretStore> list() {
        BarbicanSecretStoreList stores = get(BarbicanSecretStoreList.class, "/secret-stores").execute(propagate404());
        return stores == null ? Collections.emptyList() : stores.getList();
    }

    @Override
    public SecretStore get(String secretStoreId) {
        return get(BarbicanSecretStore.class, "/secret-stores/" + id(secretStoreId)).execute();
    }

    @Override
    public SecretStore globalDefault() {
        return get(BarbicanSecretStore.class, "/secret-stores/global-default").execute(propagate404());
    }

    @Override
    public SecretStore preferred() {
        return get(BarbicanSecretStore.class, "/secret-stores/preferred").execute();
    }

    @Override
    public ActionResponse setPreferred(String secretStoreId) {
        return postWithResponse("/secret-stores/" + id(secretStoreId) + "/preferred").execute();
    }

    @Override
    public ActionResponse unsetPreferred(String secretStoreId) {
        return deleteWithResponse("/secret-stores/" + id(secretStoreId) + "/preferred").execute();
    }
}
