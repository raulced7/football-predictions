package com.raulcedroj.football_predictions.group;

import com.jayway.jsonpath.JsonPath;
import com.raulcedroj.football_predictions.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "app.groups.max-members=2",
        "app.groups.max-owned-per-user=1"
})
class GroupLimitsIntegrationTest extends IntegrationTestSupport {

    @Test
    void rejectsJoiningFullGroup() throws Exception {
        String owner = tokenFor("owner-limit@example.com", false);
        String second = tokenFor("second-limit@example.com", false);
        String third = tokenFor("third-limit@example.com", false);

        String inviteCode = JsonPath.read(createGroup(owner, "Completo")
                .andReturn().getResponse().getContentAsString(), "$.inviteCode");

        join(second, inviteCode).andExpect(status().isOk());
        join(third, inviteCode).andExpect(status().isConflict());
    }

    @Test
    void rejectsCreatingMoreGroupsThanAllowed() throws Exception {
        String owner = tokenFor("owner-many@example.com", false);

        createGroup(owner, "Primero").andExpect(status().isCreated());
        createGroup(owner, "Segundo").andExpect(status().isConflict());
    }
}