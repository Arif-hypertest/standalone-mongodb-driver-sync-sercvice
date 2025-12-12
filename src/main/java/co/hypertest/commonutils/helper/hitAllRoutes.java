package co.hypertest.commonutils.helper;

import co.hypertest.commonutils.dto.RouteResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpServerErrorException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.*;

public class hitAllRoutes {

    private static final int TIMEOUT_MS = 5000;
    private static final int THREAD_POOL_SIZE = 10;
    private static final int MAX_RETRIES = 3;

    public static void main(String[] args) throws InterruptedException {
        final int port = Integer.parseInt(System.getenv().getOrDefault("DEMO_APP_PORT", "8080"));
        String inspectorUrl = "http://localhost:" + port + "/get-routes-for-hyper-testing";

        RestTemplate rest = createRestTemplate();
        RoutesResponse body = fetchRoutes(rest, inspectorUrl, port);

        if (body == null || body.getRoutes() == null) {
            System.err.println("❌ Failed to fetch or parse route response.");
            return;
        }

        List<RouteResponse> routes = body.getRoutes();
        System.out.printf("\n—– Calling %d routes on port %d —–\n\n", routes.size(), body.getPort());

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        List<Callable<Void>> tasks = routes.stream().map(route -> (Callable<Void>) () -> {
            String url = "http://localhost:" + body.getPort() + route.getReqPath();
            return processRoute(rest, route, url);
        }).toList();

        executor.invokeAll(tasks);
        executor.shutdown();
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
    }

    // Modified fetchRoutes method: parses JSON array and wraps it in a RoutesResponse
    private static RoutesResponse fetchRoutes(RestTemplate rest, String url, int port) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String jsonResponse = rest.getForObject(url, String.class);
                // Deserialize JSON array into List<RouteResponse>
                ObjectMapper mapper = new ObjectMapper();
                List<RouteResponse> routes = mapper.readValue(jsonResponse, new TypeReference<List<RouteResponse>>() {});
                // Create and return a RoutesResponse wrapping the port and list of routes
                RoutesResponse response = new RoutesResponse();
                response.setPort(port);
                response.setRoutes(routes);
                return response;
            } catch (HttpServerErrorException.InternalServerError e) {
                System.err.printf("Attempt %d/%d: Server error: %s%n",
                        attempt, MAX_RETRIES, e.getResponseBodyAsString());
                if (attempt == MAX_RETRIES) {
                    System.err.println("Maximum retry attempts reached. Failing.");
                    return null;
                }
                try {
                    Thread.sleep(1000 * attempt); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            } catch (Exception e) {
                System.err.printf("Unexpected error: %s%n", e.getMessage());
                return null;
            }
        }
        return null;
    }

    private static Void processRoute(RestTemplate rest, RouteResponse route, String url) {
        long start = System.currentTimeMillis();
        try {
            ResponseEntity<String> response = rest.exchange(
                    url,
                    HttpMethod.valueOf(route.getReqVerb().name()),
                    null,
                    String.class
            );
            long timeTaken = System.currentTimeMillis() - start;
            System.out.printf("✔ %s %s → %d [%d ms]%n",
                    route.getReqVerb(), route.getReqPath(), response.getStatusCodeValue(), timeTaken);
        } catch (Exception ex) {
            long timeTaken = System.currentTimeMillis() - start;
            System.err.printf("✖ %s %s → ERROR [%d ms]: %s%n",
                    route.getReqVerb(), route.getReqPath(), timeTaken, ex.getMessage());
        }
        return null;
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT_MS);
        factory.setReadTimeout(TIMEOUT_MS);
        return new RestTemplate(factory);
    }

    // Inner class matching expected RoutesResponse structure for this client
    private static class RoutesResponse {
        private int port;
        private List<RouteResponse> routes;

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public List<RouteResponse> getRoutes() {
            return routes;
        }

        public void setRoutes(List<RouteResponse> routes) {
            this.routes = routes;
        }
    }
}