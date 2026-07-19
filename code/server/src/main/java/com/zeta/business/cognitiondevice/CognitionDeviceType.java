package com.zeta.business.cognitiondevice;

/**
 * 屏柜学习图上的抽象设备类型。
 */
public enum CognitionDeviceType {
    /** IED 设备外观认知（保留既有存储值以兼容历史数据）。 */
    IED,
    /** IED 设备操作认知。 */
    IED_OPERATION,
    OTHER_DEVICE,
    TERMINAL_GROUP,
    PLATE_GROUP
}
