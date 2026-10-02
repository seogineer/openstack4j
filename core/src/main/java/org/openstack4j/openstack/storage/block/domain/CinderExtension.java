package org.openstack4j.openstack.storage.block.domain;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.ModelEntity;
import org.openstack4j.openstack.common.ListResult;
import org.openstack4j.model.storage.block.BlockExtension;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderExtension implements BlockExtension {

    private static final long serialVersionUID = 1L;

    private String name;
    private String alias;
    private String description;
    private String updated;

    @Override public String getName() { return name; }
    @Override public String getAlias() { return alias; }
    @Override public String getDescription() { return description; }
    @Override public String getUpdated() { return updated; }

    public static class Extensions extends ListResult<CinderExtension> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("extensions")
        private List<CinderExtension> items;

        @Override
        protected List<CinderExtension> value() {
            return items;
        }
    }
}
