package org.openstack4j.openstack.image.v2.internal.ext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openstack4j.api.image.v2.ext.ImageInfoService;
import org.openstack4j.model.image.v2.ext.ImageStore;
import org.openstack4j.model.image.v2.ext.ImageUsage;
import org.openstack4j.openstack.image.v2.domain.ext.GlanceImageStore.Stores;
import org.openstack4j.openstack.image.v2.domain.ext.GlanceImageUsage;
import org.openstack4j.openstack.image.v2.domain.ext.GlanceImportMethods;

public class ImageInfoServiceImpl extends BaseImageExtService implements ImageInfoService {

    @Override
    public List<String> importMethods() {
        GlanceImportMethods methods = showStrict(GlanceImportMethods.class, "/info/import");
        return methods == null ? Collections.emptyList() : methods.getValue();
    }

    @Override public List<? extends ImageStore> stores() { return listOf(Stores.class, "/info/stores", null); }
    @Override public List<? extends ImageStore> storesDetail() { return listOf(Stores.class, "/info/stores/detail", null); }

    @Override
    public Map<String, ? extends ImageUsage> usage() {
        GlanceImageUsage usage = showStrict(GlanceImageUsage.class, "/info/usage");
        return usage == null || usage.getUsage() == null ? Collections.emptyMap() : usage.getUsage();
    }
}
