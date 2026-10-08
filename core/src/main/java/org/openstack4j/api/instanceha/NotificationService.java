package org.openstack4j.api.instanceha;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.instanceha.Notification;
import org.openstack4j.model.instanceha.VMove;
import org.openstack4j.model.instanceha.options.NotificationOptions;

/** Failure notifications ({@code /notifications}) and the instance moves of their recovery. */
public interface NotificationService extends RestService {

    /** @param filters e.g. {@code source_host_uuid}, {@code type}, {@code status}, {@code generated-since}, {@code limit}, {@code marker} */
    List<? extends Notification> list(Map<String, String> filters);

    /** @return the notification with its recovery progress (instance-ha 1.1), or {@code null} when it does not exist */
    Notification get(String notificationId);

    Notification create(NotificationOptions options);

    /** @return the instance moves of a host failure recovery (instance-ha 1.3); a missing notification raises */
    List<? extends VMove> listVMoves(String notificationId, Map<String, String> filters);

    /** @return the move, or {@code null} when it does not exist (instance-ha 1.3) */
    VMove getVMove(String notificationId, String vmoveId);
}
