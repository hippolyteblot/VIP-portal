package fr.insalyon.creatis.vip.datamanager.integrationtest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.web.firewall.FirewalledRequest;
import org.springframework.security.web.firewall.HttpFirewall;

/**
 * Test-only configuration for the vip-datamanagement module.
 *
 * Overrides the production HttpFirewall with a permissive one so that paths
 * containing ".." segments (path-traversal test cases) are not rejected at the
 * framework level and can reach our controller, which then validates them and
 * returns the expected 400/JSON response.
 *
 * This class is only active under the "test" Spring profile (set by BaseSpringIT)
 * and is picked up automatically by SpringCoreConfig's component scan.
 */
@Configuration
@Profile("test")
public class DatamanagerTestConfig {

    @Bean
    @Primary
    public HttpFirewall permissiveHttpFirewall() {
        return new HttpFirewall() {
            @Override
            public FirewalledRequest getFirewalledRequest(HttpServletRequest request) {
                return new FirewalledRequest(request) {
                    @Override
                    public void reset() {}
                };
            }

            @Override
            public HttpServletResponse getFirewalledResponse(HttpServletResponse response) {
                return response;
            }
        };
    }
}
