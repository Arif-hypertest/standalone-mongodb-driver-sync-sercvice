package co.hypertest.commonutils.service;


import co.hypertest.commonutils.dto.RouteResponse;
import co.hypertest.commonutils.helper.RequestFilter;

import java.util.List;

public interface RouteService {
    /**
     * @return a list of {@link RouteResponse} objects containing the HTTP method and corresponding URI.
     * @author Kuldeep Bishnoi
     */
    public List<RouteResponse> getRoutes(String port, RequestFilter.Filter filter);
}
