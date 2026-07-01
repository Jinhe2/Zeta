package com.zeta.screen.wiring;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

/** 端子连线判据通道对，对应 ct-screen.terminal_wiring_criterion_pair */
@Entity
@Table(name = "terminal_wiring_criterion_pair")
@Getter
@Setter
@NoArgsConstructor
public class TerminalWiringCriterionPair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criterion_id", nullable = false)
    private TerminalWiringCriterion criterion;

    @Column(name = "input_channel_no", nullable = false)
    private Integer inputChannelNo;

    @Column(name = "output_channel_no", nullable = false)
    private Integer outputChannelNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_mode", nullable = false)
    private MatchMode matchMode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum MatchMode {
        SATISFIED, NOT_SATISFIED
    }
}
