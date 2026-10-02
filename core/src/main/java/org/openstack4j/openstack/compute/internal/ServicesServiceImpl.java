package org.openstack4j.openstack.compute.internal;

import org.openstack4j.openstack.compute.functions.ToActionResponseFunction;

import org.openstack4j.openstack.compute.domain.JsonBody;

import org.openstack4j.model.compute.ext.ServiceUpdate;

import org.openstack4j.model.common.ActionResponse;

import java.util.LinkedHashMap;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.openstack4j.openstack.compute.internal.ComputeMicroVersions.V;

import org.openstack4j.api.compute.ext.ServicesService;
import org.openstack4j.model.compute.ext.Service;
import org.openstack4j.openstack.compute.domain.ext.ExtService;
import org.openstack4j.openstack.compute.domain.ext.ExtService.Services;
import org.openstack4j.openstack.manila.domain.actions.ServiceAction;
import org.openstack4j.openstack.manila.domain.actions.ServiceActions;

/**
 * Compute Services service provides CRUD capabilities for nova service(s).
 *
 * @author Stephan Latour
 */
public class ServicesServiceImpl extends BaseComputeServices implements ServicesService {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends Service> list() {
        return get(Services.class, uri("/os-services")).execute().getList();
    }

    /**
     * Returns list of compute services filtered by parameters.
     * <p>Author:Wang Ting/王婷</p>
     *
     * @Title: list
     * @see org.openstack4j.api.compute.ServicesService#list(java.util.Map)
     */
    @Override
    public List<? extends Service> list(Map<String, String> filteringParams) {
        Invocation<Services> req = get(Services.class, uri("/os-services"));
        if (filteringParams != null) {
            for (Map.Entry<String, String> entry : filteringParams.entrySet()) {
                req = req.param(entry.getKey(), entry.getValue());
            }
        }
        return req.execute().getList();
    }

    /**
     * Enables a compute services.
     * <p>Author:Wang Ting/王婷</p>
     *
     * @Title: enableService
     * @see org.openstack4j.api.compute.ServicesService#enableService(java.lang.String, java.lang.String)
     */
    @Override
    public ExtService enableService(String binary, String host) {
        Objects.requireNonNull(binary);
        Objects.requireNonNull(host);

        return capped(put(ExtService.class, uri("/os-services/enable")), V(52)).entity(ServiceAction.enable(binary, host)).execute();
    }

    /**
     * Disables a compute service.
     * <p>Author:Wang Ting/王婷</p>
     *
     * @Title: disableService
     * @see org.openstack4j.api.compute.ServicesService#disableService(java.lang.String, java.lang.String)
     */
    @Override
    public ExtService disableService(String binary, String host) {
        Objects.requireNonNull(binary);
        Objects.requireNonNull(host);

        return capped(put(ExtService.class, uri("/os-services/disable")), V(52)).entity(ServiceAction.disable(binary, host)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExtService forceDownService(String binary, String host) {
        Objects.requireNonNull(binary);
        Objects.requireNonNull(host);
        return forceDownInvocation().entity(ServiceActions.forceDown(binary, host)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExtService forceUpService(String binary, String host) {
        Objects.requireNonNull(binary);
        Objects.requireNonNull(host);
        return forceDownInvocation().entity(ServiceActions.forceUp(binary, host)).execute();
    }

    private Invocation<ExtService> forceDownInvocation() {
        Invocation<ExtService> invocation = put(ExtService.class, uri("/os-services/force-down"));
        if (effectiveMicroVersion(null) == null) {
            invocation.header("x-openstack-nova-api-version", "2.11");    // legacy behaviour
        } else {
            requireMicroVersion("Forcing a service down or up", V(11));
            capped(invocation, V(52));    // binary/host body replaced by PUT /os-services/{id} in 2.53
        }
        return invocation;
    }

    @Override
    public Service update(String serviceId, ServiceUpdate update) {
        Objects.requireNonNull(serviceId);
        Objects.requireNonNull(update);
        requireMicroVersion("Updating a service by id", V(53));
        return put(ExtService.class, uri("/os-services/%s", serviceId)).entity(JsonBody.of(update.toMap())).execute();
    }

    @Override
    public ActionResponse delete(String serviceId) {
        Objects.requireNonNull(serviceId);
        return ToActionResponseFunction.INSTANCE.apply(delete(Void.class, uri("/os-services/%s", serviceId)).executeWithResponse());
    }

    @Override
    public ExtService disableWithReason(String binary, String host, String reason) {
        Objects.requireNonNull(binary);
        Objects.requireNonNull(host);
        Objects.requireNonNull(reason);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("binary", binary);
        body.put("host", host);
        body.put("disabled_reason", reason);
        return capped(put(ExtService.class, uri("/os-services/disable-log-reason")), V(52)).entity(JsonBody.of(body)).execute();
    }
}
