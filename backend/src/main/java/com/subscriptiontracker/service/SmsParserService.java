package com.subscriptiontracker.service;

import com.subscriptiontracker.dto.response.SmsParseResponse;

public interface SmsParserService {

    SmsParseResponse parseSms(String smsText);
}
