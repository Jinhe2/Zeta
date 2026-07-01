package com.zeta.screen.terminal;

import com.zeta.screen.cabinet.Cabinet;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

/** 端子排，对应 ct-screen.terminal_strip */
@Entity
@Table(name = "terminal_strip")
@Getter
@Setter
@NoArgsConstructor
public class TerminalStrip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cabinet_id", nullable = false)
    private Cabinet cabinet;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "label_prefix", nullable = false, length = 64)
    private String labelPrefix;

    @Column(name = "function_desc", length = 256)
    private String functionDesc;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Lob
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
