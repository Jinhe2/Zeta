package com.zeta.business.entities.devicedisplay;

import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "terminal_operation_terminals")
@Getter
@Setter
@NoArgsConstructor
public class TerminalOperationTerminal {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "terminal_operation_id", nullable = false)
  private Long terminalOperationId;

  @Column(name = "terminal_id", nullable = false)
  private Long terminalId;

  @Column(name = "expected_output_code", length = 8)
  private String expectedOutputCode;

  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder;
}
