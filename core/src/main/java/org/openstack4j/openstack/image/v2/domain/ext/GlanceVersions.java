package org.openstack4j.openstack.image.v2.domain.ext;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
        Objects.requireNonNull(version, "version");
        String current = getCurrent();
        return current != null && compare(strip(version), current) <= 0;
    }

    private static String strip(String id) {
        return id != null && id.startsWith("v") ? id.substring(1) : id;
    }

    /** Compares dotted versions part by part by their leading digits ("2.9" < "2.17", "2.17-beta" equals "2.17"). */
    static int compare(String a, String b) {
        String[] x = a.split("\\."), y = b.split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int xi = i < x.length ? number(x[i]) : 0;
            int yi = i < y.length ? number(y[i]) : 0;
            if (xi != yi)
                return Integer.compare(xi, yi);
        }
        return 0;
    }

    /** The leading ASCII digits of a version part ("17-beta" is 17); none counts as 0, too many as the largest int. */
    private static int number(String part) {
        int end = 0;
        while (end < part.length() && part.charAt(end) >= '0' && part.charAt(end) <= '9')
            end++;
        if (end == 0)
            return 0;
        try {
            return Integer.parseInt(part.substring(0, end));
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }
}
