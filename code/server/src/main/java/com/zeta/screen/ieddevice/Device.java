package com.zeta.screen.ieddevice;

import com.zeta.screen.cabinet.Cabinet;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

/** IED 保护装置，对应 ct-screen.ied_device */
@Entity
@Table(name = "ied_device")
@Getter
@Setter
@NoArgsConstructor
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cabinet_id", nullable = false)
    private Cabinet cabinet;

    @Column(name = "ied_name", nullable = false, length = 128)
    private String iedName;

    @Column(name = "device_type", length = 64)
    private String deviceType;

    @Column(length = 128)
    private String manufacturer;

    @Column(name = "ied_desc", length = 256)
    private String iedDesc;

    @Column(name = "config_version", length = 32)
    private String configVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** API 兼容：SCD IED name */
    public String getCode() {
        return iedName;
    }

    /** API 兼容：展示名称 */
    public String getName() {
        if (iedDesc != null && !iedDesc.trim().isEmpty()) {
            return iedDesc;
        }
        return iedName;
    }

    /** API 兼容：说明文字 */
    public String getDescription() {
        return iedDesc;
    }

    public void setCode(String code) {
        this.iedName = code;
    }

    public void setName(String name) {
        this.iedDesc = name;
    }

    public void setDescription(String description) {
        this.iedDesc = description;
    }
}
