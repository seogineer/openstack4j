package org.openstack4j.test.microversion;

import org.openstack4j.api.exceptions.MicroVersionException;
import org.openstack4j.api.placement.v1.exceptions.PlacementMicroVersionException;
import org.openstack4j.openstack.internal.MicroVersion;
import org.openstack4j.openstack.internal.microversion.MicroVersionState;
import org.openstack4j.openstack.internal.microversion.MicroVersionStore;
import org.openstack4j.openstack.internal.microversion.MicroVersions;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class MicroVersionStoreTest {

    @BeforeMethod
    public void clear() {
        MicroVersionStore.clearAll();
    }

    @Test
    public void stateIsKeptPerSessionIdentityAndKey() {
        Object sessionA = new Object();
        Object sessionB = new Object();
        MicroVersionState a = MicroVersionStore.putIfAbsent(sessionA, "compute|http://x", new MicroVersionState(new MicroVersion(2, 1), new MicroVersion(2, 100)));

        Assert.assertSame(MicroVersionStore.get(sessionA, "compute|http://x"), a);
        Assert.assertNull(MicroVersionStore.get(sessionA, "compute|http://y"));
        Assert.assertNull(MicroVersionStore.get(sessionB, "compute|http://x"));
    }

    @Test
    public void putIfAbsentKeepsTheFirstState() {
        Object session = new Object();
        MicroVersionState first = MicroVersionStore.putIfAbsent(session, "k", new MicroVersionState(new MicroVersion(2, 1), new MicroVersion(2, 50)));
        MicroVersionState second = MicroVersionStore.putIfAbsent(session, "k", new MicroVersionState(new MicroVersion(2, 1), new MicroVersion(2, 99)));

        Assert.assertSame(second, first);
        Assert.assertEquals(second.getServerMax().toString(), "2.50");
    }

    @Test
    public void newStateIsDisabledAndUnpinned() {
        MicroVersionState state = new MicroVersionState(new MicroVersion(2, 1), new MicroVersion(2, 100));
        Assert.assertFalse(state.isEnabled());
        Assert.assertNull(state.getPinned());
    }

    @Test
    public void minMaxAndParse() {
        Assert.assertEquals(MicroVersions.min(new MicroVersion(2, 104), new MicroVersion(2, 100)).toString(), "2.100");
        Assert.assertEquals(MicroVersions.max(new MicroVersion(2, 1), new MicroVersion(2, 9)).toString(), "2.9");
        Assert.assertEquals(MicroVersions.parse("2.47").toString(), "2.47");
    }

    @Test(expectedExceptions = MicroVersionException.class)
    public void parseRejectsGarbage() {
        MicroVersions.parse("latest");
    }

    @Test
    public void placementExceptionIsAMicroVersionException() {
        Assert.assertTrue(new PlacementMicroVersionException("x") instanceof MicroVersionException);
    }
}
