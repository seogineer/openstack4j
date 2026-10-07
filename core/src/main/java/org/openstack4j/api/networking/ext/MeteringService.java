package org.openstack4j.api.networking.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.common.RestService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.ext.MeteringLabel;
import org.openstack4j.model.network.ext.MeteringLabelRule;
import org.openstack4j.model.network.options.MeteringLabelOptions;
import org.openstack4j.model.network.options.MeteringLabelRuleOptions;

/**
 * Metering labels and rules ({@code /v2.0/metering}, metering): counting router traffic.
 */
public interface MeteringService extends RestService {

    /**
     * Lists metering labels.
     *
     * @return the result
     */
    List<? extends MeteringLabel> listLabels();

    /**
     * Lists metering labels.
     *
     * @param filters the filters
     * @return the result
     */
    List<? extends MeteringLabel> listLabels(Map<String, String> filters);

    /**
     * @param id the id
     * @return the result
     */
    MeteringLabel getLabel(String id);

    /**
     * Creates a metering label (admin).
     *
     * @param options the options
     * @return the result
     */
    MeteringLabel createLabel(MeteringLabelOptions options);

    /**
     * @param id the id
     * @return the action response
     */
    ActionResponse deleteLabel(String id);

    /**
     * Lists metering label rules.
     *
     * @return the result
     */
    List<? extends MeteringLabelRule> listRules();

    /**
     * Lists metering label rules.
     *
     * @param filters the filters
     * @return the result
     */
    List<? extends MeteringLabelRule> listRules(Map<String, String> filters);

    /**
     * @param id the id
     * @return the result
     */
    MeteringLabelRule getRule(String id);

    /**
     * Creates a metering label rule.
     *
     * @param options the options
     * @return the result
     */
    MeteringLabelRule createRule(MeteringLabelRuleOptions options);

    /**
     * @param id the id
     * @return the action response
     */
    ActionResponse deleteRule(String id);
}
