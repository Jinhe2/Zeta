package com.zeta.screen.cabinet;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

/** 屏柜信息，对应 ct-screen.cabinet */
@Entity
@Table(name = "cabinet")
@Getter
@Setter
@NoArgsConstructor
public class Cabinet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 256)
    private String location;

    @Lob
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** API 兼容：优先用 location 作为业务标识，否则回退 name */
    public String getCode() {
        if (location != null && !location.trim().isEmpty()) {
            return location;
        }
        return name;
    }

    public void setCode(String code) {
        this.location = code;
    }
}
