package org.openstack4j.api.manila.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.ext.ResourceLock;
import org.openstack4j.model.manila.ext.options.ResourceLockCreate;
import org.openstack4j.model.manila.ext.options.ResourceLockUpdate;

/** Resource locks ({@code /v2/resource-locks}, microversion 2.81). */
public interface ResourceLockService extends RestService {

    /** @return the resource locks */
    List<? extends ResourceLock> list();

    /** @param filters query parameters such as {@code resource_id}, {@code resource_type}, {@code resource_action}, {@code lock_context}, {@code all_projects}, {@code limit}, {@code offset} */
    List<? extends ResourceLock> list(Map<String, String> filters);

    /** @return the resource lock, or {@code null} when it does not exist */
    ResourceLock get(String id);

    ResourceLock create(ResourceLockCreate create);

    ResourceLock update(String id, ResourceLockUpdate update);

    ActionResponse delete(String id);
}
