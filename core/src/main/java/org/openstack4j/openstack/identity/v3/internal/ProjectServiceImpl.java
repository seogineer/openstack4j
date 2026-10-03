package org.openstack4j.openstack.identity.v3.internal;

import org.openstack4j.openstack.internal.microversion.JsonBody;
import org.openstack4j.openstack.identity.v3.domain.KeystoneProjectTags;
import java.util.Collections;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import org.openstack4j.model.identity.v3.options.ProjectListOptions;
import java.util.List;
import java.util.Objects;

import org.openstack4j.api.identity.v3.ProjectService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.Project;
import org.openstack4j.openstack.identity.v3.domain.KeystoneProject;
import org.openstack4j.openstack.identity.v3.domain.KeystoneProject.Projects;

import static org.openstack4j.core.transport.ClientConstants.PATH_PROJECTS;

public class ProjectServiceImpl extends BaseIdentityServices implements ProjectService {

    @Override
    public Project create(Project project) {
        Objects.requireNonNull(project);
        return post(KeystoneProject.class, PATH_PROJECTS).entity(project).execute();
    }

    @Override
    public Project create(String domainId, String name, String description, boolean enabled) {
        Objects.requireNonNull(domainId);
        Objects.requireNonNull(name);
        Objects.requireNonNull(description);
        Objects.requireNonNull(enabled);
        return create(KeystoneProject.builder().domainId(domainId).name(name).description(description).enabled(enabled).build());
    }

    @Override
    public Project get(String projectId) {
        Objects.requireNonNull(projectId);
        return get(KeystoneProject.class, PATH_PROJECTS, "/", projectId).execute();
    }

    @Override
    public List<? extends Project> getByName(String projectName) {
        Objects.requireNonNull(projectName);
        return get(Projects.class, uri(PATH_PROJECTS)).param("name", projectName).execute().getList();
    }

    @Override
    public Project getByName(String projectName, String domainId) {
        Objects.requireNonNull(projectName);
        Objects.requireNonNull(domainId);
        return get(Projects.class, uri(PATH_PROJECTS)).param("name", projectName).param("domain_id", domainId).execute().first();
    }

    @Override
    public Project update(Project project) {
        Objects.requireNonNull(project);
        return patch(KeystoneProject.class, PATH_PROJECTS, "/", project.getId()).entity(project).execute();
    }

    @Override
    public ActionResponse delete(String projectId) {
        Objects.requireNonNull(projectId);
        return deleteWithResponse(PATH_PROJECTS, "/", projectId).execute();
    }

    @Override
    public List<? extends Project> list() {
        return get(Projects.class, uri(PATH_PROJECTS)).execute().getList();
    }

    @Override
    public List<? extends Project> list(ProjectListOptions options) {
        return get(Projects.class, uri(PATH_PROJECTS)).params(Objects.requireNonNull(options).toQueryParams()).execute().getList();
    }


    /** Tags may contain spaces and other reserved characters; encode them as one path segment. */
    private static String tagSegment(String tag) {
        return URLEncoder.encode(Objects.requireNonNull(tag), StandardCharsets.UTF_8).replace("+", "%20");
    }

    @Override
    public List<String> tags(String projectId) {
        KeystoneProjectTags tags = get(KeystoneProjectTags.class, uri("/projects/%s/tags", Objects.requireNonNull(projectId))).execute();
        return tags == null || tags.getTags() == null ? Collections.emptyList() : tags.getTags();
    }

    @Override
    public ActionResponse hasTag(String projectId, String tag) {
        return getWithResponse(uri("/projects/%s/tags/%s", Objects.requireNonNull(projectId), tagSegment(tag))).execute();
    }

    @Override
    public ActionResponse addTag(String projectId, String tag) {
        return putWithResponse(uri("/projects/%s/tags/%s", Objects.requireNonNull(projectId), tagSegment(tag))).execute();
    }

    @Override
    public List<String> replaceTags(String projectId, List<String> tags) {
        KeystoneProjectTags result = put(KeystoneProjectTags.class, uri("/projects/%s/tags", Objects.requireNonNull(projectId)))
                .entity(JsonBody.of(Collections.singletonMap("tags", Objects.requireNonNull(tags)))).execute();
        return result == null || result.getTags() == null ? Collections.emptyList() : result.getTags();
    }

    @Override
    public ActionResponse removeTag(String projectId, String tag) {
        return deleteWithResponse(uri("/projects/%s/tags/%s", Objects.requireNonNull(projectId), tagSegment(tag))).execute();
    }

    @Override
    public ActionResponse removeAllTags(String projectId) {
        return deleteWithResponse(uri("/projects/%s/tags", Objects.requireNonNull(projectId))).execute();
    }
}
