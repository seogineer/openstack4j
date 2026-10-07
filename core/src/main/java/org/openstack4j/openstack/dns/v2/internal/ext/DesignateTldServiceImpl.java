package org.openstack4j.openstack.dns.v2.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.dns.v2.ext.DesignateTldService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.dns.v2.ext.Tld;
import org.openstack4j.model.dns.v2.options.TldCreate;
import org.openstack4j.model.dns.v2.options.TldUpdate;
import org.openstack4j.openstack.dns.v2.domain.ext.DesignateTld;
import org.openstack4j.openstack.dns.v2.domain.ext.DesignateTld.DesignateTldList;

public class DesignateTldServiceImpl extends BaseDesignateExtService implements DesignateTldService {

    @Override
    public List<? extends Tld> list() {
        return list(null);
    }

    @Override
    public List<? extends Tld> list(Map<String, String> filters) {
        return listOf(DesignateTldList.class, "/tlds", filters);
    }

    @Override
    public Tld get(String id) {
        return show(DesignateTld.class, "/tlds/" + id(id));
    }

    @Override
    public Tld create(TldCreate create) {
        return create(DesignateTld.class, "/tlds", create);
    }

    @Override
    public Tld update(String id, TldUpdate update) {
        return update(DesignateTld.class, "/tlds/" + id(id), update);
    }

    @Override
    public ActionResponse delete(String id) {
        return remove("/tlds/" + id(id));
    }
}
