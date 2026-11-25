package ee.taltech.ite4120.guidelines.approverregistry.mock;

import ee.taltech.ite4120.guidelines.approverregistry.http.wire.RrIsikResponse;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The imitated population registry — canned data served by the application
 * itself, so the component works offline. Enabled only when
 * {@code guidelines.approver-registry.mock-server=true}.
 *
 * <p>It answers in the REGISTRY's vocabulary (the wire record), not ours, so
 * the adapter exercises the same mapping it would against the real thing.
 *
 * <p>The code {@code 00000000000} always answers 503, so the failure path can
 * be demonstrated on demand rather than by unplugging something.
 */
@RestController
@RequestMapping("/mock-approver-registry")
@ConditionalOnProperty(name = "guidelines.approver-registry.mock-server", havingValue = "true")
public class MockApproverRegistryController {

    /** The personal code that always fails — for demonstrating the failure path. */
    public static final String ALWAYS_FAILS = "00000000000";

    private static final Map<String, RrIsikResponse> ISIKUD = Map.of(
            "38102130265", new RrIsikResponse("38102130265", "Jaan", "Tamm"),
            "47503121234", new RrIsikResponse("47503121234", "Mari", "Kask"),
            "50011020017", new RrIsikResponse("50011020017", "Peeter", "Sepp"));

    @GetMapping("/isikud/{isikukood}")
    public ResponseEntity<RrIsikResponse> isik(@PathVariable String isikukood) {
        if (ALWAYS_FAILS.equals(isikukood)) {
            return ResponseEntity.status(503).build();
        }
        RrIsikResponse isik = ISIKUD.get(isikukood);
        return isik == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(isik);
    }
}
