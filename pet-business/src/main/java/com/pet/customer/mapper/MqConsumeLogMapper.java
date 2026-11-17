package com.pet.customer.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MQ 消费幂等台账（映射表 mq_consume_log_wsh）。
 *
 * <p>为什么需要它：消费端是「先写通知、再 basicAck」。如果进程在两步之间崩溃，
 * 消息会被 Broker 重新投递，通知就会重复插入。仅靠内存 Set 无法跨实例、也无法跨重启。</p>
 *
 * <p>做法：消费前先用 (consumer, eventKey) 抢占一条台账记录，唯一索引保证只有一次能插进去；
 * 插入成功才继续业务，冲突则说明这条事件已经处理过，直接跳过。</p>
 *
 * <p>注意：这里用普通 INSERT 而不是 MySQL 专有的 INSERT IGNORE / ON DUPLICATE KEY，
 * 让调用方用 Spring 的 DuplicateKeyException 统一判断，避免把方言写死在 SQL 里。</p>
 */
@Mapper
public interface MqConsumeLogMapper {

    /**
     * 抢占一条消费记录。
     *
     * @param consumer 消费者标识，例如 complaint-process
     * @param eventKey 业务事件唯一键，例如 12:resolved
     * @return 插入行数（1 表示抢到）
     */
    @Insert("INSERT INTO mq_consume_log_wsh (consumer_wsh, event_key_wsh, created_at_wsh) "
            + "VALUES (#{consumer}, #{eventKey}, NOW())")
    int insertClaim(@Param("consumer") String consumer, @Param("eventKey") String eventKey);

    /**
     * 释放消费记录。
     *
     * <p>业务处理失败时调用：让这条事件在下次投递时还能被重新处理，
     * 否则一次失败会被台账永久判定为「已处理」，通知就永远发不出去。</p>
     *
     * @param consumer 消费者标识
     * @param eventKey 业务事件唯一键
     * @return 删除行数
     */
    @Delete("DELETE FROM mq_consume_log_wsh WHERE consumer_wsh = #{consumer} AND event_key_wsh = #{eventKey}")
    int releaseClaim(@Param("consumer") String consumer, @Param("eventKey") String eventKey);
}
