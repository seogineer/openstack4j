package org.openstack4j.api.dns.v2.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.dns.v2.ext.ZoneExport;
import org.openstack4j.model.dns.v2.ext.ZoneImport;

/** Zone exports and imports as zone files ({@code /v2/zones/tasks/exports}, {@code /v2/zones/tasks/imports}). */
public interface ZoneFileService extends RestService {

    /** Starts exporting a zone; poll {@link #getExport(String)} until its status is {@code COMPLETE}. */
    ZoneExport export(String zoneId);

    List<? extends ZoneExport> listExports();

    /** @param filters query parameters such as {@code status}, {@code zone_id}, {@code limit}, {@code marker} */
    List<? extends ZoneExport> listExports(Map<String, String> filters);

    /** @return the export, or {@code null} when it does not exist */
    ZoneExport getExport(String exportId);

    /** @return the exported zone file ({@code text/dns}); a missing or unfinished export raises */
    String getExportContent(String exportId);

    ActionResponse deleteExport(String exportId);

    /** Starts creating a zone from a zone file; poll {@link #getImport(String)} for the new zone's id. */
    ZoneImport importZone(String zoneFile);

    List<? extends ZoneImport> listImports();

    /** @param filters query parameters such as {@code status}, {@code zone_id}, {@code limit}, {@code marker} */
    List<? extends ZoneImport> listImports(Map<String, String> filters);

    /** @return the import, or {@code null} when it does not exist */
    ZoneImport getImport(String importId);

    ActionResponse deleteImport(String importId);
}
