package com.zeta.business.entities.samplingtest.dto;

import com.zeta.business.entities.samplingtest.SamplingTestMediaType;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SamplingTestItemResponse {
  private Long id;
  private Long screenCabinetId;
  private String title;
  private SamplingTestMediaType mediaType;
  private String imageUrl;
  private String videoPath;
  private String content;
  private int sortOrder;
  private boolean enabled;
  private Instant createdAt;
  private List<SamplingTestChannelResponse> channels;
}
