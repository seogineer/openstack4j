package org.openstack4j.openstack.dns.v2.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.dns.v2.ext.DesignateTsigKeyService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.dns.v2.ext.TsigKey;
import org.openstack4j.model.dns.v2.options.TsigKeyCreate;
import org.openstack4j.model.dns.v2.options.TsigKeyUpdate;
import org.openstack4j.openstack.dns.v2.domain.ext.DesignateTsigKey;
import org.openstack4j.openstack.dns.v2.domain.ext.DesignateTsigKey.DesignateTsigKeyList;

public class DesignateTsigKeyServiceImpl extends BaseDesignateExtService implements DesignateTsigKeyService {

    @Override
    public List<? extends TsigKey> list() {
        return list(null);
    }

    @Override
    public List<? extends TsigKey> list(Map<String, String> filters) {
        return listOf(DesignateTsigKeyList.class, "/tsigkeys", filters);
    }

    @Override
    public TsigKey get(String id) {
        return show(DesignateTsigKey.class, "/tsigkeys/" + id(id));
    }

    @Override
    public TsigKey create(TsigKeyCreate create) {
        return create(DesignateTsigKey.class, "/tsigkeys", create);
    }

    @Override
    public TsigKey update(String id, TsigKeyUpdate update) {
        return update(DesignateTsigKey.class, "/tsigkeys/" + id(id), update);
    }

    @Override
    public ActionResponse delete(String id) {
        return remove("/tsigkeys/" + id(id));
    }
}
