package org.openstack4j.openstack.image.v2.domain.ext;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openstack4j.model.image.v2.ext.ImageVersions;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GlanceVersions implements ImageVersions {

    private static final long serialVersionUID = 1L;

    @JsonProperty("versions")
    private List<GlanceVersion> versions;

    @Override
    public List<GlanceVersion> getVersions() {
        return versions == null ? Collections.emptyList() : versions;
    }

    @Override
    public String getCurrent() {
        return getVersions().stream().filter(v -> "CURRENT".equals(v.getStatus())).map(v -> strip(v.getId())).findFirst()
                .orElseGet(() -> getVersions().stream().map(v -> strip(v.getId())).max(GlanceVersions::compare).orElse(null));
    }

    @Override
    public boolean supports(String version) {
        String current = getCurrent();
        return current != null && compare(strip(version), current) <= 0;
    }

    private static String strip(String id) {
        return id != null && id.startsWith("v") ? id.substring(1) : id;
    }

    /** Compares dotted versions part by part as integers ("2.9" < "2.17"). */
    static int compare(String a, String b) {
        String[] x = a.split("\\."), y = b.split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int xi = i < x.length ? Integer.parseInt(x[i]) : 0;
            int yi = i < y.length ? Integer.parseInt(y[i]) : 0;
            if (xi != yi)
                return Integer.compare(xi, yi);
        }
        return 0;
    }
}
