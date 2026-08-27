package com.zeta.business.entities.settinglist;

/** 定值基准业务中的数值类型判定规则。 */
public final class SettingValueTypePolicy {
  public static final String FLOAT = "FLOAT";

  private SettingValueTypePolicy() {}

  /** Tmms 模型值以毫秒整型承载，定值基准换算为秒后按浮点型处理。 */
  public static String effectiveType(String settingRef, String catalogValueType) {
    return isTimeSetting(settingRef) ? FLOAT : catalogValueType;
  }

  public static boolean isTimeSetting(String settingRef) {
    return settingRef != null && settingRef.contains("Tmms");
  }
}
