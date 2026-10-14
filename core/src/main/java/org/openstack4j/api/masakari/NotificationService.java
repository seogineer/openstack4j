package org.openstack4j.api.masakari;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.masakari.Notification;
import org.openstack4j.model.masakari.VMove;
import org.openstack4j.model.masakari.options.NotificationOptions;

/** Failure notifications ({@code /notifications}) and the instance moves of their recovery. */
public interface NotificationService extends RestService {

    /** @param filters e.g. {@code source_host_uuid}, {@code type}, {@code status}, {@code generated-since}, {@code limit}, {@code marker} */
    List<? extends Notification> list(Map<String, String> filters);

    /** @return the notification with its recovery progress, or {@code null} when it does not exist. Always sent at instance-ha 1.1; a Masakari without 1.1 (before Stein) answers 406. */
    Notification get(String notificationId);

    Notification create(NotificationOptions options);

    /** @return the instance moves of a host failure recovery (instance-ha 1.3); a missing notification raises */
    List<? extends VMove> listVMoves(String notificationId, Map<String, String> filters);

    /** @return the move, or {@code null} when it does not exist (instance-ha 1.3) */
    VMove getVMove(String notificationId, String vmoveId);
}
