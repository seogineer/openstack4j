package org.openstack4j.model.storage.block.options;

/** Paging and sorting for {@code GET /volume-transfers[/detail]} (3.59+). */
public class TransferListOptions extends BlockStorageListOptions<TransferListOptions> {

    public static TransferListOptions create() {
        return new TransferListOptions();
    }

    @Override public TransferListOptions limit(int limit) { return put("limit", limit, 59); }
    @Override public TransferListOptions marker(String marker) { return put("marker", marker, 59); }
    @Override public TransferListOptions offset(int offset) { return put("offset", offset, 59); }
    @Override public TransferListOptions sortKey(String key) { return put("sort_key", key, 59); }
    @Override public TransferListOptions sortDir(String dir) { return put("sort_dir", dir, 59); }
    @Override public TransferListOptions sort(String sort) { return put("sort", sort, 59); }
}
