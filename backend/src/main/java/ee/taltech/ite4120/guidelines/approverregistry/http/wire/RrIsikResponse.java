package ee.taltech.ite4120.guidelines.approverregistry.http.wire;

/**
 * WIRE RECORD — the population registry's response exactly as the registry
 * defines it.
 *
 * <p>Field names are the provider's Estonian names, deliberately NOT
 * translated: a wire record must stay diffable against the provider's
 * published schema. When they rename {@code eesnimi}, you must be able to grep
 * for {@code eesnimi}. The English renaming happens in exactly one place — the
 * mapper inside the adapter.
 */
public record RrIsikResponse(
        String isikukood,
        String eesnimi,
        String perekonnanimi) {}
