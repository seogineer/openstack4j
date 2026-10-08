package org.openstack4j.model.instanceha.options;

import java.util.Map;
import java.util.Objects;

/** Body of a failure notification (usually sent by the masakari monitors). */
public class NotificationOptions extends MasakariAttributes<NotificationOptions> {

    /**
     * @param type          {@code COMPUTE_HOST}, {@code PROCESS} or {@code VM}
     * @param hostname      the host the failure happened on
     * @param generatedTime when the failure was detected, e.g. {@code 2017-04-23T07:18:51.523726}
     * @param payload       the event, e.g. {@code {"instance_uuid": …, "vir_domain_event": "STOPPED_DESTROYED", "event": "LIFECYCLE"}}
     */
    public static NotificationOptions create(String type, String hostname, String generatedTime, Map<String, ?> payload) {
        return new NotificationOptions().put("type", Objects.requireNonNull(type, "type")).put("hostname", Objects.requireNonNull(hostname, "hostname"))
                .put("generated_time", Objects.requireNonNull(generatedTime, "generatedTime")).put("payload", Objects.requireNonNull(payload, "payload"));
    }

    @Override
    protected NotificationOptions self() {
        return this;
    }
}
