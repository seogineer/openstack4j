package org.openstack4j.api;

import org.openstack4j.api.adjutant.AdjutantService;
import org.openstack4j.api.blazar.BlazarService;
import org.openstack4j.api.cloudkitty.CloudKittyService;
import org.openstack4j.api.cyborg.CyborgService;
import org.openstack4j.api.ironic.IronicService;
import org.openstack4j.api.masakari.MasakariService;
import org.openstack4j.api.watcher.WatcherService;
import org.openstack4j.api.zaqar.ZaqarService;
import org.openstack4j.api.zun.ZunService;
import org.openstack4j.openstack.adjutant.internal.AdjutantServiceImpl;
import org.openstack4j.openstack.blazar.internal.BlazarServiceImpl;
import org.openstack4j.openstack.cloudkitty.internal.CloudKittyServiceImpl;
import org.openstack4j.openstack.cyborg.internal.CyborgServiceImpl;
import org.openstack4j.openstack.ironic.internal.IronicServiceImpl;
import org.openstack4j.openstack.masakari.internal.MasakariServiceImpl;
import org.openstack4j.openstack.watcher.internal.WatcherServiceImpl;
import org.openstack4j.openstack.zaqar.internal.ZaqarServiceImpl;
import org.openstack4j.openstack.zun.internal.ZunServiceImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

/** The fork-added services are reached through their project names, and the Apis getters resolve the same implementations. */
@Test(suiteName = "ProjectNameAccessors")
public class ProjectNameAccessorTests extends AbstractTest {

    @Override
    protected Service service() {
        return Service.IDENTITY;
    }

    public void accessorsReturnProjectNamedServices() {
        OSClient.OSClientV3 os = osv3();
        Assert.assertTrue(os.ironic() instanceof IronicServiceImpl);
        Assert.assertTrue(os.masakari() instanceof MasakariServiceImpl);
        Assert.assertTrue(os.blazar() instanceof BlazarServiceImpl);
        Assert.assertTrue(os.cyborg() instanceof CyborgServiceImpl);
        Assert.assertTrue(os.cloudkitty() instanceof CloudKittyServiceImpl);
        Assert.assertTrue(os.zaqar() instanceof ZaqarServiceImpl);
        Assert.assertTrue(os.adjutant() instanceof AdjutantServiceImpl);
        Assert.assertTrue(os.watcher() instanceof WatcherServiceImpl);
        Assert.assertTrue(os.zun() instanceof ZunServiceImpl);
    }

    public void apisGettersResolveTheSameImplementations() {
        IronicService ironic = Apis.getIronicService();
        MasakariService masakari = Apis.getMasakariService();
        BlazarService blazar = Apis.getBlazarService();
        CyborgService cyborg = Apis.getCyborgService();
        CloudKittyService cloudkitty = Apis.getCloudKittyService();
        ZaqarService zaqar = Apis.getZaqarService();
        AdjutantService adjutant = Apis.getAdjutantService();
        WatcherService watcher = Apis.getWatcherService();
        ZunService zun = Apis.getZunService();
        Assert.assertEquals(ironic.getClass(), IronicServiceImpl.class);
        Assert.assertEquals(masakari.getClass(), MasakariServiceImpl.class);
        Assert.assertEquals(blazar.getClass(), BlazarServiceImpl.class);
        Assert.assertEquals(cyborg.getClass(), CyborgServiceImpl.class);
        Assert.assertEquals(cloudkitty.getClass(), CloudKittyServiceImpl.class);
        Assert.assertEquals(zaqar.getClass(), ZaqarServiceImpl.class);
        Assert.assertEquals(adjutant.getClass(), AdjutantServiceImpl.class);
        Assert.assertEquals(watcher.getClass(), WatcherServiceImpl.class);
        Assert.assertEquals(zun.getClass(), ZunServiceImpl.class);
    }
}
