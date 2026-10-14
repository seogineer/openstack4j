package org.openstack4j.api.manila.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.manila.ext.ShareTypeInfo;

/** Share types at the session microversion (floor 2.7); the legacy shareTypes() stays at 2.6. */
public interface ShareTypeExtService extends RestService {

    /**
     * {@code GET /types}.
     *
     * @param filters is_public = true (default) | false | all, applied for admins only (others always get public types);
     *                extra_specs (2.43, a dict literal); may be null
     */
    List<? extends ShareTypeInfo> list(Map<String, String> filters);

    /** {@code GET /types/{id}}, or {@code null} when it does not exist */
    ShareTypeInfo get(String shareTypeId);
}
