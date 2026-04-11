package com.skybooker.auth.dto;

import com.skybooker.auth.enums.AuthProvider;
import com.skybooker.auth.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSummaryResponse {
    private String userId;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private AuthProvider provider;
    private Boolean active;
    private String passportNumber;
    private String nationality;
}
