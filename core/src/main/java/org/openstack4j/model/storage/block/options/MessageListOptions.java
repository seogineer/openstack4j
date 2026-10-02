package org.openstack4j.model.storage.block.options;

/** Filters for {@code GET /messages} (3.3+; paging and sorting 3.5+). */
public class MessageListOptions extends BlockStorageListOptions<MessageListOptions> {

    public static MessageListOptions create() {
        return new MessageListOptions();
    }

    @Override public MessageListOptions limit(int limit) { return put("limit", limit, 5); }
    @Override public MessageListOptions marker(String marker) { return put("marker", marker, 5); }
    @Override public MessageListOptions offset(int offset) { return put("offset", offset, 5); }
    @Override public MessageListOptions sortKey(String key) { return put("sort_key", key, 5); }
    @Override public MessageListOptions sortDir(String dir) { return put("sort_dir", dir, 5); }
    @Override public MessageListOptions sort(String sort) { return put("sort", sort, 5); }
    public MessageListOptions resourceType(String type) { return put("resource_type", type, 0); }
    public MessageListOptions resourceUuid(String uuid) { return put("resource_uuid", uuid, 0); }
    public MessageListOptions eventId(String eventId) { return put("event_id", eventId, 0); }
    public MessageListOptions requestId(String requestId) { return put("request_id", requestId, 0); }
    public MessageListOptions messageLevel(String level) { return put("message_level", level, 0); }
}
