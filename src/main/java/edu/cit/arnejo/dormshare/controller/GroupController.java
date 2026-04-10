package edu.cit.arnejo.dormshare.controller;

import edu.cit.arnejo.dormshare.dto.ApiResponse;
import edu.cit.arnejo.dormshare.dto.GroupRequest;
import edu.cit.arnejo.dormshare.entity.UserEntity;
import edu.cit.arnejo.dormshare.service.GroupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    /**
     * POST /api/groups
     * Create a new group. The authenticated user becomes the ADMIN.
     */
    @PostMapping
    public ResponseEntity<ApiResponse> createGroup(
            @RequestBody GroupRequest request,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }
        ApiResponse result = groupService.createGroup(request.getName(), user.getId());
        if (result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    /**
     * POST /api/groups/join
     * Join an existing group using an invite code.
     */
    @PostMapping("/join")
    public ResponseEntity<ApiResponse> joinGroup(
            @RequestBody GroupRequest request,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }
        ApiResponse result = groupService.joinGroup(request.getInviteCode(), user.getId());
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    /**
     * DELETE /api/groups/leave/{groupId}
     * Leave a specific group.
     */
    @DeleteMapping("/leave/{groupId}")
    public ResponseEntity<ApiResponse> leaveGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }
        ApiResponse result = groupService.leaveGroup(groupId, user.getId());
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    /**
     * GET /api/groups/my
     * Get all groups the user belongs to.
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse> getMyGroups(@AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }
        ApiResponse result = groupService.getMyGroups(user.getId());
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/groups/{groupId}
     * Get a specific group's details.
     */
    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse> getGroupById(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH-001", "Unauthorized", "You must be logged in"));
        }
        ApiResponse result = groupService.getGroupById(groupId, user.getId());
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
    }
}
