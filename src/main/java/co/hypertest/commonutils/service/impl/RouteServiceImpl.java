package co.hypertest.commonutils.service.impl;

import co.hypertest.commonutils.annotation.SkipHypertestTesting;
import co.hypertest.commonutils.dto.RouteResponse;
import co.hypertest.commonutils.service.RouteService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import co.hypertest.commonutils.helper.RequestFilter;

@Log4j2
@Service
public class RouteServiceImpl implements RouteService {

    @Autowired
    private ApplicationContext applicationContext;


    private List<String> getRoutesWithAnnotation(Class<? extends Annotation> annotationClass) {
        List<String> routes = new ArrayList<>();
        Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(RestController.class);

        for (Object controller : controllers.values()) {
            Class<?> targetClass = AopUtils.getTargetClass(controller); // resolves proxy
            Method[] methods = targetClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals("createPost")) {
                    System.out.println(method.getName());
                }
                // Exclude methods that have @SkipHyperTest annotation
                if (method.isAnnotationPresent(annotationClass)) {
                    String controllerPath = Optional.ofNullable(targetClass.getDeclaredAnnotation(RequestMapping.class))
                            .map(RequestMapping::value)
                            .filter(values -> values.length > 0)
                            .map(values -> values[0])
                            .orElse("");
                    String path = controllerPath + getRoutePath(method);
                    routes.add(path);
                }
            }
        }
        return routes;
    }

    private String getRoutePath(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) {
            GetMapping mapping = method.getAnnotation(GetMapping.class);
            return mapping.value().length > 0 ? mapping.value()[0] : "";
        } else {
            System.out.println("You have used annotation on a method that doesn't have GetMapping");
            System.out.println("Method: " + method.getName());
            return null;
        }
    }

    @Override
    public List<RouteResponse> getRoutes(String port, RequestFilter.Filter filter) {
        List<String> endpointsToSkip = getRoutesWithAnnotation(SkipHypertestTesting.class);
        List<RouteResponse> routes = new ArrayList<>();
        RestTemplate restTemplate = new RestTemplate();
        URI url;
        try {
            url = new URI("http://localhost:" + port + "/v3/api-docs");
        } catch (URISyntaxException e) {
            System.err.println("Failed to create URI");
            throw new RuntimeException(e);
        }
        String response = restTemplate.getForObject(url, String.class);
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(response);
            JsonNode pathsNode = root.path("paths");
            Iterator<Map.Entry<String, JsonNode>> fields = pathsNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String path = field.getKey();
                JsonNode methodsNode = field.getValue();
                Iterator<Map.Entry<String, JsonNode>> methods = methodsNode.fields();
                while (methods.hasNext()) {
                    try {
                        Map.Entry<String, JsonNode> method = methods.next();
                        String verb = method.getKey().toUpperCase();
                        co.hypertest.commonutils.enums.Method methodEnum = co.hypertest.commonutils.enums.Method.valueOf(verb);
                        if (path.equals("/get-routes-for-hyper-testing") || endpointsToSkip.stream().anyMatch(skip -> skip.equals(path)) || !methodEnum.equals(co.hypertest.commonutils.enums.Method.GET)) {
                            System.out.println("Skip path: " + path);
                            continue;
                        }
                        routes.add(new RouteResponse(methodEnum, path));
                    } catch (Exception e) {
                        System.err.println("Failed to parse the response from /v3/api-docs");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Suggestion - Check if the springdoc-openapi-starter-webmvc-ui dependency is added to the project");
            throw new RuntimeException("Failed to parse the response from /v3/api-docs", e);
        }

        if (filter != null) {
            routes = routes.stream().filter(filter::fn).collect(Collectors.toCollection(ArrayList::new));
        }

        return routes;
    }
}
