package com.scoder.jusic.common.page;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HulkPageTest {

    @Test
    void testHulkPageJacksonDeserializationWithUnknownFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        String jsonWithLimitEnd = "{\"pageIndex\":1,\"pageSize\":10,\"totalSize\":100,\"totalPage\":10,\"currentPage\":1,\"limitStart\":0,\"limitEnd\":10}";
        HulkPage<?> deserialized = mapper.readValue(jsonWithLimitEnd, HulkPage.class);

        assertNotNull(deserialized);
        assertEquals(1, deserialized.getPageIndex());
        assertEquals(10, deserialized.getPageSize());

        String reserializedJson = mapper.writeValueAsString(deserialized);
        assertTrue(reserializedJson.contains("\"limitEnd\":10"));
        assertTrue(reserializedJson.contains("\"limitStart\":0"));
    }
}
