package ee.taltech.ite4120.guidelines.approverregistry.xroad;

import ee.taltech.ite4120.guidelines.approverregistry.ApproverInfo;
import ee.taltech.ite4120.guidelines.approverregistry.ApproverRegistryAdapter;
import ee.taltech.ite4120.guidelines.approverregistry.ApproverRegistryException;

/**
 * The X-Road transport — a skeleton, as in the worked example.
 *
 * <p>What changes when this replaces the HTTP adapter, and what does not:
 *
 * <ul>
 *   <li>The INTERFACE does not change. {@link ApproverRegistryAdapter} is the
 *       port; the register's service and controller never learn which
 *       transport answered.</li>
 *   <li>The ENVELOPE changes. A classic X-Road call is SOAP: the routing
 *       identifiers travel in the header ({@code xro:client},
 *       {@code xro:service}, {@code xro:id}, {@code xro:protocolVersion}) and
 *       the provider's own vocabulary in the body. The security servers on
 *       both ends sign and log the whole message — which is where the
 *       evidential value comes from.</li>
 *   <li>The FAILURE SHAPE changes. A SOAP Fault arrives inside HTTP 200, so
 *       checking the status code alone reports every failure as a success.
 *       The body must be parsed before the answer is believed.</li>
 * </ul>
 *
 * <p>Access is a registration matter, not a coding one: the subsystem
 * {@code EE/EDU/taltech/ite4120} must be registered and granted rights to the
 * service before any of this answers. That is why the course runs against the
 * imitated registry, and why this class throws rather than pretends.
 */
public class XRoadApproverRegistryAdapter implements ApproverRegistryAdapter {

    private final String securityServerUrl;
    private final String xRoadInstance;
    private final String clientSubsystem;
    private final String serviceSubsystem;

    public XRoadApproverRegistryAdapter(String securityServerUrl, String xRoadInstance,
                                        String clientSubsystem, String serviceSubsystem) {
        this.securityServerUrl = securityServerUrl;
        this.xRoadInstance = xRoadInstance;
        this.clientSubsystem = clientSubsystem;
        this.serviceSubsystem = serviceSubsystem;
    }

    @Override
    public ApproverInfo lookup(String isikukood) {
        throw new ApproverRegistryException(
                "X-Road transport is not wired in this course build: would call "
                        + serviceSubsystem + " on " + xRoadInstance + " via " + securityServerUrl
                        + " as " + clientSubsystem
                        + ". Use guidelines.approver-registry.mode=http against the imitated registry.");
    }
}
