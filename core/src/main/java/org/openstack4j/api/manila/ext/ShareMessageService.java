package org.openstack4j.api.manila.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.manila.ext.ShareMessage;

/** User messages ({@code /v2/messages}, microversion 2.37). */
public interface ShareMessageService extends RestService {

    /** @return the user messages */
    List<? extends ShareMessage> list();

    /** @param filters query parameters such as {@code resource_type}, {@code resource_id}, {@code message_level}, {@code action_id}, {@code request_id}, {@code created_since} (2.52), {@code limit} */
    List<? extends ShareMessage> list(Map<String, String> filters);

    /** @return the message, or {@code null} when it does not exist */
    ShareMessage get(String id);

    ActionResponse delete(String id);
}
