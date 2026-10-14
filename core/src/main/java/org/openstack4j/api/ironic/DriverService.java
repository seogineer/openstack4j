package org.openstack4j.api.ironic;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.core.transport.HttpMethod;
import org.openstack4j.model.ironic.Driver;
import org.openstack4j.model.common.ActionResponse;

/** Bare metal drivers ({@code /v1/drivers}). */
public interface DriverService extends RestService {

    /** @return the drivers enabled on the conductors */
    List<? extends Driver> list();

    /** @param filters query parameters {@code type} ({@code classic} or {@code dynamic}) and {@code detail} ({@code true}), both microversion 1.30 */
    List<? extends Driver> list(Map<String, String> filters);

    /** @return the driver, or {@code null} when it is not enabled */
    Driver get(String driverName);

    /** @return the driver's {@code driver_info} properties and their descriptions; an unknown driver raises */
    Map<String, String> getProperties(String driverName);

    /** @return the RAID logical disk properties the driver accepts (microversion 1.12); an unknown driver raises */
    Map<String, Object> getRaidLogicalDiskProperties(String driverName);

    /** @return the driver's vendor passthru methods: method name to {@code http_methods}, {@code async}, {@code description}, {@code attach}; an unknown driver raises */
    Map<String, Object> listVendorPassthruMethods(String driverName);

    /**
     * Calls a driver vendor passthru method with {@code POST} ({@code POST /v1/drivers/{driver}/vendor_passthru?method=});
     * the response body is dropped (use the {@link HttpMethod} overload for it). A missing driver or method is a failed response.
     */
    ActionResponse vendorPassthru(String driverName, String method, Map<String, ?> args);

    /**
     * Calls a driver vendor passthru method and returns its result.
     *
     * @param httpMethod GET, POST, PUT, PATCH or DELETE (one of the method's {@code http_methods})
     * @param args       sent as the JSON body with POST, PUT and PATCH ({@code {}} when null); must be null or empty with GET and DELETE
     * @return the JSON object the method returned; a JSON value that is not an object (string, list, number, boolean) under the key {@code "return"};
     *         {@code null} when the response has no body (asynchronous methods answer 202 without one) or the body is JSON {@code null}
     * @throws IllegalArgumentException another HTTP method, or args with GET/DELETE
     * @throws org.openstack4j.api.exceptions.ResponseException a missing driver (404) or any other failure; a method with {@code attach=true} (raw bytes) raises too
     */
    Map<String, Object> vendorPassthru(String driverName, HttpMethod httpMethod, String method, Map<String, ?> args);
}
