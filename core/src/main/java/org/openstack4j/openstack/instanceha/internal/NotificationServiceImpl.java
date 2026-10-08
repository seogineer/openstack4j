package org.openstack4j.openstack.instanceha.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openstack4j.api.instanceha.NotificationService;
import org.openstack4j.model.instanceha.Notification;
import org.openstack4j.model.instanceha.VMove;
import org.openstack4j.model.instanceha.options.NotificationOptions;
import org.openstack4j.openstack.instanceha.domain.MasakariNotification;
import org.openstack4j.openstack.instanceha.domain.MasakariNotification.MasakariNotificationList;
import org.openstack4j.openstack.instanceha.domain.MasakariVMove;
import org.openstack4j.openstack.instanceha.domain.MasakariVMove.MasakariVMoveList;

public class NotificationServiceImpl extends BaseMasakariService implements NotificationService {

    @Override
    public List<? extends Notification> list(Map<String, String> filters) {
        return listOf(MasakariNotificationList.class, "/notifications", filters);
    }

    @Override
    public Notification get(String notificationId) {
        return get(MasakariNotification.class, "/notifications/" + id(notificationId)).header(API_VERSION, "instance-ha 1.1").execute();
    }

    @Override
    public Notification create(NotificationOptions options) {
        return create(MasakariNotification.class, "/notifications", "notification", options);
    }

    @Override
    public List<? extends VMove> listVMoves(String notificationId, Map<String, String> filters) {
        MasakariVMoveList moves = get(MasakariVMoveList.class, "/notifications/" + id(notificationId) + "/vmoves").header(API_VERSION, "instance-ha 1.3")
                .params(filters == null ? Collections.emptyMap() : filters).execute(propagate404());
        return moves == null ? Collections.emptyList() : moves.getList();
    }

    @Override
    public VMove getVMove(String notificationId, String vmoveId) {
        return get(MasakariVMove.class, "/notifications/" + id(notificationId) + "/vmoves/" + id(vmoveId)).header(API_VERSION, "instance-ha 1.3").execute();
    }
}
