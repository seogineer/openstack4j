package org.openstack4j.api.manila.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.ext.ShareTransfer;
import org.openstack4j.model.manila.ext.options.ShareTransferCreate;

/** Share transfers ({@code /v2/share-transfers}, microversion 2.77). */
public interface ShareTransferService extends RestService {

    /** @return the share transfers */
    List<? extends ShareTransfer> list();

    /** @param filters query parameters such as {@code name}, {@code resource_type}, {@code resource_id}, {@code source_project_id}, {@code limit}, {@code offset} */
    List<? extends ShareTransfer> list(Map<String, String> filters);

    /** @return the share transfer, or {@code null} when it does not exist */
    ShareTransfer get(String id);

    ShareTransfer create(ShareTransferCreate create);

    /** Accepts a transfer in the receiving project with the creator's {@code authKey}; {@code clearAccessRules} removes the share's access rules. */
    ActionResponse accept(String id, String authKey, boolean clearAccessRules);

    ActionResponse delete(String id);
}
