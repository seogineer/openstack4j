package org.openstack4j.openstack.compute.internal;

import org.openstack4j.openstack.internal.microversion.MicroVersions;

import org.openstack4j.model.compute.KeypairListOptions;

import static org.openstack4j.openstack.compute.internal.ComputeMicroVersions.V;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

import org.openstack4j.api.compute.KeypairService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.Keypair;
import org.openstack4j.openstack.compute.domain.NovaKeypair;
import org.openstack4j.openstack.compute.domain.NovaKeypair.Keypairs;

/**
 * Keypair Service manages SSH Keys within OpenStack Compute (Nova).
 *
 * @author Jeremy Unruh
 */
public class KeypairServiceImpl extends BaseComputeServices implements KeypairService {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends Keypair> list() {
        return get(Keypairs.class, uri("/os-keypairs")).execute().getList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Keypair get(String name) {
        Objects.requireNonNull(name);
        return get(NovaKeypair.class, uri("/os-keypairs/%s", name)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionResponse delete(String name) {
        Objects.requireNonNull(name);
        return deleteWithResponse(uri("/os-keypairs/%s", name)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Keypair create(String name, @Nullable String publicKey) {
        Objects.requireNonNull(name);
        Invocation<NovaKeypair> req = post(NovaKeypair.class, uri("/os-keypairs"));
        if (publicKey == null)
            capped(req, V(91));    // key generation removed in 2.92
        return req.entity(NovaKeypair.create(name, publicKey)).execute();
    }

    @Override
    public List<? extends Keypair> list(KeypairListOptions options) {
        Objects.requireNonNull(options);
        if (options.getRequiredMicroVersion() != null)
            requireMicroVersion("Key pair list filters " + options.toQueryParams().keySet(), MicroVersions.parse(options.getRequiredMicroVersion()));
        return get(Keypairs.class, uri("/os-keypairs")).params(options.toQueryParams()).execute().getList();
    }

    @Override
    public Keypair create(String name, @Nullable String publicKey, String type) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(type);
        requireMicroVersion("Key pair type", V(2));
        Invocation<NovaKeypair> req = post(NovaKeypair.class, uri("/os-keypairs"));
        if (publicKey == null)
            capped(req, V(91));    // key generation removed in 2.92
        return req.entity(NovaKeypair.create(name, publicKey, type)).execute();
    }
}
