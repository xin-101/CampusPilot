package com.campuspilot.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campuspilot.entity.User;
import com.campuspilot.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    
    private final UserMapper userMapper;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getIsDeleted, 0)
        );
        
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        
        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }
}