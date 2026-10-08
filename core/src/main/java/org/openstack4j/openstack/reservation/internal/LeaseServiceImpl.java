package org.openstack4j.openstack.reservation.internal;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.reservation.LeaseService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.reservation.Lease;
import org.openstack4j.openstack.reservation.domain.BlazarLease;
import org.openstack4j.openstack.reservation.domain.BlazarLease.BlazarLeaseList;

public class LeaseServiceImpl extends BaseBlazarService implements LeaseService {

    @Override
    public List<? extends Lease> list() {
        return list(null);
    }

    @Override
    public List<? extends Lease> list(Map<String, String> filters) {
        return listOf(BlazarLeaseList.class, "/leases", filters);
    }

    @Override
    public Lease get(String leaseId) {
        return show(BlazarLease.class, "/leases/" + id(leaseId));
    }

    @Override
    public Lease create(Map<String, ?> lease) {
        return create(BlazarLease.class, "/leases", lease);
    }

    @Override
    public Lease update(String leaseId, Map<String, ?> fields) {
        return update(BlazarLease.class, "/leases/" + id(leaseId), fields);
    }

    @Override
    public ActionResponse delete(String leaseId) {
        return remove("/leases/" + id(leaseId));
    }
}
