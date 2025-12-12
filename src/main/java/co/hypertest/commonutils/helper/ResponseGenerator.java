package co.hypertest.commonutils.helper;

import co.hypertest.commonutils.dto.HtPayload;

import static hypertest.javaagent.bootstrap.util.StringConstantsUtils.IS_INSTRUMENTATION_TESTING_ENABLED;

public class ResponseGenerator {
    public static Object generate(Object obj) {
        if (IS_INSTRUMENTATION_TESTING_ENABLED) {
            return new HtPayload(obj);
        } else {
            return obj;
        }
    }
}
