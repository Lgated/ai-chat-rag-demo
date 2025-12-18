package com.example.demo.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conversation")
@Data
@NoArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    //mappedBy = "conversation" : Message 实体里 必须 有一个名叫 conversation 的成员（@ManyToOne）
    //cascade = CascadeType.ALL : 对当前 Conversation 做的任何持久化操作（persist、merge、remove、refresh、detach）都会级联到它的每一条 Message 上。
    //                            举例：保存 Conversation 时会把列表里的新 Message 一并 insert；删除 Conversation 时会把所有 Message 一并 delete。
    //orphanRemoval = true : 把某条 Message 从 messages 列表里 摘掉（set(null) 或 remove()）并提交事务后，
    //                       JPA 会自动生成 delete SQL 把这条记录从数据库抹掉——俗称“孤儿删除”。不需要显式调用 em.remove(msg)。
    //fetch = FetchType.LAZY ： 只有第一次真正访问 conversation.getMessages() 时才会去数据库捞数据；否则不会主动 join/select。节省内存、查询量。
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Message> messages = new ArrayList<>();


}

