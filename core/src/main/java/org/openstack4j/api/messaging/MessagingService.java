package org.openstack4j.api.messaging;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;

/**
 * Messaging (Zaqar v2): queues, messages, claims, subscriptions, pools and flavors. Results are {@code Map}s. Every
 * request carries a {@code Client-ID} header: a {@code Client-ID} given in the client's {@code headers(...)}, else the
 * one set with {@link #useClientId}, else a UUID generated once per client. Zaqar hides a client's own messages from
 * its listings unless {@code echo=true}.
 */
public interface MessagingService extends RestService {

    /** Uses {@code clientId} (a UUID) as the {@code Client-ID} of the current client's requests. */
    void useClientId(String clientId);

    /** @param filters e.g. {@code limit}, {@code marker}, {@code detailed}, {@code name}, {@code with_count} @return {@code queues}, {@code links}, {@code count} */
    Map<String, Object> listQueues(Map<String, String> filters);

    /** @param metadata e.g. {@code _default_message_ttl}, {@code _max_messages_post_size}, {@code _dead_letter_queue}, or {@code null} */
    ActionResponse createQueue(String queueName, Map<String, ?> metadata);

    /** @return the queue's metadata; a missing queue raises */
    Map<String, Object> getQueue(String queueName);

    /** @param patch JSON Patch operations on {@code /metadata/...} @return the queue's metadata */
    Map<String, Object> updateQueue(String queueName, List<Map<String, Object>> patch);

    ActionResponse deleteQueue(String queueName);

    /** @return {@code messages}: {@code claimed}, {@code free}, {@code total} (and oldest/newest) */
    Map<String, Object> queueStats(String queueName);

    /**
     * Creates a pre-signed URL for the queue.
     *
     * @param paths   e.g. {@code messages}, {@code claims}, {@code subscriptions}
     * @param methods e.g. {@code GET}, {@code POST}
     * @param expires e.g. {@code 2026-12-31T00:00:00}, or {@code null}
     */
    Map<String, Object> shareQueue(String queueName, List<String> paths, List<String> methods, String expires);

    /** @param resourceTypes {@code messages} and/or {@code subscriptions}, or {@code null} for both */
    ActionResponse purgeQueue(String queueName, List<String> resourceTypes);

    /** @param messages each with {@code body} and optional {@code ttl}, {@code delay} @return the new messages' hrefs */
    List<String> postMessages(String queueName, List<Map<String, Object>> messages);

    /**
     * @param filters e.g. {@code marker}, {@code limit}, {@code echo}, {@code include_claimed}
     * @return {@code messages} (empty when none) and {@code links}; the next page's {@code marker} is in the {@code next} link
     */
    Map<String, Object> listMessages(String queueName, Map<String, String> filters);

    /** @return the messages with these ids */
    List<Map<String, Object>> getMessages(String queueName, List<String> messageIds);

    /** Deletes the messages with these ids. */
    ActionResponse deleteMessages(String queueName, List<String> messageIds);

    /** Deletes and returns up to {@code count} messages. */
    List<Map<String, Object>> popMessages(String queueName, int count);

    /** @return the message, or {@code null} when it does not exist */
    Map<String, Object> getMessage(String queueName, String messageId);

    /** @param claimId the claim holding the message, or {@code null} */
    ActionResponse deleteMessage(String queueName, String messageId, String claimId);

    /**
     * Claims messages.
     *
     * @param ttl   seconds the claim lives
     * @param grace seconds added to the claimed messages' ttl
     * @param limit or {@code null}
     * @return {@code claim_id} and the claimed {@code messages}; no {@code claim_id} and no messages when none are available
     */
    Map<String, Object> claimMessages(String queueName, int ttl, int grace, Integer limit);

    /** @return the claim ({@code age}, {@code ttl}, {@code messages}), or {@code null} */
    Map<String, Object> getClaim(String queueName, String claimId);

    /** @param grace or {@code null} */
    ActionResponse updateClaim(String queueName, String claimId, int ttl, Integer grace);

    ActionResponse releaseClaim(String queueName, String claimId);

    /** @return {@code subscriptions} and {@code links} */
    Map<String, Object> listSubscriptions(String queueName, Map<String, String> filters);

    /** @return the subscription, or {@code null} */
    Map<String, Object> getSubscription(String queueName, String subscriptionId);

    /** @param subscriber e.g. {@code http://…} or {@code mailto:…} @param options or {@code null} @return the subscription id */
    String createSubscription(String queueName, String subscriber, int ttl, Map<String, ?> options);

    /** @param fields e.g. {@code subscriber}, {@code ttl}, {@code options} */
    ActionResponse updateSubscription(String queueName, String subscriptionId, Map<String, ?> fields);

    ActionResponse deleteSubscription(String queueName, String subscriptionId);

    /** Confirms (or un-confirms) a subscription that requires confirmation. */
    ActionResponse confirmSubscription(String queueName, String subscriptionId, boolean confirmed);

    /** @param filters e.g. {@code limit}, {@code marker}, {@code detailed} @return {@code pools} and {@code links} (admin) */
    Map<String, Object> listPools(Map<String, String> filters);

    /** @param pool {@code weight}, {@code uri}, optional {@code flavor}, {@code options} */
    ActionResponse createPool(String poolName, Map<String, ?> pool);

    /** @return the pool, or {@code null} */
    Map<String, Object> getPool(String poolName);

    ActionResponse updatePool(String poolName, Map<String, ?> fields);

    ActionResponse deletePool(String poolName);

    /** @return {@code flavors} and {@code links} (admin) */
    Map<String, Object> listFlavors(Map<String, String> filters);

    /** @param flavor e.g. {@code pool_list}, {@code capabilities} */
    ActionResponse createFlavor(String flavorName, Map<String, ?> flavor);

    /** @return the flavor, or {@code null} */
    Map<String, Object> getFlavor(String flavorName);

    ActionResponse updateFlavor(String flavorName, Map<String, ?> fields);

    ActionResponse deleteFlavor(String flavorName);

    /** @return whether the service answers {@code GET /v2/ping} */
    boolean ping();

    /** @return the detailed health of the service (admin) */
    Map<String, Object> health();
}
