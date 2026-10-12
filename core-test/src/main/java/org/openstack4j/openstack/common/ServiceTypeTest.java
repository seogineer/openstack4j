package org.openstack4j.openstack.common;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openstack4j.api.types.ServiceType;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.model.identity.URLResolverParams;
import org.openstack4j.openstack.identity.internal.DefaultEndpointURLResolver;
import org.openstack4j.openstack.identity.v3.domain.KeystoneToken;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class ServiceTypeTest {

    private Map<ServiceType, Collection<String>> types;

    private Map<ServiceType, Collection<String>> unknownTypes;

    @BeforeSuite
    public void setup() {
        types = new HashMap<>();
        types.put(ServiceType.IDENTITY, Arrays.asList("identity", "identityV2", "identityv3"));
        types.put(ServiceType.APP_CATALOG, Arrays.asList("application-catalog", "Application-catalogv2", "application-Catalogv3"));
        types.put(ServiceType.COMPUTE, Arrays.asList("compute", "COMPUTEv2", "computeV3"));
        types.put(ServiceType.IMAGE, Arrays.asList("image", "imageV2", "imagev3"));
        types.put(ServiceType.BLOCK_STORAGE, Arrays.asList("volume", "volumev1", "volumev2"));
        types.put(ServiceType.OBJECT_STORAGE, Arrays.asList("obJect-stOre", "object-storev2", "object-storev3"));
        types.put(ServiceType.NETWORK, Arrays.asList("network", "networkv2.0", "networkV3"));
        types.put(ServiceType.EC2, Arrays.asList("EC2", "ec2V2", "ec2v3"));
        types.put(ServiceType.TELEMETRY, Arrays.asList("metering", "meteringV2.0", "meteringv3"));
        types.put(ServiceType.TELEMETRY_AODH, Arrays.asList("alarming", "alarmingV2", "alarmingV3"));
        types.put(ServiceType.ORCHESTRATION, Arrays.asList("orchestration", "orchestrationv2", "orchestrationv3"));
        types.put(ServiceType.CLUSTERING, Arrays.asList("clustering", "clusteringV2", "clustering3"));
        types.put(ServiceType.SAHARA, Arrays.asList("data_processing", "data_processingV1", "data_processingv3"));
        types.put(ServiceType.SHARE, Arrays.asList("share", "sharev2", "shareV3"));
        types.put(ServiceType.DATABASE, Arrays.asList("database", "databaseV2", "Databasev3"));
        types.put(ServiceType.BARBICAN, Arrays.asList("key-manager", "key-managerv2", "key-managerv3"));
        types.put(ServiceType.TACKER, Arrays.asList("nfv-orchestration", "nfv-orchestrationv2", "nfv-orchestration3"));
        types.put(ServiceType.ARTIFACT, Arrays.asList("artifact", "artifactv2", "artifactv3"));
        types.put(ServiceType.MAGNUM, Arrays.asList("container", "ContainerV3", "containerv1"));
        types.put(ServiceType.DNS, Arrays.asList("dns", "dnsv2", "dnsV3"));
        types.put(ServiceType.WORKFLOW, Arrays.asList("workflow", "workflowv3", "workflowv2"));

        unknownTypes = new HashMap();
        unknownTypes.put(ServiceType.ORCHESTRATION, Arrays.asList("heat-cfg", "heatother", "heatvm", "heat-cfg4"));

    }

    @Test
    public void testNameResolveByType() {
        for (Map.Entry<ServiceType, Collection<String>> entry : types.entrySet()) {
            for (String type : entry.getValue()) {
                assertEquals(entry.getKey(), ServiceType.forName(type));
            }
        }
    }

    @Test
    public void testNameNotResolved() {
        for (Map.Entry<ServiceType, Collection<String>> entry : unknownTypes.entrySet()) {
            for (String type : entry.getValue()) {
                assertEquals(ServiceType.UNKNOWN, ServiceType.forName(type));
            }
        }
    }

    @Test
    public void aliasesResolve() {
        for (String n : new String[]{"admin-logic", "ADMIN-LOGIC", "admin-logicv1"})
            assertEquals(ServiceType.forName(n), ServiceType.REGISTRATION);
        assertEquals(ServiceType.forName("data-processing"), ServiceType.SAHARA);
        assertEquals(ServiceType.forName("container-infra"), ServiceType.MAGNUM);
        assertEquals(ServiceType.forName("container-infrastructure-management"), ServiceType.MAGNUM);
        assertEquals(ServiceType.forName("shared-file-system"), ServiceType.SHARE);
        assertEquals(ServiceType.forName("block-storage"), ServiceType.BLOCK_STORAGE);
        assertEquals(ServiceType.forName("block-storagev3"), ServiceType.BLOCK_STORAGE);
        assertEquals(ServiceType.forName("resource-optimization"), ServiceType.OPTIMIZATION);
        assertEquals(ServiceType.forName("message"), ServiceType.MESSAGING);
        assertEquals(ServiceType.forName("alarm"), ServiceType.TELEMETRY_AODH);
        assertEquals(ServiceType.forCatalogEntry("admin-logic", "renamed"), ServiceType.REGISTRATION);

        assertEquals(ServiceType.forName("container"), ServiceType.MAGNUM);
        assertEquals(ServiceType.REGISTRATION.getType(), "registration");
        assertEquals(ServiceType.MAGNUM.getType(), "container");
    }

    @Test
    public void everyIdentifierResolvesToItsOwnConstant() {
        for (ServiceType t : ServiceType.values()) {
            assertEquals(ServiceType.forName(t.getServiceName()), t);
            assertEquals(ServiceType.forName(t.getType()), t);
            assertEquals(ServiceType.forName(t.name()), t);
            for (String alias : t.getAliases())
                assertEquals(ServiceType.forName(alias), t, alias);
        }
    }

    @Test
    public void aliasTypeResolvesInV3Catalog() throws Exception {
        String json = "{\"token\":{\"project\":{\"id\":\"service-type-alias-test\",\"name\":\"p\",\"domain\":{\"id\":\"default\"}},"
                + "\"catalog\":[{\"type\":\"admin-logic\",\"name\":\"custom\",\"endpoints\":[{\"interface\":\"public\","
                + "\"region\":\"RegionOne\",\"region_id\":\"RegionOne\",\"url\":\"http://127.0.0.1:5050/v1\"}]}]}}";
        KeystoneToken token = ObjectMapperSingleton.getContext(KeystoneToken.class).readValue(json, KeystoneToken.class);
        assertEquals(new DefaultEndpointURLResolver().findURLV3(URLResolverParams.create(token, ServiceType.REGISTRATION)),
                "http://127.0.0.1:5050/v1");
    }
}
