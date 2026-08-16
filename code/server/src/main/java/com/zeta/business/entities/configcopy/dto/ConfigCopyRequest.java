package com.zeta.business.entities.configcopy.dto;

import com.zeta.business.entities.configcopy.ConfigCopyModule;
import com.zeta.business.entities.configcopy.ConfigCopyScope;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfigCopyRequest {
  @NotNull private ConfigCopyScope scope;
  @NotNull private Long sourceId;
  @NotEmpty private Set<ConfigCopyModule> modules = EnumSet.noneOf(ConfigCopyModule.class);
  @Valid @NotEmpty private List<TargetCopyRequest> targets = new ArrayList<>();
}
