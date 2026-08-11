package com.zeta.business.entities.devicedisplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TestInstrumentOutputCodeTest {

  @Test
  void 应规范化合法输出代码() {
    assertEquals("Ua", TestInstrumentOutputCode.canonicalize(" ua "));
    assertEquals("In2", TestInstrumentOutputCode.canonicalize("IN2"));
  }

  @Test
  void 应允许空配置并拒绝未知代码() {
    assertNull(TestInstrumentOutputCode.canonicalize(null));
    assertNull(TestInstrumentOutputCode.canonicalize("  "));
    assertNull(TestInstrumentOutputCode.canonicalize("U4"));
  }
}
