package co.hypertest.commonutils;

import co.hypertest.commonutils.dto.RouteResponse;
import co.hypertest.commonutils.helper.RequestFilter;
import co.hypertest.commonutils.service.RouteService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Log4j2
@RestController
public class RouteController {
    private final RouteService routeService;

    @Value("${server.port}")
    String port;

    RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    /**
     * Handles the GET request to retrieve the list of all routes in current application.
     *
     * @return a list of {@link RouteResponse} objects containing the HTTP method and corresponding URI.
     * @see RouteService#getRoutes(String, RequestFilter.Filter)
     */
    @GetMapping("/get-routes-for-hyper-testing")
    public ResponseEntity<List<RouteResponse>> getRoutesForHyperTesting() {
        List<RouteResponse> routes = routeService.getRoutes(this.port, RequestFilter.getFilter());
        // Using the builder method to create the response entity
        return ResponseEntity.ok(routes);
    }
}
