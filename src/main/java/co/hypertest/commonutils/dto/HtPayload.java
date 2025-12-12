package co.hypertest.commonutils.dto;

import hypertest.javaagent.bootstrap.jsonschema.JsonSchemaGenerator;
import hypertest.javaagent.bootstrap.jsonschema.entity.JsonSchema;

import java.util.ArrayList;
import java.util.List;

public class HtPayload {
    private JsonSchema responseSchema;
    private IgnoredDiffs ignoredDiffs;

    public HtPayload(Object obj) {
        try {
            this.responseSchema = JsonSchemaGenerator.generateSchema(obj, true);
            this.ignoredDiffs = new IgnoredDiffs();
        } catch (IllegalAccessException e) {
            System.err.println("Error while generating schema");
            throw new RuntimeException(e);
        }
    }

    public HtPayload(JsonSchema responseSchema, IgnoredDiffs ignoredDiffs) {
        this.responseSchema = responseSchema;
        this.ignoredDiffs = ignoredDiffs;
    }

    public JsonSchema getResponseSchema() {
        return responseSchema;
    }

    public void setResponseSchema(JsonSchema responseSchema) {
        this.responseSchema = responseSchema;
    }

    public IgnoredDiffs getIgnoredDiffs() {
        return ignoredDiffs;
    }

    public void setIgnoredDiffs(IgnoredDiffs ignoredDiffs) {
        this.ignoredDiffs = ignoredDiffs;
    }

    private static class IgnoredDiffs {
        private List<Object> disabledVsRecord = new ArrayList<>();
        private List<Object> recordVsReplay = new ArrayList<>();

        public List<Object> getDisabledVsRecord() {
            return disabledVsRecord;
        }

        public void setDisabledVsRecord(List<Object> disabledVsRecord) {
            this.disabledVsRecord = disabledVsRecord;
        }

        public List<Object> getRecordVsReplay() {
            return recordVsReplay;
        }

        public void setRecordVsReplay(List<Object> recordVsReplay) {
            this.recordVsReplay = recordVsReplay;
        }
    }

}
