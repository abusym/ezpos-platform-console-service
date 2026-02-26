package net.ezpos.console.common.entity.id

import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.id.IdentifierGenerator
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.Serializable
import java.time.Instant

/**
 * Snowflake 风格的 `Long` 主键生成器（Hibernate `IdentifierGenerator`）。
 *
 * ## ID 结构（本实现）
 * 该实现将 ID 按位拆分为：
 *
 * - timestamp：毫秒时间戳偏移（`currentTimeMillis - epoch`）
 * - machineId：机器/实例 ID（需要落在可用位宽内）
 * - sequence：同一毫秒内的序列号
 *
 * 位宽由常量定义（见 companion object）：
 * - `MACHINE_BITS = 5`（支持 0..31）
 * - `SEQUENCE_BITS = 8`（同一毫秒最多 256 个 ID）
 *
 * ## 并发与一致性
 * - `nextId` 使用 `@Synchronized`，确保单 JVM 内生成过程串行，避免竞争导致重复。
 * - `lastTimestamp/sequence` 放在 `companion object`，保证多个生成器实例共享同一状态（对“同 JVM 多 Session/多 Bean”仍安全）。
 *
 * ## 时钟回拨处理
 * 若检测到当前时间小于上次生成时间（时钟回拨），该实现会将 `currentTimestamp` 强制推进到 `lastTimestamp + 1`，
 * 以保证单 JVM 内 ID 单调递增且不重复（代价是 ID 的时间部分可能“领先真实时间”一点点）。
 *
 * ## 配置
 * - `snowflake.machine-id`：机器/实例 ID（默认 1）
 * - `snowflake.epoch`：自定义纪元毫秒值（默认 `1735689600000`）
 *
 * 注意：`machineId` 应确保在 \(0..2^{MACHINE_BITS}-1\) 范围内，否则高位会溢出影响 ID 结构。
 */
@Component("snowflakeIdGenerator")
class SnowflakeIdGenerator(
    @Value("\${snowflake.machine-id:1}")
    private val machineId: Long,

    @Value("\${snowflake.epoch:1735689600000}")
    private val epoch: Long,
) : IdentifierGenerator {

    companion object {
        /** 机器 ID 位宽（默认 5 bits，可表达 0..31）。 */
        private const val MACHINE_BITS = 5
        /** 同一毫秒序列位宽（默认 8 bits，可表达 0..255）。 */
        private const val SEQUENCE_BITS = 8

        /** 同一毫秒内序列号最大值（全 1 掩码）。 */
        private const val MAX_SEQUENCE = (1 shl SEQUENCE_BITS) - 1

        /** 机器 ID 左移偏移：低位先放 sequence，再放 machineId。 */
        private const val MACHINE_SHIFT = SEQUENCE_BITS
        /** 时间戳部分左移偏移：machineId + sequence 的总位宽。 */
        private const val TIMESTAMP_SHIFT = MACHINE_BITS + SEQUENCE_BITS

        /**
         * 上次生成 ID 时的毫秒时间戳（用于检测同毫秒/时钟回拨）。
         *
         * `@Volatile` 用于保证多线程可见性（同时 `nextId` 也做了同步）。
         */
        @Volatile
        private var lastTimestamp = -1L

        /** 同一毫秒内的序列号计数器。 */
        private var sequence = 0L

        /**
         * 生成下一个 ID。
         *
         * @param epoch 自定义纪元（毫秒），用于缩短 timestamp 部分并延长可用年限
         * @param machineId 机器/实例 ID（应在可用位宽内）
         * @return 唯一 `Long` ID（单 JVM 内保证不重复）
         */
        @Synchronized
        fun nextId(epoch: Long, machineId: Long): Long {
            var currentTimestamp = currentTime()

            if (currentTimestamp < lastTimestamp) {
                currentTimestamp = lastTimestamp + 1
            }

            if (currentTimestamp == lastTimestamp) {
                sequence = (sequence + 1) and MAX_SEQUENCE.toLong()
                if (sequence == 0L) {
                    currentTimestamp = waitNextMillis(currentTimestamp)
                }
            } else {
                sequence = 0L
            }

            lastTimestamp = currentTimestamp

            return ((currentTimestamp - epoch) shl TIMESTAMP_SHIFT) or
                (machineId shl MACHINE_SHIFT) or
                sequence
        }

        /** 获取当前毫秒时间戳。单独抽取便于测试/替换。 */
        private fun currentTime(): Long = Instant.now().toEpochMilli()

        /**
         * 忙等到下一毫秒。
         *
         * 当同一毫秒内 sequence 溢出（达到 MAX_SEQUENCE）时，需要等待时间推进以保证不重复。
         */
        private fun waitNextMillis(lastTimestamp: Long): Long {
            var timestamp = currentTime()
            while (timestamp <= lastTimestamp) {
                timestamp = currentTime()
            }
            return timestamp
        }
    }

    /**
     * Hibernate 回调生成主键。
     *
     * @return 由 [nextId] 生成的 `Long`，以 [Serializable] 形式返回给 Hibernate
     */
    override fun generate(
        session: SharedSessionContractImplementor?,
        obj: Any?,
    ): Serializable = nextId(epoch, machineId)
}

