package ee.taltech.ite4120.guidelines.approverregistry;

import ee.taltech.ite4120.guidelines.approverregistry.http.HttpApproverRegistryAdapter;
import ee.taltech.ite4120.guidelines.approverregistry.xroad.XRoadApproverRegistryAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chooses the transport behind {@link ApproverRegistryAdapter} from
 * configuration — the same shape as the worked example's owner registry.
 *
 * <ul>
 *   <li>{@code guidelines.approver-registry.mode=http} (default) — plain
 *       HTTP+JSON against {@code guidelines.approver-registry.url}, which
 *       locally points at this application's own imitated registry.</li>
 *   <li>{@code guidelines.approver-registry.mode=xroad} — a real security
 *       server. A skeleton in this course.</li>
 * </ul>
 */
@Configuration
public class ApproverRegistryConfig {

    @Bean
    public ApproverRegistryAdapter approverRegistryAdapter(
            @Value("${guidelines.approver-registry.mode:http}") String mode,
            @Value("${guidelines.approver-registry.url:http://localhost:18440/mock-approver-registry}") String url,
            @Value("${guidelines.approver-registry.xroad.security-server-url:http://localhost:8080}") String ssUrl,
            @Value("${guidelines.approver-registry.xroad.instance:ee-test}") String instance,
            @Value("${guidelines.approver-registry.xroad.client-subsystem:ite4120}") String clientSubsystem,
            @Value("${guidelines.approver-registry.xroad.service-subsystem:rr}") String serviceSubsystem) {
        if ("xroad".equalsIgnoreCase(mode)) {
            return new XRoadApproverRegistryAdapter(ssUrl, instance, clientSubsystem, serviceSubsystem);
        }
        return new HttpApproverRegistryAdapter(url);
    }
}
