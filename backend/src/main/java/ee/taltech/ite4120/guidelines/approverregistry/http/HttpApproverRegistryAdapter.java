package ee.taltech.ite4120.guidelines.approverregistry.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.taltech.ite4120.guidelines.approverregistry.ApproverInfo;
import ee.taltech.ite4120.guidelines.approverregistry.ApproverRegistryAdapter;
import ee.taltech.ite4120.guidelines.approverregistry.ApproverRegistryException;
import ee.taltech.ite4120.guidelines.approverregistry.http.wire.RrIsikResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Layer A + the mapper: fetches the registry's answer over plain HTTP+JSON and
 * translates the wire record into our domain {@link ApproverInfo}.
 *
 * <p>In the default local configuration the URL points back at this very
 * application's mock registry — an imitated environment, which is what the
 * assessment criteria permit. Point it at a real registry by changing
 * {@code guidelines.approver-registry.url}; nothing else changes.
 */
public class HttpApproverRegistryAdapter implements ApproverRegistryAdapter {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper json = new ObjectMapper();
    private final String baseUrl;

    public HttpApproverRegistryAdapter(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public ApproverInfo lookup(String isikukood) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/isikud/" + isikukood))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                // A person the registry does not know is a MISS, not a failure.
                return null;
            }
            if (response.statusCode() != 200) {
                throw new ApproverRegistryException(
                        "approver registry answered HTTP " + response.statusCode());
            }
            RrIsikResponse wire = json.readValue(response.body(), RrIsikResponse.class);
            // The mapper — the ONLY place where Estonian wire names become ours.
            return new ApproverInfo(wire.isikukood(), wire.eesnimi(), wire.perekonnanimi());
        } catch (IOException e) {
            throw new ApproverRegistryException("approver registry unreachable: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApproverRegistryException("approver registry call interrupted", e);
        }
    }
}
