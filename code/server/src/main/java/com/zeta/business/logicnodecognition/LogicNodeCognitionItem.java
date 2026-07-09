package com.zeta.business.logicnodecognition;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

/** 逻辑框图节点的认知条目，节点本身来自 screen 库，只在业务库保存教学内容。 */
@Entity
@Table(name = "logic_node_cognition_items")
@Getter
@Setter
@NoArgsConstructor
public class LogicNodeCognitionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "logic_diagram_id", nullable = false)
    private Long logicDiagramId;

    @Column(name = "node_id", nullable = false, length = 128)
    private String nodeId;

    @Column(name = "node_type", nullable = false, length = 32)
    private String nodeType;

    @Column(name = "node_name", nullable = false, length = 256)
    private String nodeName;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Lob
    @Column(name = "image_data")
    private byte[] imageData;

    @Column(name = "image_content_type", length = 100)
    private String imageContentType;

    @Column(name = "left_percent")
    private Double leftPercent;

    @Column(name = "top_percent")
    private Double topPercent;

    @Column(name = "width_percent")
    private Double widthPercent;

    @Column(name = "height_percent")
    private Double heightPercent;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false,
            columnDefinition = "TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant createdAt = Instant.now();
}
