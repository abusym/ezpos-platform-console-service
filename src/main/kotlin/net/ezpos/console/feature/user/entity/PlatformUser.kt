package net.ezpos.console.feature.user.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import net.ezpos.console.common.entity.base.IdEntity
import org.hibernate.annotations.UpdateTimestamp
import java.time.OffsetDateTime

@Entity
@Table(
    name = "platform_users",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_platform_users_username", columnNames = ["username"]),
    ],
    indexes = [
        Index(name = "idx_platform_users_username", columnList = "username"),
    ],
)
class PlatformUser(
    @Column(name = "username", nullable = false, length = 64)
    var username: String,

    @Column(name = "password_hash", nullable = true, length = 100)
    var passwordHash: String,

    @Column(name = "display_name", length = 128)
    var displayName: String? = null,

    @Column(name = "email", length = 128)
    var email: String? = null,

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true,
) : IdEntity() {
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    var updatedAt: OffsetDateTime? = null
}

