package com.subscriptiontracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsParseRequest {

    @NotBlank(message = "SMS text is required")
    @Size(max = 1000, message = "SMS text must not exceed 1000 characters")
    private String smsText;
}
