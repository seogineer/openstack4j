package org.openstack4j.openstack.octavia.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.octavia.ext.OctaviaFlavorService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.octavia.ext.OctaviaFlavor;
import org.openstack4j.model.octavia.options.OctaviaFlavorOptions;
import org.openstack4j.openstack.octavia.domain.ext.OctaviaFlavorEntity;
import org.openstack4j.openstack.octavia.domain.ext.OctaviaFlavorEntity.Flavors;

public class OctaviaFlavorServiceImpl extends BaseOctaviaExtService implements OctaviaFlavorService {

    private static final String PATH = "/lbaas/flavors";
    private static final String ROOT = "flavor";

    @Override public List<? extends OctaviaFlavor> list() { return listOf(Flavors.class, PATH, null); }
    @Override public List<? extends OctaviaFlavor> list(Map<String, String> filters) { return listOf(Flavors.class, PATH, filters); }
    @Override public OctaviaFlavor get(String id) { return show(OctaviaFlavorEntity.class, PATH + "/" + id(id)); }
    @Override public OctaviaFlavor create(OctaviaFlavorOptions options) { return create(OctaviaFlavorEntity.class, PATH, ROOT, options); }
    @Override public OctaviaFlavor update(String id, OctaviaFlavorOptions options) { return update(OctaviaFlavorEntity.class, PATH + "/" + id(id), ROOT, options); }
    @Override public ActionResponse delete(String id) { return remove(PATH + "/" + id(id)); }
}
