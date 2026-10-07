package org.openstack4j.openstack.dns.v2.internal.ext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstack4j.api.dns.v2.ext.ZoneFileService;
import org.openstack4j.core.transport.ExecutionOptions;
import org.openstack4j.core.transport.HttpResponse;
import org.openstack4j.core.transport.propagation.PropagateOnStatus;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.dns.v2.ext.ZoneExport;
import org.openstack4j.model.dns.v2.ext.ZoneImport;
import org.openstack4j.openstack.dns.v2.domain.ext.DesignateZoneExport;
import org.openstack4j.openstack.dns.v2.domain.ext.DesignateZoneExport.DesignateZoneExportList;
import org.openstack4j.openstack.dns.v2.domain.ext.DesignateZoneImport;
import org.openstack4j.openstack.dns.v2.domain.ext.DesignateZoneImport.DesignateZoneImportList;
import org.openstack4j.openstack.internal.microversion.JsonBody;

public class ZoneFileServiceImpl extends BaseDesignateExtService implements ZoneFileService {

    private static final String TEXT_DNS = "text/dns";
    private static final String EXPORTS = "/zones/tasks/exports";
    private static final String IMPORTS = "/zones/tasks/imports";

    @Override
    public ZoneExport export(String zoneId) {
        return post(DesignateZoneExport.class, "/zones/" + id(zoneId) + "/tasks/export").entity(JsonBody.of(Map.of()))
                .execute(propagate404());
    }

    @Override
    public List<? extends ZoneExport> listExports() {
        return listExports(null);
    }

    @Override
    public List<? extends ZoneExport> listExports(Map<String, String> filters) {
        return listOf(DesignateZoneExportList.class, EXPORTS, filters);
    }

    @Override
    public ZoneExport getExport(String exportId) {
        return show(DesignateZoneExport.class, EXPORTS + "/" + id(exportId));
    }

    @Override
    public String getExportContent(String exportId) {
        return get(String.class, EXPORTS + "/" + id(exportId) + "/export").header("Accept", TEXT_DNS)
                .execute(ExecutionOptions.create(ZoneFileServiceImpl::text, PropagateOnStatus.on(404)));
    }

    @Override
    public ActionResponse deleteExport(String exportId) {
        return remove(EXPORTS + "/" + id(exportId));
    }

    @Override
    public ZoneImport importZone(String zoneFile) {
        byte[] bytes = Objects.requireNonNull(zoneFile, "zoneFile").getBytes(StandardCharsets.UTF_8);
        return post(DesignateZoneImport.class, IMPORTS).entity(Payloads.create(new ByteArrayInputStream(bytes))).contentType(TEXT_DNS)
                .execute(propagate404());
    }

    @Override
    public List<? extends ZoneImport> listImports() {
        return listImports(null);
    }

    @Override
    public List<? extends ZoneImport> listImports(Map<String, String> filters) {
        return listOf(DesignateZoneImportList.class, IMPORTS, filters);
    }

    @Override
    public ZoneImport getImport(String importId) {
        return show(DesignateZoneImport.class, IMPORTS + "/" + id(importId));
    }

    @Override
    public ActionResponse deleteImport(String importId) {
        return remove(IMPORTS + "/" + id(importId));
    }

    private static String text(HttpResponse response) {
        try (InputStream in = response.getInputStream()) {
            return in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
