package org.openstack4j.openstack.manila.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.manila.ext.ResourceLockService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.ext.ResourceLock;
import org.openstack4j.model.manila.ext.options.ResourceLockCreate;
import org.openstack4j.model.manila.ext.options.ResourceLockUpdate;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.manila.domain.ext.ManilaResourceLock;
import org.openstack4j.openstack.manila.domain.ext.ManilaResourceLock.ManilaResourceLockList;
import org.openstack4j.openstack.manila.internal.ManilaMicroVersions;

public class ResourceLockServiceImpl extends BaseManilaExtService implements ResourceLockService {

    private static final MicroVersion FLOOR = ManilaMicroVersions.V(81);

    @Override
    public List<? extends ResourceLock> list() {
        return list(null);
    }

    @Override
    public List<? extends ResourceLock> list(Map<String, String> filters) {
        return listOf(FLOOR, ManilaResourceLockList.class, "/resource-locks", filters);
    }

    @Override
    public ResourceLock get(String id) {
        return show(FLOOR, ManilaResourceLock.class, "/resource-locks/" + id(id));
    }

    @Override
    public ResourceLock create(ResourceLockCreate create) {
        return create(FLOOR, ManilaResourceLock.class, "/resource-locks", "resource_lock", create);
    }

    @Override
    public ResourceLock update(String id, ResourceLockUpdate update) {
        return update(FLOOR, ManilaResourceLock.class, "/resource-locks/" + id(id), "resource_lock", update);
    }

    @Override
    public ActionResponse delete(String id) {
        return remove(FLOOR, "/resource-locks/" + id(id));
    }
}
