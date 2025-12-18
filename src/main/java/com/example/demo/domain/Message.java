package com.example.demo.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "message")
@Data
@NoArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) //指定主键的「自动生成策略」，即主键值由数据库自动生成，无需手动设置
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 多对1配置 -- 懒加载配置
    //查询Message时，不会立即查询关联的Conversation，只有调用message.getConversation()时才会触发sql查询
    @JoinColumn(name = "conversation_id", nullable = false)
    @JsonIgnore // 标记的字段不会被序列化
    //             Jackson 不会调用 getMessages()
    //             代理对象不会被触发初始化
    //             不会抛出异常
    //指定外键的映射规则，就是message表中关联conversation表的外键列。
    private Conversation conversation;

    @Column(nullable = false, length = 20)
    private String role;        // "user" / "assistant"

    @Column(columnDefinition = "text", nullable = false)
    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

}

