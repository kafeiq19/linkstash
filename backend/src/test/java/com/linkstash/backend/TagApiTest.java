package com.linkstash.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TagApiTest extends BaseIntegrationTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUpUsers() throws Exception {
        tokenA = TestApi.register(this, "tag_user_a", "password1");
        tokenB = TestApi.register(this, "tag_user_b", "password1");
    }

    private long createTag(String token, String name) throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("name", name);
        var result = mockMvc.perform(post("/api/tags")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value(name.trim()))
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    @Test
    void createRenameDeleteAndUniqueName() throws Exception {
        long id = createTag(tokenA, "  reading  ");

        // duplicate name → 400
        mockMvc.perform(post("/api/tags")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"reading\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        // same name allowed for another user
        createTag(tokenB, "reading");

        // rename ok
        ObjectNode rename = mapper.createObjectNode();
        rename.put("name", "to-read");
        mockMvc.perform(patch("/api/tags/" + id)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rename.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("to-read"));

        // rename onto existing → 400
        createTag(tokenA, "archive");
        rename.put("name", "archive");
        mockMvc.perform(patch("/api/tags/" + id)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rename.toString()))
                .andExpect(status().isBadRequest());

        // invalid name length
        mockMvc.perform(post("/api/tags")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());

        // delete
        mockMvc.perform(delete("/api/tags/" + id)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ok").value(true));

        mockMvc.perform(patch("/api/tags/" + id)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ghost\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTagUnlinksBookmarksAndCounts() throws Exception {
        long tagId = createTag(tokenA, "linked");

        mockMvc.perform(post("/api/bookmarks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestApi.toJson(TestApi.bookmarkBody(
                                "https://example.com/1", "B1", null, "linked"))))
                .andExpect(status().isOk());
        var created = mockMvc.perform(post("/api/bookmarks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestApi.toJson(TestApi.bookmarkBody(
                                "https://example.com/2", "B2", null, "linked"))))
                .andExpect(status().isOk())
                .andReturn();
        long bookmarkId = mapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // tag list shows count
        mockMvc.perform(get("/api/tags")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("linked"))
                .andExpect(jsonPath("$.data[0].count").value(2));

        // delete tag unlinks
        mockMvc.perform(delete("/api/tags/" + tagId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // bookmark remains, tags empty
        mockMvc.perform(get("/api/bookmarks/" + bookmarkId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tags.length()").value(0));

        mockMvc.perform(get("/api/tags")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void tagIsolationReturns404() throws Exception {
        long id = createTag(tokenA, "private-tag");

        mockMvc.perform(patch("/api/tags/" + id)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"stolen\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/tags/" + id)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }
}
