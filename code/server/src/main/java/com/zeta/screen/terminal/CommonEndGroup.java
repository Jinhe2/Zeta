package com.zeta.screen.terminal;

import com.zeta.screen.cabinet.Cabinet;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

/** 公共端组，对应 ct-screen.common_end_group */
@Entity
@Table(name = "common_end_group")
@Getter
@Setter
@NoArgsConstructor
public class CommonEndGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cabinet_id", nullable = false)
    private Cabinet cabinet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "terminal_strip_id", nullable = false)
    private TerminalStrip terminalStrip;

    @Column(nullable = false, length = 128)
    private String name;

    @Lob
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
