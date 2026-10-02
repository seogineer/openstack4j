package org.openstack4j.api.storage;

import java.util.List;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.storage.block.VolumeMessage;
import org.openstack4j.model.storage.block.options.MessageListOptions;

/** User-facing fault messages ({@code /messages}, block storage microversion 3.3+; paging and sorting 3.5+). */
public interface BlockMessageService extends RestService {

    List<? extends VolumeMessage> list();

    List<? extends VolumeMessage> list(MessageListOptions options);

    VolumeMessage get(String messageId);

    ActionResponse delete(String messageId);
}
