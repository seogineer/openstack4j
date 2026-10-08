package org.openstack4j.api.reservation;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.reservation.Lease;

/** Blazar leases ({@code /v1/leases}). */
public interface LeaseService extends RestService {

    List<? extends Lease> list();

    /** @param filters query parameters, e.g. {@code project_id} (admin) */
    List<? extends Lease> list(Map<String, String> filters);

    /** @return the lease, or {@code null} when it does not exist */
    Lease get(String leaseId);

    /**
     * @param lease {@code name}, {@code start_date} ({@code "now"} or {@code YYYY-MM-DD hh:mm}), {@code end_date},
     *              {@code reservations} (e.g. {@code resource_type physical:host} with {@code min}, {@code max},
     *              {@code hypervisor_properties}, or {@code virtual:instance}, {@code virtual:floatingip}), optional
     *              {@code events}, {@code before_end_date}
     */
    Lease create(Map<String, ?> lease);

    /** @param fields e.g. {@code name}, {@code end_date}, {@code prolong_for}, {@code reservations} (with ids) */
    Lease update(String leaseId, Map<String, ?> fields);

    ActionResponse delete(String leaseId);
}
