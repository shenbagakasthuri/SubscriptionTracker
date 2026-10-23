package com.subscriptiontracker.service;

import com.subscriptiontracker.dto.request.SmsParseRequest;
import com.subscriptiontracker.dto.response.SmsTransactionResponse;
import com.subscriptiontracker.dto.response.SubscriptionSuggestionResponse;

import java.util.List;

public interface SmsTransactionService {

    SmsTransactionResponse importTransaction(SmsParseRequest request, String userEmail);

    List<SmsTransactionResponse> getUserTransactions(String userEmail);

    SmsTransactionResponse getTransactionById(Long id, String userEmail);

    List<SubscriptionSuggestionResponse> getSubscriptionSuggestions(String userEmail);
}
