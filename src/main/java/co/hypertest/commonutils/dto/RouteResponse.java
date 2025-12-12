package co.hypertest.commonutils.dto;

import co.hypertest.commonutils.enums.Method;

public class RouteResponse {
    private Method reqVerb;
    private String reqPath;

    public RouteResponse() {
    }

    public RouteResponse(Method reqVerb, String reqPath) {
        this.reqVerb = reqVerb;
        this.reqPath = reqPath;
    }

    public Method getReqVerb() {
        return reqVerb;
    }

    public void setReqVerb(Method reqVerb) {
        this.reqVerb = reqVerb;
    }

    public String getReqPath() {
        return reqPath;
    }

    public void setReqPath(String reqPath) {
        this.reqPath = reqPath;
    }
}
