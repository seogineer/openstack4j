package org.openstack4j.openstack.tacker.internal.sol;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import org.openstack4j.api.tacker.VnfPackageService;
import org.openstack4j.core.transport.HttpEntityHandler;
import org.openstack4j.core.transport.HttpResponse;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.common.Payload;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class VnfPackageServiceImpl extends BaseTackerSolService implements VnfPackageService {

    public VnfPackageServiceImpl() {
        // the v1 package controllers do not check a Version header
        super("/vnfpkgm/v1", null);
    }

    private String pkg(String vnfPkgId) {
        return path("/vnf_packages/" + id(vnfPkgId));
    }

    @Override
    public Map<String, Object> list(Map<String, String> params) {
        return page(path("/vnf_packages"), params);
    }

    @Override
    public Map<String, Object> create(Map<String, ?> userDefinedData) {
        Map<String, Object> body = userDefinedData == null ? Map.of() : Map.of("userDefinedData", userDefinedData);
        return strict(post(Map.class, path("/vnf_packages")).entity(JsonBody.of(body)));
    }

    @Override
    public Map<String, Object> get(String vnfPkgId) {
        return show(pkg(vnfPkgId));
    }

    @Override
    public Map<String, Object> update(String vnfPkgId, Map<String, ?> fields) {
        return strict(patch(Map.class, pkg(vnfPkgId)).entity(JsonBody.of(body(fields, "fields"))));
    }

    @Override
    public ActionResponse delete(String vnfPkgId) {
        return act(deleteWithResponse(pkg(vnfPkgId)));
    }

    @Override
    public ActionResponse uploadContent(String vnfPkgId, Payload<?> csarZip) {
        // a fixed length: uWSGI may not de-chunk a chunked body
        byte[] content;
        try (InputStream in = Objects.requireNonNull(csarZip, "csarZip").open()) {
            content = in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read the package content: " + e.getMessage(), e);
        }
        return act(putWithResponse(pkg(vnfPkgId) + "/package_content").entity(Payloads.create(new ByteArrayInputStream(content)))
                .contentType("application/zip"));
    }

    @Override
    public ActionResponse uploadContentFromUri(String vnfPkgId, Map<String, ?> options) {
        return act(postWithResponse(pkg(vnfPkgId) + "/package_content/upload_from_uri").entity(JsonBody.of(body(options, "options"))));
    }

    private ActionResponse download(Invocation<Void> invocation, File file) {
        Objects.requireNonNull(file, "file");
        HttpResponse response = invocation.executeWithResponse();
        if (response.getStatus() >= 400)
            return act(response);
        try (InputStream in = response.getInputStream()) {
            String type = response.getContentType();
            if (in != null && type != null && type.startsWith("application/json")) {
                // a text VNFD negotiated as JSON comes back as a JSON string
                JsonNode json = ObjectMapperSingleton.getContext(Map.class).readTree(in);
                byte[] text = json != null && json.isTextual() ? json.asText().getBytes(StandardCharsets.UTF_8)
                        : String.valueOf(json).getBytes(StandardCharsets.UTF_8);
                Files.write(file.toPath(), text);
            } else {
                Files.copy(in == null ? InputStream.nullInputStream() : in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return ActionResponse.actionSuccess(response.getStatus());
        } catch (IOException e) {
            return ActionResponse.actionFailed("Failed to write to " + file + ": " + e.getMessage(), 500);
        } finally {
            HttpEntityHandler.closeQuietly(response);
        }
    }

    private static ActionResponse act(HttpResponse failed) {
        return ActionResponse.actionFailed(failure(failed).getMessage(), failed.getStatus());
    }

    @Override
    public ActionResponse downloadContent(String vnfPkgId, File file) {
        return download(get(Void.class, pkg(vnfPkgId) + "/package_content").header("Accept", "application/zip"), file);
    }

    @Override
    public ActionResponse downloadVnfd(String vnfPkgId, String accept, File file) {
        return download(get(Void.class, pkg(vnfPkgId) + "/vnfd").header("Accept", accept == null ? "text/plain,application/zip" : accept), file);
    }

    @Override
    public ActionResponse downloadArtifact(String vnfPkgId, String artifactPath, File file) {
        Objects.requireNonNull(artifactPath, "artifactPath");
        if (artifactPath.isBlank() || artifactPath.startsWith("/") || artifactPath.contains("..") || artifactPath.indexOf('?') >= 0 || artifactPath.indexOf('#') >= 0)
            throw new IllegalArgumentException("Not a valid artifact path: '" + artifactPath + "'");
        return download(get(Void.class, pkg(vnfPkgId) + "/artifacts/" + artifactPath).header("Accept", "*/*"), file);
    }
}
