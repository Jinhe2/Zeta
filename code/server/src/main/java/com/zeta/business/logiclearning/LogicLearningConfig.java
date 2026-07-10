package com.zeta.business.logiclearning;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

/** 逻辑学习展示配置；逻辑框图基础数据仍来自只读 screen 库。 */
@Entity
@Table(name = "logic_learning_configs")
@Getter
@Setter
@NoArgsConstructor
public class LogicLearningConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "logic_diagram_id", nullable = false, unique = true)
    private Long logicDiagramId;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
