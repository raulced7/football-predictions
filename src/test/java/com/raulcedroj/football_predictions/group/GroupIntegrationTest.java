package com.raulcedroj.football_predictions.group;

import com.jayway.jsonpath.JsonPath;
import com.raulcedroj.football_predictions.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GroupIntegrationTest extends IntegrationTestSupport {

    @Test
    void ownerCreatesGroupAndFriendJoinsWithInviteCode() throws Exception {
        String owner = tokenFor("owner-1@example.com", false);
        String friend = tokenFor("friend-1@example.com", false);

        String created = createGroup(owner, "Los de siempre")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberCount").value(1))
                .andExpect(jsonPath("$.competitionName").value("LaLiga"))
                .andExpect(jsonPath("$.inviteCode", matchesPattern("^[A-Z2-9]{8}$")))
                .andReturn().getResponse().getContentAsString();

        Integer groupId = JsonPath.read(created, "$.id");
        String inviteCode = JsonPath.read(created, "$.inviteCode");

        join(friend, inviteCode.toLowerCase())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberCount").value(2));

        // Unirse otra vez no falla ni duplica
        join(friend, inviteCode).andExpect(status().isOk())
                .andExpect(jsonPath("$.memberCount").value(2));

        mockMvc.perform(get("/api/groups").header("Authorization", "Bearer " + friend))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(groupId)).isNotEmpty());

        mockMvc.perform(get("/api/groups/{id}", groupId).header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members", hasSize(2)));
    }

    @Test
    void rejectsUnknownInviteCode() throws Exception {
        String user = tokenFor("lost-user@example.com", false);
        join(user, "ZZZZZZZZ").andExpect(status().isNotFound());
    }

    @Test
    void nonMembersCannotSeeGroup() throws Exception {
        String owner = tokenFor("owner-2@example.com", false);
        String stranger = tokenFor("stranger@example.com", false);

        Integer groupId = JsonPath.read(createGroup(owner, "Privado")
                .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/groups/{id}", groupId).header("Authorization", "Bearer " + stranger))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerCannotLeaveButCanDeleteAndMembersCannotDelete() throws Exception {
        String owner = tokenFor("owner-3@example.com", false);
        String friend = tokenFor("friend-3@example.com", false);

        String created = createGroup(owner, "Temporal").andReturn().getResponse().getContentAsString();
        Integer groupId = JsonPath.read(created, "$.id");
        join(friend, JsonPath.read(created, "$.inviteCode")).andExpect(status().isOk());

        mockMvc.perform(delete("/api/groups/{id}/members/me", groupId)
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/groups/{id}", groupId).header("Authorization", "Bearer " + friend))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/groups/{id}/members/me", groupId)
                        .header("Authorization", "Bearer " + friend))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/groups/{id}", groupId).header("Authorization", "Bearer " + owner))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/groups/{id}", groupId).header("Authorization", "Bearer " + owner))
                .andExpect(status().isNotFound());
    }
}