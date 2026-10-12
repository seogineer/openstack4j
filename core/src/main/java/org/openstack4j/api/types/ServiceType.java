package org.openstack4j.api.types;

import java.util.List;
import java.util.regex.Pattern;

public enum ServiceType {

    IDENTITY("keystone", "identity"),
    APP_CATALOG("murano", "application-catalog"),
    COMPUTE("nova", "compute"),
    IMAGE("glance", "image"),
    BLOCK_STORAGE("cinder", "volume", "block-storage"),
    OBJECT_STORAGE("object-store", "object-store"),
    NETWORK("neutron", "network"),
    OCTAVIA("octavia", "load-balancer"),
    PLACEMENT("placement", "placement"),
    EC2("ec2", "ec2"),
    TELEMETRY("ceilometer", "metering"),
    TELEMETRY_AODH("aodh", "alarming", "alarm"),
    ORCHESTRATION("heat", "orchestration"),
    CLUSTERING("senlin", "clustering"),
    SAHARA("sahara", "data_processing", "data-processing"),
    SHARE("manila", "share", "shared-file-system"),
    DATABASE("trove", "database"),
    BARBICAN("barbican", "key-manager"),
    TACKER("tacker", "nfv-orchestration"),
    ARTIFACT("glare", "artifact"),
    MAGNUM("magnum", "container", "container-infrastructure-management", "container-infra"),
    DNS("designate", "dns"),
    WORKFLOW("mistral", "workflow"),
    BAREMETAL("ironic", "baremetal"),
    INSTANCE_HA("masakari", "instance-ha"),
    RESERVATION("blazar", "reservation"),
    RATING("cloudkitty", "rating"),
    ACCELERATOR("cyborg", "accelerator"),
    MESSAGING("zaqar", "messaging", "message"),
    REGISTRATION("adjutant", "registration", "admin-logic"),
    OPTIMIZATION("watcher", "infra-optim", "resource-optimization"),
    CONTAINER_APP("zun", "application-container"),
    UNKNOWN("NA", "NA");

    private static final String SERVICE_PATTERN_SUFFIX = "[v|\\d|\\.]*";
    private final String serviceName;
    private final String type;
    private final List<String> aliases;
    private final Pattern servicePattern;

    ServiceType(String serviceName, String type, String... aliases) {
        this.serviceName = serviceName;
        this.type = type;
        this.aliases = List.of(aliases);
        StringBuilder pattern = new StringBuilder()
                .append(Pattern.quote(serviceName)).append(SERVICE_PATTERN_SUFFIX)
                .append('|').append(Pattern.quote(type)).append(SERVICE_PATTERN_SUFFIX)
                .append('|').append(Pattern.quote(this.name())).append(SERVICE_PATTERN_SUFFIX);
        for (String alias : aliases)
            pattern.append('|').append(Pattern.quote(alias)).append(SERVICE_PATTERN_SUFFIX);
        this.servicePattern = Pattern.compile(pattern.toString(), Pattern.CASE_INSENSITIVE);
    }

    public static ServiceType forName(String name) {
        if (name == null || name.isEmpty()) {
            return ServiceType.UNKNOWN;
        }
        for (ServiceType s : ServiceType.values()) {
            if (s.getServicePattern().matcher(name).matches())
                return s;
        }
        return ServiceType.UNKNOWN;
    }

    /**
     * Resolves a catalog entry: by its name when the name identifies a known service, else by its type. Zun registers
     * type {@code container} (which {@link #MAGNUM} also matches) under the name {@code zun}.
     *
     * @param type the catalog entry's type
     * @param name the catalog entry's name
     * @return the service type, or {@link #UNKNOWN}
     */
    public static ServiceType forCatalogEntry(String type, String name) {
        ServiceType byName = forName(name);
        return byName != UNKNOWN ? byName : forName(type);
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public String getType() {
        return this.type;
    }

    /**
     * Other catalog types this service is registered under (for example the service-types-authority type);
     * {@link #getType()} is unchanged. Aliases are used when a catalog entry is matched by name or type
     * ({@link #forName}, {@link #forCatalogEntry}, v3 catalog), not for lookups keyed by {@link #getType()}
     * (the v2 type fallback, {@code getServiceVersion()}).
     */
    public List<String> getAliases() {
        return this.aliases;
    }

    private Pattern getServicePattern() {
        return this.servicePattern;
    }
}
