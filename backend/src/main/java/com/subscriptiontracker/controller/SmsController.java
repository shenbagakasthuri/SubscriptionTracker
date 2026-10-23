package com.subscriptiontracker.controller;

import com.subscriptiontracker.dto.request.SmsParseRequest;
import com.subscriptiontracker.dto.response.ApiResponse;
import com.subscriptiontracker.dto.response.SmsParseResponse;
import com.subscriptiontracker.dto.response.SmsTransactionResponse;
import com.subscriptiontracker.dto.response.SubscriptionSuggestionResponse;
import com.subscriptiontracker.service.SmsParserService;
import com.subscriptiontracker.service.SmsTransactionService;
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
@RequestMapping("/api/sms")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "SMS Transactions", description = "Endpoints for parsing debit SMS messages and managing imported transactions")
public class SmsController {

    private final SmsParserService smsParserService;
    private final SmsTransactionService smsTransactionService;

    @PostMapping("/parse")
    @Operation(summary = "Parse SMS preview", description = "Parses an SMS debit message and returns a preview without saving")
    public ResponseEntity<ApiResponse<SmsParseResponse>> parseSms(
            @Valid @RequestBody SmsParseRequest request) {
        SmsParseResponse response = smsParserService.parseSms(request.getSmsText());
        return ResponseEntity.ok(ApiResponse.success("SMS parsed successfully", response));
    }

    @PostMapping("/import")
    @Operation(summary = "Import a parsed debit transaction", description = "Parses and saves a valid debit transaction for the authenticated user")
    public ResponseEntity<ApiResponse<SmsTransactionResponse>> importTransaction(
            @Valid @RequestBody SmsParseRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        SmsTransactionResponse response = smsTransactionService.importTransaction(request, email);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transaction imported successfully", response));
    }

    @GetMapping("/imports")
    @Operation(summary = "List imported transactions", description = "Retrieves all transactions imported by the authenticated user")
    public ResponseEntity<ApiResponse<List<SmsTransactionResponse>>> getImportedTransactions(
            Authentication authentication) {
        String email = authentication.getName();
        List<SmsTransactionResponse> transactions = smsTransactionService.getUserTransactions(email);
        return ResponseEntity.ok(ApiResponse.success("Imported transactions retrieved successfully", transactions));
    }

    @GetMapping("/imports/{id}")
    @Operation(summary = "Get an imported transaction", description = "Retrieves a specific transaction owned by the authenticated user")
    public ResponseEntity<ApiResponse<SmsTransactionResponse>> getTransactionById(
            @PathVariable Long id,
            Authentication authentication) {
        String email = authentication.getName();
        SmsTransactionResponse transaction = smsTransactionService.getTransactionById(id, email);
        return ResponseEntity.ok(ApiResponse.success("Transaction retrieved successfully", transaction));
    }

    @GetMapping("/subscription-suggestions")
    @Operation(summary = "Identify possible recurring payments", description = "Analyzes imported transactions to identify potential recurring subscriptions")
    public ResponseEntity<ApiResponse<List<SubscriptionSuggestionResponse>>> getSubscriptionSuggestions(
            Authentication authentication) {
        String email = authentication.getName();
        List<SubscriptionSuggestionResponse> suggestions = smsTransactionService.getSubscriptionSuggestions(email);
        return ResponseEntity.ok(ApiResponse.success("Subscription suggestions generated successfully", suggestions));
    }
}
