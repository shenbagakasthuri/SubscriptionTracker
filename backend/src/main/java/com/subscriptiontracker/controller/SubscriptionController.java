package com.subscriptiontracker.controller;

import com.subscriptiontracker.dto.request.SubscriptionRequest;
import com.subscriptiontracker.dto.response.ApiResponse;
import com.subscriptiontracker.dto.response.SubscriptionResponse;
import com.subscriptiontracker.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Subscriptions", description = "Endpoints for managing user subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    @Operation(summary = "Create a subscription", description = "Creates a new subscription associated with the authenticated user")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> createSubscription(
            @Valid @RequestBody SubscriptionRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        SubscriptionResponse response = subscriptionService.createSubscription(request, email);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Subscription created successfully", response));
    }

    @GetMapping
    @Operation(summary = "List subscriptions", description = "Retrieves all subscriptions owned by the authenticated user")
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> getAllSubscriptions(
            Authentication authentication) {
        String email = authentication.getName();
        List<SubscriptionResponse> subscriptions = subscriptionService.getUserSubscriptions(email);
        return ResponseEntity.ok(ApiResponse.success("Subscriptions retrieved successfully", subscriptions));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve one subscription", description = "Retrieves a specific subscription owned by the authenticated user")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getSubscriptionById(
            @PathVariable Long id,
            Authentication authentication) {
        String email = authentication.getName();
        SubscriptionResponse response = subscriptionService.getSubscriptionById(id, email);
        return ResponseEntity.ok(ApiResponse.success("Subscription retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a subscription", description = "Updates an existing subscription owned by the authenticated user")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> updateSubscription(
            @PathVariable Long id,
            @Valid @RequestBody SubscriptionRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        SubscriptionResponse response = subscriptionService.updateSubscription(id, request, email);
        return ResponseEntity.ok(ApiResponse.success("Subscription updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a subscription", description = "Deletes a subscription owned by the authenticated user")
    public ResponseEntity<Void> deleteSubscription(
            @PathVariable Long id,
            Authentication authentication) {
        String email = authentication.getName();
        subscriptionService.deleteSubscription(id, email);
        return ResponseEntity.noContent().build();
    }
}
