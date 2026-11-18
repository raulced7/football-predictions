package com.raulcedroj.football_predictions.user;

import com.jayway.jsonpath.JsonPath;
import com.raulcedroj.football_predictions.avatar.AvatarStorage;
import com.raulcedroj.football_predictions.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountDeletionIntegrationTest extends IntegrationTestSupport {

    @MockitoBean
    private AvatarStorage storage;

    @Test
    void deletesTheAccountAndHandsTheGroupsToTheOldestMember() throws Exception {
        String owner = tokenFor("delete-owner@example.com", false);
        String friend = tokenFor("delete-friend@example.com", false);
        String created = createGroup(owner, "Con amigos").andReturn().getResponse().getContentAsString();
        Integer sharedGroup = JsonPath.read(created, "$.id");
        join(friend, JsonPath.read(created, "$.inviteCode")).andExpect(status().isOk());
        Integer soloGroup = JsonPath.read(createGroup(owner, "Solo yo").andReturn().getResponse().getContentAsString(),
                "$.id");
        jdbcTemplate.update("UPDATE app_user SET avatar_url = 'https://img.test/1/foto.jpg' WHERE email = ?",
                "delete-owner@example.com");

        deleteAccount(owner, "password123").andExpect(status().isNoContent());

        assertThat(count("SELECT count(*) FROM app_user WHERE email = 'delete-owner@example.com'")).isZero();
        // El grupo con más gente pasa al amigo; el grupo en el que estaba solo desaparece
        assertThat(jdbcTemplate.queryForObject("""
                SELECT u.email FROM friend_group g JOIN app_user u ON u.id = g.owner_id WHERE g.id = ?
                """, String.class, sharedGroup)).isEqualTo("delete-friend@example.com");
        assertThat(count("SELECT count(*) FROM friend_group WHERE id = " + soloGroup)).isZero();
        verify(storage).delete("https://img.test/1/foto.jpg");

        login("delete-owner@example.com").andExpect(status().isUnauthorized());
    }

    @Test
    void requiresTheCurrentPassword() throws Exception {
        String token = tokenFor("delete-wrong@example.com", false);

        deleteAccount(token, "wrong-password").andExpect(status().isBadRequest());

        login("delete-wrong@example.com").andExpect(status().isOk());
    }

    private ResultActions deleteAccount(String token, String password) throws Exception {
        return mockMvc.perform(delete("/api/users/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "password": "%s" }
                        """.formatted(password)));
    }

    private ResultActions login(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "email": "%s", "password": "password123" }
                        """.formatted(email)));
    }

    private Integer count(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
