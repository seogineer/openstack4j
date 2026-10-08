package org.openstack4j.openstack.trove.internal.ext;

import java.util.List;
import java.util.Map;

import org.openstack4j.api.trove.ext.TroveDatastoreExtService;
import org.openstack4j.model.common.ActionResponse;

public class TroveDatastoreExtServiceImpl extends BaseTroveExtService implements TroveDatastoreExtService {

    private static final String PARAMETERS = "configuration-parameters";

    @Override
    public ActionResponse deleteDatastore(String datastore) {
        return remove("/datastores/" + id(datastore));
    }

    @Override
    public Map<String, Object> getVersion(String versionId) {
        return objectOf("/datastores/versions/" + id(versionId), "version");
    }

    @Override
    public List<Map<String, Object>> listParameters(String datastore, String version) {
        return listOf("/datastores/" + id(datastore) + "/versions/" + id(version) + "/parameters", PARAMETERS, null);
    }

    @Override
    public Map<String, Object> getParameter(String datastore, String version, String parameterName) {
        return mapOf("/datastores/" + id(datastore) + "/versions/" + id(version) + "/parameters/" + id(parameterName), null);
    }

    @Override
    public List<Map<String, Object>> listParameters(String versionId) {
        return listOf("/datastores/versions/" + id(versionId) + "/parameters", PARAMETERS, null);
    }

    @Override
    public Map<String, Object> getParameter(String versionId, String parameterName) {
        return mapOf("/datastores/versions/" + id(versionId) + "/parameters/" + id(parameterName), null);
    }

    @Override
    public List<Map<String, Object>> limits() {
        return listOf("/limits", "limits", null);
    }
}
