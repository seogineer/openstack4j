package org.openstack4j.api.gbp;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import org.openstack4j.api.AbstractTest;
import org.openstack4j.api.Builders;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.gbp.Direction;
import org.openstack4j.model.gbp.PolicyClassifier;
import org.openstack4j.model.gbp.PolicyClassifierUpdate;
import org.openstack4j.model.gbp.Protocol;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Test cases for policy classifier on GBP
 *
 * @author vinod borole
 */
@Test(suiteName = "grouppolicy/policy_classifiers")
public class PolicyClassifierServiceTest extends AbstractTest {

    private static final String POLICY_CLASSIFIERS = "/network/gbp/policy_classifiers.json";
    private static final String POLICY_CLASSIFIER = "/network/gbp/policy_classifier.json";
    private static final String POLICY_CLASSIFIER_UPDATE = "/network/gbp/policy_classifier_update.json";


    @Override
    protected Service service() {
        return Service.NETWORK;
    }

    @Test
    public void testListPolicyClassifier() throws Exception {
        respondWith(POLICY_CLASSIFIERS);
        List<? extends PolicyClassifier> policyClasifierList = osv2().gbp().policyClassifier().list();
        assertEquals(2, policyClasifierList.size());
        Objects.requireNonNull(policyClasifierList.get(0));
        Logger.getLogger(getClass().getName()).info(getClass().getName() + " : Policy Classifier from List : " + policyClasifierList.get(0));
        assertEquals(policyClasifierList.get(0).getId(), "36e41adb-0b9b-4a11-abd5-66e5386139d4");
    }

    @Test
    public void testGetPolicyClassifier() throws Exception {
        respondWith(POLICY_CLASSIFIER);
        String id = "36e41adb-0b9b-4a11-abd5-66e5386139d4";
        PolicyClassifier policyclassifier = osv2().gbp().policyClassifier().get(id);
        Logger.getLogger(getClass().getName()).info(getClass().getName() + " : Policy Classifier by ID : " + policyclassifier);
        assertNotNull(policyclassifier);
        assertEquals(id, policyclassifier.getId());
    }

    @Test
    public void testCreatePolicyClassifier() throws Exception {
        respondWith(POLICY_CLASSIFIER);
        PolicyClassifier policyClassifierCreate = Builders.policyClassifier().name("icmp").direction(Direction.BI).protocol(Protocol.ICMP).build();
        PolicyClassifier policyClassifier = osv2().gbp().policyClassifier().create(policyClassifierCreate);
        Logger.getLogger(getClass().getName()).info(getClass().getName() + " : Create Policy Classifier : " + policyClassifier);
        assertEquals("icmp", policyClassifier.getName());
    }

    @Test
    public void testUpdatePolicyClassifier() throws Exception {
        respondWith(POLICY_CLASSIFIER_UPDATE);
        String id = "36e41adb-0b9b-4a11-abd5-66e5386139d4";
        PolicyClassifierUpdate policyClassifierUpdate = Builders.policyClassifierUpdate().name("icmp").description("icmp-update").build();
        PolicyClassifier policyClassifier = osv2().gbp().policyClassifier().update(id, policyClassifierUpdate);
        Logger.getLogger(getClass().getName()).info(getClass().getName() + " : Upate Policy Action : " + policyClassifier);
        assertEquals("icmp-update", policyClassifier.getDescription());

    }

    @Test
    public void testDeletePolicyClassifier() {
        respondWith(200);
        String id = "36e41adb-0b9b-4a11-abd5-66e5386139d4";
        ActionResponse result = osv2().gbp().policyClassifier().delete(id);
        assertTrue(result.isSuccess());
    }

    @Test
    public void testClassifierPortRange() {
        assertEquals(Builders.policyClassifier().portRangeMin(1).build().getPortRange(), "1");
        assertEquals(Builders.policyClassifier().portRangeMax(9).build().getPortRange(), "9");
        assertEquals(Builders.policyClassifier().portRangeMin(1).portRangeMax(9).build().getPortRange(), "1:9");
        assertEquals(Builders.policyClassifier().portRangeMax(9).portRangeMin(1).build().getPortRange(), "1:9");
        assertEquals(Builders.policyClassifier().portRangeMin(1).portRangeMin(2).build().getPortRange(), "2");
        assertEquals(Builders.policyClassifier().portRangeMin(1).portRangeMax(9).portRangeMin(2).build().getPortRange(), "2:9");
        assertEquals(Builders.policyClassifier().portRangeMax(9).portRangeMax(8).build().getPortRange(), "8");
        assertEquals(Builders.policyClassifier().portRangeMin(1).portRangeMax(9).portRangeMax(8).build().getPortRange(), "1:8");
    }

    @Test
    public void testClassifierPortRangeFromExisting() {
        PolicyClassifier range = Builders.policyClassifier().portRangeMin(1).portRangeMax(9).build();
        assertEquals(range.toBuilder().portRangeMin(3).build().getPortRange(), "3:9");
        assertEquals(Builders.policyClassifier().from(Builders.policyClassifier().portRangeMin(1).portRangeMax(9).build())
                .portRangeMax(8).build().getPortRange(), "1:8");
        // from() resets the remembered sides of a builder that already had values
        assertEquals(Builders.policyClassifier().portRangeMin(1).portRangeMax(9)
                .from(Builders.policyClassifier().portRangeMin(20).portRangeMax(30).build()).portRangeMin(25).build().getPortRange(), "25:30");
        // single number: side unknown, legacy behavior kept
        assertEquals(Builders.policyClassifier().portRangeMin(5).build().toBuilder().portRangeMin(3).build().getPortRange(), "3:5");
        assertEquals(Builders.policyClassifier().portRangeMin(5).build().toBuilder().portRangeMax(7).build().getPortRange(), "5:7");
    }

    @Test
    public void testClassifierUpdatePortRange() {
        assertEquals(Builders.policyClassifierUpdate().portRangeMin(1).build().getPortRange(), "1");
        assertEquals(Builders.policyClassifierUpdate().portRangeMax(9).build().getPortRange(), "9");
        assertEquals(Builders.policyClassifierUpdate().portRangeMin(1).portRangeMax(9).build().getPortRange(), "1:9");
        assertEquals(Builders.policyClassifierUpdate().portRangeMax(9).portRangeMin(1).build().getPortRange(), "1:9");
        assertEquals(Builders.policyClassifierUpdate().portRangeMin(1).portRangeMin(2).build().getPortRange(), "2");
        assertEquals(Builders.policyClassifierUpdate().portRangeMin(1).portRangeMax(9).portRangeMin(2).build().getPortRange(), "2:9");
        assertEquals(Builders.policyClassifierUpdate().portRangeMax(9).portRangeMax(8).build().getPortRange(), "8");
        assertEquals(Builders.policyClassifierUpdate().portRangeMin(1).portRangeMax(9).portRangeMax(8).build().getPortRange(), "1:8");
    }

    @Test
    public void testClassifierUpdateToBuilderAndExisting() {
        PolicyClassifierUpdate update = Builders.policyClassifierUpdate().name("n").portRangeMin(1).portRangeMax(9).build();
        assertNotNull(update.toBuilder());
        PolicyClassifierUpdate copy = update.toBuilder().portRangeMin(3).build();
        assertEquals(copy.getPortRange(), "3:9");
        assertEquals(Builders.policyClassifierUpdate().from(Builders.policyClassifierUpdate().portRangeMin(1).portRangeMax(9).build())
                .portRangeMax(8).build().getPortRange(), "1:8");
        assertEquals(Builders.policyClassifierUpdate().portRangeMax(5).build().toBuilder().portRangeMin(3).build().getPortRange(), "3:5");
        assertEquals(Builders.policyClassifierUpdate().portRangeMax(5).build().toBuilder().portRangeMax(7).build().getPortRange(), "5:7");
    }

}
