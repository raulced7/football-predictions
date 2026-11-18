package com.raulcedroj.football_predictions.avatar;

import com.raulcedroj.football_predictions.user.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
public class AvatarController {

    private final AvatarService avatarService;

    public AvatarController(AvatarService avatarService) {
        this.avatarService = avatarService;
    }

    @PostMapping(path = "/api/users/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse upload(@AuthenticationPrincipal Jwt jwt, @RequestParam("file") MultipartFile file)
            throws IOException {
        if (file.isEmpty()) {
            throw new InvalidImageException("Elige una foto.");
        }
        return avatarService.upload(Long.valueOf(jwt.getSubject()), file.getBytes());
    }

    @DeleteMapping("/api/users/me/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal Jwt jwt) {
        avatarService.remove(Long.valueOf(jwt.getSubject()));
    }

    /** Para que un administrador pueda quitar una foto inapropiada. */
    @DeleteMapping("/api/admin/users/{userId}/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAsAdmin(@PathVariable Long userId) {
        avatarService.remove(userId);
    }
}
