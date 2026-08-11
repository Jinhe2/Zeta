package com.zeta.business.entities.devicedisplay;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.StringUtils;

/** 试验仪支持的十六个语义输出代码。 */
public final class TestInstrumentOutputCode {

  private static final Map<String, String> CANONICAL_CODES;

  static {
    Map<String, String> codes = new LinkedHashMap<>();
    for (String code : new String[] {
        "Ua", "Ub", "Uc", "Un", "Ux", "Uy", "Uz", "Un2",
        "Ia", "Ib", "Ic", "In", "Ix", "Iy", "Iz", "In2"
    }) {
      codes.put(code.toLowerCase(Locale.ROOT), code);
    }
    CANONICAL_CODES = Collections.unmodifiableMap(codes);
  }

  private TestInstrumentOutputCode() {}

  /** 空值表示未配置；合法值统一为协议约定的大小写形式。 */
  public static String canonicalize(String value) {
    if (!StringUtils.hasText(value)) return null;
    return CANONICAL_CODES.get(value.trim().toLowerCase(Locale.ROOT));
  }
}
