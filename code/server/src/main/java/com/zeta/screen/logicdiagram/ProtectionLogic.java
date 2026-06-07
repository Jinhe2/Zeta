package com.zeta.screen.logicdiagram;

import com.zeta.screen.ieddevice.Device;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

/** 保护逻辑框图，对应 ct-screen.logic_diagram */
@Entity
@Table(name = "logic_diagram")
@Getter
@Setter
@NoArgsConstructor
public class ProtectionLogic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ied_device_id", nullable = false)
    private Device device;

    @Column(name = "logic_id", nullable = false, length = 128)
    private String logicId;

    @Column(name = "logic_name", nullable = false, length = 256)
    private String logicName;

    @Column(name = "protect_type", length = 64)
    private String protectType;

    @Column(nullable = false, length = 16)
    private String version = "1.0";

    @Lob
    private String description;

    @Column(name = "config_json", nullable = false, columnDefinition = "json")
    private String configJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** API 兼容 */
    public String getCode() {
        return logicId;
    }

    public String getTitle() {
        return logicName;
    }

    public String getCategory() {
        return protectType;
    }

    public void setCode(String code) {
        this.logicId = code;
    }

    public void setTitle(String title) {
        this.logicName = title;
    }

    public void setCategory(String category) {
        this.protectType = category;
    }
}
