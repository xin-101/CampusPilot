package com.campuspilot.auth;

import com.campuspilot.dto.LoginRequest;
import com.campuspilot.dto.LoginResponse;

public interface AuthService {
    
    LoginResponse login(LoginRequest request);
}