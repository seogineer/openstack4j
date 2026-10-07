package org.openstack4j.openstack.baremetal.internal;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.baremetal.RunbookService;
import org.openstack4j.model.baremetal.BaremetalPatch;
import org.openstack4j.model.baremetal.Runbook;
import org.openstack4j.model.baremetal.options.RunbookCreate;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.baremetal.domain.IronicRunbook;
import org.openstack4j.openstack.baremetal.domain.IronicRunbook.IronicRunbookList;

public class RunbookServiceImpl extends BaseBaremetalServices implements RunbookService {

    @Override
    public List<? extends Runbook> list() {
        return list(null);
    }

    @Override
    public List<? extends Runbook> list(Map<String, String> filters) {
        return listOf(IronicRunbookList.class, "/runbooks", withDetail(filters));
    }

    @Override
    public Runbook get(String ident) {
        return show(IronicRunbook.class, "/runbooks/" + id(ident));
    }

    @Override
    public Runbook create(RunbookCreate create) {
        return create(IronicRunbook.class, "/runbooks", create);
    }

    @Override
    public Runbook update(String ident, List<BaremetalPatch> patches) {
        return patchWith(IronicRunbook.class, "/runbooks/" + id(ident), patches);
    }

    @Override
    public ActionResponse delete(String ident) {
        return remove("/runbooks/" + id(ident));
    }
}
