package com.clinic.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * 自定义UserDetails实现，包含用户额外信息
 */
@Getter
public class UserDetailsImpl extends User {

    private final Long userId;
    private final Long clinicId;
    private final String realName;

    public UserDetailsImpl(Long userId, String username, String password, Long clinicId, String realName,
                           Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.userId = userId;
        this.clinicId = clinicId;
        this.realName = realName;
    }

    public UserDetailsImpl(Long userId, String username, String password, boolean enabled,
                           boolean accountNonExpired, boolean credentialsNonExpired,
                           boolean accountNonLocked, Long clinicId, String realName,
                           Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.userId = userId;
        this.clinicId = clinicId;
        this.realName = realName;
    }
}
