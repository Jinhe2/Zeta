package com.zeta.business.devicedisplay;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "terminal_operation_items")
@Getter
@Setter
@NoArgsConstructor
public class TerminalOperation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_display_item_id", nullable = false, unique = true)
    private Long deviceDisplayItemId;

    @Column(name = "terminal_strip_id", nullable = false)
    private Long terminalStripId;
}
