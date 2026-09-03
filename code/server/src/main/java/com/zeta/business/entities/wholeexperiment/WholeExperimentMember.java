package com.zeta.business.entities.wholeexperiment;

import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "whole_experiment_member", uniqueConstraints = {
    @UniqueConstraint(name = "uk_whole_experiment_sequence", columnNames = {"experiment_id", "sequence_no"}),
    @UniqueConstraint(name = "uk_whole_experiment_logic", columnNames = {"experiment_id", "logic_diagram_id"})})
@Getter
@Setter
public class WholeExperimentMember {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(name = "experiment_id", nullable = false) private Long experimentId;
  @Column(name = "logic_diagram_id", nullable = false) private Long logicDiagramId;
  @Column(name = "sequence_no", nullable = false) private Integer sequenceNo;
  @Column(name = "logic_code", length = 256) private String code;
  @Column(name = "logic_title", length = 512) private String title;
}
