package org.openstack4j.openstack.identity.v3.internal;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.openstack4j.api.identity.v3.RevocationEventService;
import org.openstack4j.model.identity.v3.RevocationEvent;
import org.openstack4j.openstack.identity.v3.domain.KeystoneRevocationEvent.RevocationEvents;

public class RevocationEventServiceImpl extends BaseIdentityServices implements RevocationEventService {

    @Override
    public List<? extends RevocationEvent> list() {
        return get(RevocationEvents.class, "/OS-REVOKE/events").execute().getList();
    }

    @Override
    public List<? extends RevocationEvent> list(Date since) {
        return get(RevocationEvents.class, "/OS-REVOKE/events")
                .param("since", Objects.requireNonNull(since).toInstant().truncatedTo(ChronoUnit.SECONDS).toString()).execute().getList();
    }
}
