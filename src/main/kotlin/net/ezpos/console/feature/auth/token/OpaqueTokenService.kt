package net.ezpos.console.feature.auth.token

import net.ezpos.console.common.security.spi.TokenIntrospector
import net.ezpos.console.feature.auth.config.OpaqueTokenProperties
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.Base64

@Service
class OpaqueTokenService(
    private val redis: StringRedisTemplate,
    private val props: OpaqueTokenProperties,
) : TokenIntrospector {
    private val random = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    fun issueAccessToken(userId: Long): String {
        val token = generateToken()
        redis.opsForValue().set(key(token), userId.toString(), props.accessTokenTtl)
        return token
    }

    override fun resolveUserId(token: String): Long? {
        val raw = redis.opsForValue().get(key(token)) ?: return null
        val userId = raw.toLongOrNull() ?: return null
        redis.expire(key(token), props.accessTokenTtl)
        return userId
    }

    fun revoke(token: String) {
        redis.delete(key(token))
    }

    fun ttlSeconds(): Long = props.accessTokenTtl.seconds

    private fun key(token: String): String = props.redisKeyPrefix + token

    private fun generateToken(bytes: Int = 32): String {
        val buf = ByteArray(bytes)
        random.nextBytes(buf)
        return encoder.encodeToString(buf)
    }
}

