package org.openstack4j.test.microversion;

import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.microversion.MicroVersionState;
import org.openstack4j.openstack.internal.microversion.MicroVersionStore;
import org.testng.Assert;
import org.testng.annotations.Test;

public class MicroVersionStoreFastPathTest {

    @Test
    public void hasAnyTracksWhetherAnyStateExists() {
        MicroVersionStore.clearAll();
        Assert.assertFalse(MicroVersionStore.hasAny());
        MicroVersionStore.putIfAbsent(new Object(), "k", new MicroVersionState(new MicroVersion(2, 1), new MicroVersion(2, 100)));
        Assert.assertTrue(MicroVersionStore.hasAny());
        MicroVersionStore.clearAll();
        Assert.assertFalse(MicroVersionStore.hasAny());
    }
}
