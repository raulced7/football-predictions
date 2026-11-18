package com.raulcedroj.football_predictions.avatar;

import com.jayway.jsonpath.JsonPath;
import com.raulcedroj.football_predictions.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AvatarIntegrationTest extends IntegrationTestSupport {

    @MockitoBean
    private AvatarStorage storage;

    @BeforeEach
    void storageReturnsAUrlBasedOnTheKey() {
        when(storage.store(anyString(), any())).thenAnswer(call -> "https://img.test/" + call.getArgument(0));
    }

    @Test
    void uploadsAPhotoAndShowsItInTheProfileAndTheGroup() throws Exception {
        String token = tokenFor("avatar-ok@example.com", false);

        String url = JsonPath.read(upload(token, png())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.avatarUrl");

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.avatarUrl").value(url));

        String created = createGroup(token, "Fotos").andReturn().getResponse().getContentAsString();
        Integer groupId = JsonPath.read(created, "$.id");
        mockMvc.perform(get("/api/groups/{id}", groupId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.members[0].avatarUrl").value(url));
        mockMvc.perform(get("/api/groups/{id}/standings", groupId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].avatarUrl").value(url));
    }

    @Test
    void replacingThePhotoDeletesThePreviousOne() throws Exception {
        String token = tokenFor("avatar-replace@example.com", false);
        String first = JsonPath.read(upload(token, png()).andReturn().getResponse().getContentAsString(), "$.avatarUrl");

        upload(token, png()).andExpect(status().isOk());

        verify(storage).delete(first);
    }

    @Test
    void removesThePhoto() throws Exception {
        String token = tokenFor("avatar-remove@example.com", false);
        String url = JsonPath.read(upload(token, png()).andReturn().getResponse().getContentAsString(), "$.avatarUrl");

        mockMvc.perform(delete("/api/users/me/avatar").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        verify(storage).delete(url);
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.avatarUrl").value(nullValue()));
    }

    @Test
    void rejectsFilesThatAreNotImages() throws Exception {
        String token = tokenFor("avatar-invalid@example.com", false);

        upload(token, new MockMultipartFile("file", "virus.jpg", "image/jpeg", "no soy una foto".getBytes()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid image"));

        verify(storage, never()).store(anyString(), any());
    }

    @Test
    void onlyAdminsCanRemoveSomeoneElsesPhoto() throws Exception {
        String admin = tokenFor("avatar-admin@example.com", true);
        String user = tokenFor("avatar-user@example.com", false);
        String other = tokenFor("avatar-other@example.com", false);
        upload(user, png()).andExpect(status().isOk());
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM app_user WHERE email = ?", Long.class,
                "avatar-user@example.com");

        mockMvc.perform(delete("/api/admin/users/{id}/avatar", userId).header("Authorization", "Bearer " + other))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/admin/users/{id}/avatar", userId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + user))
                .andExpect(jsonPath("$.avatarUrl").value(nullValue()));
    }

    private ResultActions upload(String token, MockMultipartFile file) throws Exception {
        return mockMvc.perform(multipart("/api/users/me/avatar")
                .file(file)
                .header("Authorization", "Bearer " + token));
    }

    private static MockMultipartFile png() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB), "png", bytes);
        return new MockMultipartFile("file", "foto.png", "image/png", bytes.toByteArray());
    }
}
