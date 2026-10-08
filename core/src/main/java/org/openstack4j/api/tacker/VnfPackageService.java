package org.openstack4j.api.tacker;

import java.io.File;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.common.Payload;

/**
 * VNF package management (ETSI NFV-SOL 005/003, Tacker {@code /vnfpkgm/v1}). Results are {@code Map}s; a list returns
 * {@code items} and {@code next}, the {@code nextpage_opaque_marker} of the following page (or {@code null}).
 */
public interface VnfPackageService extends RestService {

    /** @param params e.g. {@code filter} ({@code (eq,onboardingState,ONBOARDED)}), {@code all_fields}, {@code fields}, {@code nextpage_opaque_marker}, {@code all_records}, or {@code null} */
    Map<String, Object> list(Map<String, String> params);

    /** @param userDefinedData or {@code null} @return the new (empty) package */
    Map<String, Object> create(Map<String, ?> userDefinedData);

    /** @return the package, or {@code null} when it does not exist */
    Map<String, Object> get(String vnfPkgId);

    /** @param fields {@code operationalState} and/or {@code userDefinedData} @return the changes made */
    Map<String, Object> update(String vnfPkgId, Map<String, ?> fields);

    ActionResponse delete(String vnfPkgId);

    /** Uploads the package's content (a CSAR zip); onboarding continues asynchronously. */
    ActionResponse uploadContent(String vnfPkgId, Payload<?> csarZip);

    /** @param options {@code addressInformation} and optionally {@code userName}, {@code password} */
    ActionResponse uploadContentFromUri(String vnfPkgId, Map<String, ?> options);

    /** Downloads the package's content (a CSAR zip) to {@code file}. */
    ActionResponse downloadContent(String vnfPkgId, File file);

    /** Downloads the VNFD ({@code text/plain} YAML or {@code application/zip}, as the package allows) to {@code file}. */
    ActionResponse downloadVnfd(String vnfPkgId, String accept, File file);

    /** Downloads an artifact of the package (e.g. {@code Scripts/install.sh}) to {@code file}. */
    ActionResponse downloadArtifact(String vnfPkgId, String artifactPath, File file);
}
