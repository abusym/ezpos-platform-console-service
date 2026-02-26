package net.ezpos.console.common.security.model

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

/**
 * 平台后台用户在 Spring Security 中的认证主体（Principal）。
 *
 * 该类实现 [UserDetails]，便于与 Spring Security 的认证/鉴权链路对接：
 * - `id`：业务侧识别用户的唯一标识
 * - `username`：用于展示/审计/日志等
 * - `password`：当前用于密码登录（BCrypt hash）；在 token 鉴权时不会直接使用
 *
 * 目前 `authorities` 返回空列表，表示尚未引入细粒度权限模型；后续可在此处装配角色/权限。
 */
data class PlatformUserPrincipal(
    val id: Long,
    private val passwordHash: String,
    private val enabled: Boolean,
    private val usernameValue: String,
) : UserDetails {
    /**
     * 返回该用户的权限集合。
     *
     * 当前实现为空（无角色/权限），只用于“已认证/未认证”维度的访问控制；
     * 若引入 RBAC/ABAC，可在这里返回对应的 [GrantedAuthority]。
     */
    override fun getAuthorities(): Collection<GrantedAuthority> =
        emptyList()

    /**
     * 返回密码哈希（用于密码登录流程）。
     *
     * 注意：这是哈希值而非明文密码，通常为 BCrypt hash。
     */
    override fun getPassword(): String = passwordHash

    /** 返回用户名（用于展示/审计/登录标识）。 */
    override fun getUsername(): String = usernameValue

    /** 是否未过期。当前未实现账号过期策略，恒为 true。 */
    override fun isAccountNonExpired(): Boolean = true

    /** 是否未锁定。当前未实现锁定策略，恒为 true。 */
    override fun isAccountNonLocked(): Boolean = true

    /** 凭据是否未过期。当前未实现凭据过期策略，恒为 true。 */
    override fun isCredentialsNonExpired(): Boolean = true

    /** 是否启用。与业务侧的用户启停用状态对齐。 */
    override fun isEnabled(): Boolean = enabled
}

