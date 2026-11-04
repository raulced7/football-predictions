package com.raulcedroj.football_predictions.group;

import com.raulcedroj.football_predictions.group.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse create(@AuthenticationPrincipal Jwt jwt,
                                @Valid @RequestBody CreateGroupRequest request) {
        return groupService.create(userId(jwt), request);
    }

    @PostMapping("/join")
    public GroupResponse join(@AuthenticationPrincipal Jwt jwt,
                              @Valid @RequestBody JoinGroupRequest request) {
        return groupService.join(userId(jwt), request);
    }

    @GetMapping
    public List<GroupResponse> findMyGroups(@AuthenticationPrincipal Jwt jwt) {
        return groupService.findMyGroups(userId(jwt));
    }

    @GetMapping("/{groupId}")
    public GroupDetailResponse findDetail(@AuthenticationPrincipal Jwt jwt, @PathVariable Long groupId) {
        return groupService.findDetail(userId(jwt), groupId);
    }

    @DeleteMapping("/{groupId}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@AuthenticationPrincipal Jwt jwt, @PathVariable Long groupId) {
        groupService.leave(userId(jwt), groupId);
    }

    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long groupId) {
        groupService.delete(userId(jwt), groupId);
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}