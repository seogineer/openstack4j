package org.openstack4j.openstack.identity.v3.internal;

import java.util.List;
import java.util.Objects;

import org.openstack4j.api.identity.v3.EndpointPolicyService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.Endpoint;
import org.openstack4j.model.identity.v3.Policy;
import org.openstack4j.openstack.identity.v3.domain.KeystoneEndpoint.Endpoints;
import org.openstack4j.openstack.identity.v3.domain.KeystonePolicy;

public class EndpointPolicyServiceImpl extends BaseIdentityServices implements EndpointPolicyService {

    private static String base(String policyId) {
        return "/policies/" + Objects.requireNonNull(policyId) + "/OS-ENDPOINT-POLICY";
    }

    private static String endpoint(String policyId, String endpointId) {
        return base(policyId) + "/endpoints/" + Objects.requireNonNull(endpointId);
    }

    private static String service(String policyId, String serviceId) {
        return base(policyId) + "/services/" + Objects.requireNonNull(serviceId);
    }

    private static String region(String policyId, String serviceId, String regionId) {
        return service(policyId, serviceId) + "/regions/" + Objects.requireNonNull(regionId);
    }

    @Override public ActionResponse associateWithEndpoint(String p, String e) { return put(ActionResponse.class, endpoint(p, e)).execute(); }
    @Override public ActionResponse checkEndpointAssociation(String p, String e) { return getWithResponse(endpoint(p, e)).execute(); }
    @Override public ActionResponse disassociateFromEndpoint(String p, String e) { return deleteWithResponse(endpoint(p, e)).execute(); }
    @Override public ActionResponse associateWithService(String p, String s) { return put(ActionResponse.class, service(p, s)).execute(); }
    @Override public ActionResponse checkServiceAssociation(String p, String s) { return getWithResponse(service(p, s)).execute(); }
    @Override public ActionResponse disassociateFromService(String p, String s) { return deleteWithResponse(service(p, s)).execute(); }
    @Override public ActionResponse associateWithServiceInRegion(String p, String s, String r) { return put(ActionResponse.class, region(p, s, r)).execute(); }
    @Override public ActionResponse checkServiceInRegionAssociation(String p, String s, String r) { return getWithResponse(region(p, s, r)).execute(); }
    @Override public ActionResponse disassociateFromServiceInRegion(String p, String s, String r) { return deleteWithResponse(region(p, s, r)).execute(); }
    @Override public List<? extends Endpoint> endpointsForPolicy(String p) { return get(Endpoints.class, base(p) + "/endpoints").execute().getList(); }
    @Override public Policy policyForEndpoint(String e) { return get(KeystonePolicy.class, "/endpoints/" + Objects.requireNonNull(e) + "/OS-ENDPOINT-POLICY/policy").execute(); }
    @Override public ActionResponse checkPolicyAssociations(String p) { return head(ActionResponse.class, base(p) + "/policy").execute(); }
}
