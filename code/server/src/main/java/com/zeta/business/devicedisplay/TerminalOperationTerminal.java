package com.zeta.business.devicedisplay;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "terminal_operation_terminals")
@Getter
@Setter
@NoArgsConstructor
public class TerminalOperationTerminal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "terminal_operation_id", nullable = false)
    private Long terminalOperationId;
    @Column(name = "terminal_id", nullable = false)
    private Long terminalId;
    @Column(name = "terminal_meaning", nullable = false, length = 128)
    private String terminalMeaning;
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}
