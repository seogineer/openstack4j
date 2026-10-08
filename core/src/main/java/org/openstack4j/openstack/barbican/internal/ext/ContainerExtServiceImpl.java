package org.openstack4j.openstack.barbican.internal.ext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.barbican.ext.ContainerExtService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class ContainerExtServiceImpl extends BaseBarbicanExtService implements ContainerExtService {

    private static String container(String containerId) {
        return "/containers/" + id(containerId);
    }

    @Override
    public ActionResponse addSecret(String containerId, String name, String secretRef) {
        return postWithResponse(container(containerId) + "/secrets").entity(secret(name, secretRef)).execute();
    }

    @Override
    public ActionResponse removeSecret(String containerId, String name, String secretRef) {
        return deleteWithResponse(container(containerId) + "/secrets").entity(secret(name, secretRef)).execute();
    }

    @Override
    public List<Map<String, Object>> listConsumers(String containerId) {
        return mapsOf(container(containerId) + "/consumers", "consumers");
    }

    @Override
    public ActionResponse registerConsumer(String containerId, String name, String url) {
        return postWithResponse(container(containerId) + "/consumers").entity(consumer(name, url)).execute();
    }

    @Override
    public ActionResponse removeConsumer(String containerId, String name, String url) {
        return deleteWithResponse(container(containerId) + "/consumers").entity(consumer(name, url)).execute();
    }

    private static JsonBody secret(String name, String secretRef) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", Objects.requireNonNull(name, "name"));
        body.put("secret_ref", Objects.requireNonNull(secretRef, "secretRef"));
        return JsonBody.of(body);
    }

    private static JsonBody consumer(String name, String url) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", Objects.requireNonNull(name, "name"));
        body.put("URL", Objects.requireNonNull(url, "url"));
        return JsonBody.of(body);
    }
}
