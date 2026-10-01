package io.github.seogineer.openstack4j.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.core.transport.internal.HttpExecutor;
import org.openstack4j.openstack.identity.v3.domain.KeystoneToken;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SmokeApplicationTests {

    @Test
    void httpClientConnectorIsSelected() {
        assertThat(HttpExecutor.create().getExecutorName()).isEqualTo("Apache HttpClient Connector");
    }

    @Test
    void keystoneTokenDeserializesWithBootManagedJackson() throws Exception {
        String json = "{\"token\":{\"methods\":[\"password\"],"
                + "\"expires_at\":\"2026-10-01T00:00:00.000000Z\","
                + "\"user\":{\"id\":\"u1\",\"name\":\"admin\",\"domain\":{\"id\":\"default\",\"name\":\"Default\"}}}}";

        KeystoneToken token = ObjectMapperSingleton.getContext(KeystoneToken.class)
                .readerFor(KeystoneToken.class).readValue(json);

        assertThat(token.getMethods()).containsExactly("password");
        assertThat(token.getUser().getName()).isEqualTo("admin");
    }
}
