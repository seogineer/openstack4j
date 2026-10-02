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
import org.openstack4j.model.storage.block.ResourceFilter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CinderResourceFilter implements ResourceFilter {

    private static final long serialVersionUID = 1L;

    private String resource;
    private List<String> filters;

    @Override public String getResource() { return resource; }
    @Override public List<String> getFilters() { return filters; }

    public static class ResourceFilters extends ListResult<CinderResourceFilter> {
        private static final long serialVersionUID = 1L;
        @JsonProperty("resource_filters")
        private List<CinderResourceFilter> items;

        @Override
        protected List<CinderResourceFilter> value() {
            return items;
        }
    }
}
