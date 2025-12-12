package co.hypertest.commonutils.helper;

import co.hypertest.commonutils.dto.RouteResponse;

public class RequestFilter {

    public interface Filter {
        public boolean fn (RouteResponse routeResponse);
    }

    // Changed from Public to Private so access is forced through the getter
    private static Filter filter = null;

    // Manual Getter
    public static Filter getFilter() {
        return filter;
    }

    // Manual Setter
    public static void setFilter(Filter filter) {
        RequestFilter.filter = filter;
    }
}