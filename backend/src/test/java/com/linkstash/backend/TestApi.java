package com.linkstash.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TestApi {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestApi() {
    }

    static String register(BaseIntegrationTest test, String username, String password) throws Exception {
        MvcResult result = test.mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = MAPPER.readTree(result.getResponse().getContentAsString());
        return node.path("data").path("token").asText();
    }

    static ObjectNode bookmarkBody(String url, String title, String note, String... tagNames) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("url", url);
        if (title != null) {
            node.put("title", title);
        }
        if (note != null) {
            node.put("note", note);
        }
        if (tagNames != null && tagNames.length > 0) {
            ArrayNode tags = node.putArray("tagNames");
            for (String t : tagNames) {
                tags.add(t);
            }
        }
        return node;
    }

    static String toJson(Object node) {
        return node.toString();
    }
}
