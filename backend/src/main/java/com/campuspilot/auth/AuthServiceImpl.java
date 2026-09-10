package com.campuspilot.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campuspilot.dto.LoginRequest;
import com.campuspilot.dto.LoginResponse;
import com.campuspilot.entity.User;
import com.campuspilot.mapper.UserMapper;
import com.campuspilot.security.JwtTokenProvider;
import com.campuspilot.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    
    @Override
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
                .eq(User::getIsDeleted, 0)
        );
        
        String token = jwtTokenProvider.generateToken(authentication);
        
        UserVO userVO = UserVO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .role(user.getRole())
            .status(user.getStatus())
            .createdAt(user.getCreatedAt())
            .build();
        
        return LoginResponse.builder()
            .token(token)
            .user(userVO)
            .build();
    }
}