package org.openstack4j.model.heat.ext;

import org.openstack4j.model.ModelEntity;

/** A stack output; the value is only present when shown individually. */
public interface StackOutput extends ModelEntity {
    String getOutputKey();
    Object getOutputValue();
    String getDescription();
    String getOutputError();
}
