package org.openstack4j.openstack.storage.block.domain;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import org.openstack4j.model.storage.block.BlockQuotaSet;
import org.openstack4j.model.storage.block.builder.BlockQuotaSetBuilder;
import org.openstack4j.util.ToStringHelper;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * An OpenStack Quota-Set
 *
 * @author Jeremy Unruh
 * @author Manuel Mazzuola
 */
@JsonRootName("quota_set")
public class CinderBlockQuotaSet implements BlockQuotaSet {

    private static final long serialVersionUID = 1L;

    private static final String VOLUME_TYPE_QUOTA_PREFIXES_REGEX = "(gigabytes|snapshots|volumes)_.*";

    @JsonProperty
    private String id;
    @JsonProperty
    private Integer snapshots;
    @JsonProperty
    private Integer volumes;
    @JsonProperty
    private Integer gigabytes;

    private Map<String, Integer> volumeTypesQuotas = new HashMap<>();
    @JsonProperty("backups")
    private Integer backups;
    @JsonProperty("backup_gigabytes")
    private Integer backupGigabytes;
    @JsonProperty("per_volume_gigabytes")
    private Integer perVolumeGigabytes;
    @JsonProperty("groups")
    private Integer groups;

    public static BlockQuotaSetBuilder builder() {
        return new BlockQuotaSetConcreteBuilder();
    }

    @Override
    public BlockQuotaSetBuilder toBuilder() {
        return new BlockQuotaSetConcreteBuilder(this);
    }

    @Override
    public String getId() {
        return id;
    }

    @JsonIgnore
    @Override
    public int getSnapshots() {
        return snapshots == null ? 0 : snapshots;
    }

    @JsonIgnore
    @Override
    public int getVolumes() {
        return volumes == null ? 0 : volumes;
    }

    @JsonIgnore
    @Override
    public int getGigabytes() {
        return gigabytes == null ? 0 : gigabytes;
    }

    @Override
    public Integer getBackups() {
        return backups;
    }

    @Override
    public Integer getBackupGigabytes() {
        return backupGigabytes;
    }

    @Override
    public Integer getPerVolumeGigabytes() {
        return perVolumeGigabytes;
    }

    @Override
    public Integer getGroups() {
        return groups;
    }

    @Override
    @JsonAnyGetter
    public Map<String, Integer> getVolumeTypesQuotas() {
        return volumeTypesQuotas;
    }

    @JsonAnySetter
    private void setVolumeTypesQuotas(String key, Integer value) {
        if (key.matches(VOLUME_TYPE_QUOTA_PREFIXES_REGEX)) this.volumeTypesQuotas.put(key, value);
    }

    @Override
    public String toString() {
        return new ToStringHelper(this).add("snapshots", snapshots).add("volumes", volumes).add("gigabytes", gigabytes).toString();
    }

    public static class BlockQuotaSetConcreteBuilder implements BlockQuotaSetBuilder {

        private CinderBlockQuotaSet model;

        BlockQuotaSetConcreteBuilder() {
            this.model = new CinderBlockQuotaSet();
        }

        BlockQuotaSetConcreteBuilder(CinderBlockQuotaSet model) {
            this.model = model;
            this.model.id = null;
        }

        @Override
        public BlockQuotaSet build() {
            return model;
        }

        @Override
        public BlockQuotaSetBuilder from(BlockQuotaSet in) {
            return new BlockQuotaSetConcreteBuilder((CinderBlockQuotaSet) in);
        }

        @Override
        public BlockQuotaSetBuilder volumes(int volumes) {
            model.volumes = volumes;
            return this;
        }

        @Override
        public BlockQuotaSetBuilder snapshots(int snapshots) {
            model.snapshots = snapshots;
            return this;
        }

        @Override
        public BlockQuotaSetBuilder backups(int backups) {
            model.backups = backups;
            return this;
        }

        @Override
        public BlockQuotaSetBuilder backupGigabytes(int backupGigabytes) {
            model.backupGigabytes = backupGigabytes;
            return this;
        }

        @Override
        public BlockQuotaSetBuilder perVolumeGigabytes(int perVolumeGigabytes) {
            model.perVolumeGigabytes = perVolumeGigabytes;
            return this;
        }

        @Override
        public BlockQuotaSetBuilder groups(int groups) {
            model.groups = groups;
            return this;
        }

        @Override
        public BlockQuotaSetBuilder gigabytes(int gigabytes) {
            model.gigabytes = gigabytes;
            return this;
        }

        @Override
        public BlockQuotaSetBuilder volumeTypesQuotas(Map<String, Integer> volumeTypesQuotas) {
            model.volumeTypesQuotas = volumeTypesQuotas;
            return this;
        }
    }
}
