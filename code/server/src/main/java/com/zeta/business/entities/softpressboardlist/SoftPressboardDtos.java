package com.zeta.business.entities.softpressboardlist;

import com.zeta.business.entities.settinglist.SettingListScopeType;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public final class SoftPressboardDtos {
  private SoftPressboardDtos() {}

  @Getter
  @AllArgsConstructor
  public static class ItemResponse {
    private String pressboardRef;
    private String pressboardName;
    private boolean baselineValue;
    private boolean compareEnabled;
    private Integer sortOrder;

    public static ItemResponse from(SoftPressboardListItem item) {
      return new ItemResponse(
          item.getPressboardRef(), item.getPressboardName(),
          Boolean.TRUE.equals(item.getBaselineValue()),
          !Boolean.FALSE.equals(item.getCompareEnabled()), item.getSortOrder());
    }
  }

  @Getter
  @AllArgsConstructor
  public static class ListResponse {
    private SettingListScopeType scopeType;
    private Long scopeId;
    private String scopeName;
    private Long iedDeviceId;
    private String iedName;
    private SettingListScopeType effectiveScopeType;
    private Long effectiveScopeId;
    private boolean fallbackToDevice;
    private List<ItemResponse> configuredItems;
    private List<ItemResponse> effectiveItems;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  public static class SaveItemRequest {
    @NotBlank(message = "软压板引用不能为空")
    private String pressboardRef;
    @NotNull(message = "软压板基准状态不能为空")
    private Boolean baselineValue;
    private Boolean compareEnabled = true;
    private Integer sortOrder;
  }

  @Getter
  @Setter
  public static class SaveRequest {
    @NotNull(message = "软压板基准清单不能为空")
    @Valid
    private List<SaveItemRequest> items = new ArrayList<>();
  }

  @Getter
  @AllArgsConstructor
  public static class SummonResponse {
    private int summonCount;
    private int catalogCount;
    private int matchedCount;
    private List<ItemResponse> items;
  }

  @Getter
  @AllArgsConstructor
  public static class CheckItemResponse {
    private String pressboardRef;
    private String pressboardName;
    private String baselineValue;
    private String actualValue;
    private boolean matched;
    private boolean equal;
  }

  @Getter
  @AllArgsConstructor
  public static class CheckResponse {
    private String status;
    private SettingListScopeType effectiveScopeType;
    private Long effectiveScopeId;
    private int total;
    private int equal;
    private int mismatch;
    private int missing;
    private List<CheckItemResponse> items;
  }
}
