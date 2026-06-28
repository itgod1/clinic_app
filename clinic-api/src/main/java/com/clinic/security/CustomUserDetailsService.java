package com.clinic.security;

import com.clinic.entity.SysRole;
import com.clinic.entity.SysUser;
import com.clinic.mapper.SysMenuMapper;
import com.clinic.mapper.SysRoleMapper;
import com.clinic.mapper.SysUserMapper;
import com.clinic.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysMenuMapper sysMenuMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("尝试加载用户: {}", username);
        SysUser user = sysUserMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .eq(SysUser::getStatus, 1)
        );

        if (user == null) {
            log.debug("用户不存在或已被禁用: {}", username);
            throw new UsernameNotFoundException("用户不存在或已被禁用: " + username);
        }

        log.debug("找到用户: {}, 角色ID: {}", username, user.getRoleId());

        // 加载用户权限
        List<SimpleGrantedAuthority> authorities = loadUserAuthorities(user.getId());

        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                true,
                true,
                true,
                true,
                user.getClinicId(),
                user.getRealName(),
                authorities
        );
    }

    /**
     * 加载用户权限
     */
    private List<SimpleGrantedAuthority> loadUserAuthorities(Long userId) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // 1. 加载角色
        List<SysRole> roles = sysRoleMapper.selectRolesByUserId(userId);
        for (SysRole role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleCode().toUpperCase()));
        }

        // 如果没有角色，默认给USER角色
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        // 2. 加载权限标识
        List<String> permissions = sysMenuMapper.selectPermissionsByUserId(userId);
        for (String permission : permissions) {
            if (permission != null && !permission.isEmpty()) {
                authorities.add(new SimpleGrantedAuthority(permission));
            }
        }

        log.debug("用户ID: {} 加载权限: {}", userId, authorities.stream()
                .map(SimpleGrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        return authorities;
    }
}
