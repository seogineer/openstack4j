package org.openstack4j.api.baremetal;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.baremetal.BaremetalPatch;
import org.openstack4j.model.baremetal.Runbook;
import org.openstack4j.model.baremetal.options.RunbookCreate;
import org.openstack4j.model.common.ActionResponse;

/** Bare metal runbooks ({@code /v1/runbooks}) (microversion 1.92). */
public interface RunbookService extends RestService {

    /** @return the runbooks with all fields */
    List<? extends Runbook> list();

    /** @param filters query parameters such as {@code project}, {@code limit}, {@code marker} ({@code detail=true} is sent unless given) */
    List<? extends Runbook> list(Map<String, String> filters);

    /** @return the runbook, or {@code null} when it does not exist */
    Runbook get(String ident);

    Runbook create(RunbookCreate create);

    /** Updates with JSON Patch operations. */
    Runbook update(String ident, List<BaremetalPatch> patches);

    ActionResponse delete(String ident);
}
