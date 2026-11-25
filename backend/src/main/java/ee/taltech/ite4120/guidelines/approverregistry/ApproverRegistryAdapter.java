package ee.taltech.ite4120.guidelines.approverregistry;

/**
 * The port through which the register asks "who is this approver?".
 *
 * <p>The rest of the component depends only on this interface. Which transport
 * answers — the built-in mock or a real X-Road security server — is chosen by
 * configuration ({@code guidelines.approver-registry.mode}), not by code.
 *
 * <p>Why a registry at all: the register stores the approver's personal code
 * and nothing else. The name belongs to the population register, which is its
 * authoritative source. Storing a copy here would mean holding a name that
 * goes stale the day the person marries — one source of truth per fact.
 */
public interface ApproverRegistryAdapter {

    /**
     * Look up a person by Estonian personal code.
     *
     * @return the person, or {@code null} when the registry does not know them
     *         — a miss is not a failure
     * @throws ApproverRegistryException when the registry cannot be reached or
     *         answers with a fault; the caller decides whether that fails open
     *         or closed
     */
    ApproverInfo lookup(String isikukood);
}
