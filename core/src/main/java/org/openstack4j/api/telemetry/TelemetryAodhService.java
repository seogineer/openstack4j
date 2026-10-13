package org.openstack4j.api.telemetry;

/**
 * {@link #capabilities()} reads Ceilometer's capabilities model; Aodh's {@code /v2/capabilities} is
 * {@code alarmsExt().capabilities()}.
 *
 * @author zhangjianweibj
 */
public interface TelemetryAodhService extends TelemetryService {

}
