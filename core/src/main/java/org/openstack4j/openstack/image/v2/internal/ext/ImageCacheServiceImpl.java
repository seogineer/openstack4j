package org.openstack4j.openstack.image.v2.internal.ext;

import org.openstack4j.api.image.v2.ext.ImageCacheService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.image.v2.ext.ImageCacheState;
import org.openstack4j.openstack.image.v2.domain.ext.GlanceImageCacheState;

public class ImageCacheServiceImpl extends BaseImageExtService implements ImageCacheService {

    @Override public ImageCacheState list() { return showStrict(GlanceImageCacheState.class, "/cache"); }
    @Override public ActionResponse queue(String imageId) { return putWithResponse("/cache/" + id(imageId)).execute(); }
    @Override public ActionResponse delete(String imageId) { return remove("/cache/" + id(imageId)); }
    @Override public ActionResponse clear() { return remove("/cache"); }
    @Override public ActionResponse clear(String target) { return target == null ? clear() : deleteWithResponse("/cache").header("x-image-cache-clear-target", target).execute(); }
    @Override public ActionResponse clean() { return postWithResponse("/cache/clean").execute(); }
    @Override public ActionResponse prune() { return postWithResponse("/cache/prune").execute(); }
}
