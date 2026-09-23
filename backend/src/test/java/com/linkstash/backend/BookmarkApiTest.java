package com.linkstash.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookmarkApiTest extends BaseIntegrationTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUpUsers() throws Exception {
        tokenA = TestApi.register(this, "alice_a", "password1");
        tokenB = TestApi.register(this, "bob_b", "password1");
    }

    private long createBookmark(String token, String url, String title, String note, String... tagNames) throws Exception {
        String body = TestApi.toJson(TestApi.bookmarkBody(url, title, note, tagNames));
        var result = mockMvc.perform(post("/api/bookmarks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.status").value("unread"))
                .andExpect(jsonPath("$.data.favorite").value(false))
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    @Test
    void crudRoundTrip() throws Exception {
        long id = createBookmark(tokenA, "https://example.com/post", "Hello", "note here", "read");

        mockMvc.perform(get("/api/bookmarks/" + id)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value("https://example.com/post"))
                .andExpect(jsonPath("$.data.title").value("Hello"))
                .andExpect(jsonPath("$.data.note").value("note here"))
                .andExpect(jsonPath("$.data.tags[0].name").value("read"));

        ObjectNode patch = mapper.createObjectNode();
        patch.put("title", "Updated");
        patch.put("note", "n2");
        patch.put("status", "read");
        patch.put("favorite", true);

        mockMvc.perform(patch("/api/bookmarks/" + id)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patch.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated"))
                .andExpect(jsonPath("$.data.note").value("n2"))
                .andExpect(jsonPath("$.data.status").value("read"))
                .andExpect(jsonPath("$.data.favorite").value(true));

        mockMvc.perform(get("/api/bookmarks/" + id)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("read"))
                .andExpect(jsonPath("$.data.favorite").value(true));

        mockMvc.perform(delete("/api/bookmarks/" + id)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ok").value(true));

        mockMvc.perform(get("/api/bookmarks/" + id)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void listFiltersAndPagination() throws Exception {
        createBookmark(tokenA, "https://alpha.example.com/one", "Alpha title", "about cats", "animals");
        long bId = createBookmark(tokenA, "https://beta.example.com/two", "Beta title", "about dogs", "animals");
        long cId = createBookmark(tokenA, "https://gamma.example.com/three", "Gamma", "misc", "misc");

        mockMvc.perform(patch("/api/bookmarks/" + bId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"read\",\"favorite\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/bookmarks/" + cId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"archived\"}"))
                .andExpect(status().isOk());

        // status filter
        mockMvc.perform(get("/api/bookmarks")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("status", "unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("Alpha title"));

        mockMvc.perform(get("/api/bookmarks")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("status", "archived"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("Gamma"));

        // favorite filter
        mockMvc.perform(get("/api/bookmarks")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("favorite", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("Beta title"));

        // q search over title/description/url
        mockMvc.perform(get("/api/bookmarks")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("q", "Alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("Alpha title"));

        // default (no status) = inbox: unread+read, excludes archived
        mockMvc.perform(get("/api/bookmarks")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));

        // explicit status=all is not a valid API filter
        mockMvc.perform(get("/api/bookmarks")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("status", "all"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/bookmarks")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("q", "beta.example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("Beta title"));

        // tag filter by name
        mockMvc.perform(get("/api/bookmarks")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("tag", "animals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));

        // pagination over inbox (non-archived): Alpha + Beta = 2
        mockMvc.perform(get("/api/bookmarks")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(2));

        mockMvc.perform(get("/api/bookmarks")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("page", "2")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(0));

        // invalid status → 400
        mockMvc.perform(get("/api/bookmarks")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("status", "bogus"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(patch("/api/bookmarks/" + bId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"bogus\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void userIsolationReturns404() throws Exception {
        long id = createBookmark(tokenA, "https://secret.example.com/x", "Secret", "n");

        mockMvc.perform(get("/api/bookmarks/" + id)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(patch("/api/bookmarks/" + id)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"hacked\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/bookmarks/" + id)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // list is isolated
        mockMvc.perform(get("/api/bookmarks")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void createAcceptsTagNamesAndReturnsThem() throws Exception {
        var result = mockMvc.perform(post("/api/bookmarks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestApi.toJson(TestApi.bookmarkBody(
                                "https://example.com/tagged", "T", null, "work", "later"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tags.length()").value(2))
                .andReturn();

        JsonNode tags = mapper.readTree(result.getResponse().getContentAsString()).path("data").path("tags");
        assertThat(tags.findValuesAsText("name")).containsExactlyInAnyOrder("work", "later");
    }

    @Test
    void updateReplacesTagNamesWhenPresent() throws Exception {
        long id = createBookmark(tokenA, "https://example.com/replace-tags", "RT", "n", "old1", "old2");
        var result = mockMvc.perform(patch("/api/bookmarks/" + id)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tagNames\":[\"keep\",\"fresh\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tags.length()").value(2))
                .andReturn();
        JsonNode tags = mapper.readTree(result.getResponse().getContentAsString()).path("data").path("tags");
        assertThat(tags.findValuesAsText("name")).containsExactlyInAnyOrder("keep", "fresh");

        mockMvc.perform(get("/api/bookmarks/" + id)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tags.length()").value(2));
    }
}
