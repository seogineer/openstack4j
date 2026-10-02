package org.openstack4j.openstack.storage.block.internal;

import java.util.List;
import java.util.Objects;

import org.openstack4j.api.storage.BlockMessageService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.storage.block.VolumeMessage;
import org.openstack4j.model.storage.block.options.MessageListOptions;
import org.openstack4j.openstack.internal.microversion.MicroVersions;
import org.openstack4j.openstack.storage.block.domain.CinderMessage;
import org.openstack4j.openstack.storage.block.domain.CinderMessage.Messages;

import static org.openstack4j.openstack.storage.block.internal.BlockStorageMicroVersions.V;

public class BlockMessageServiceImpl extends BaseBlockStorageServices implements BlockMessageService {

    @Override
    public List<? extends VolumeMessage> list() {
        return list(MessageListOptions.create());
    }

    @Override
    public List<? extends VolumeMessage> list(MessageListOptions options) {
        Objects.requireNonNull(options);
        requireMicroVersion("Messages", V(3));
        if (options.getRequiredMicroVersion() != null)
            requireMicroVersion("Message list options " + options.toQueryParams().keySet(), MicroVersions.parse(options.getRequiredMicroVersion()));
        return get(Messages.class, uri("/messages")).params(options.toQueryParams()).execute().getList();
    }

    @Override
    public VolumeMessage get(String messageId) {
        Objects.requireNonNull(messageId);
        requireMicroVersion("Messages", V(3));
        return get(CinderMessage.class, uri("/messages/%s", messageId)).execute();
    }

    @Override
    public ActionResponse delete(String messageId) {
        Objects.requireNonNull(messageId);
        requireMicroVersion("Messages", V(3));
        return deleteWithResponse(uri("/messages/%s", messageId)).execute();
    }
}
