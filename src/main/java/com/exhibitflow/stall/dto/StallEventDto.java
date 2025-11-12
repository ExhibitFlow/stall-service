package com.exhibitflow.stall.dto;

import com.exhibitflow.stall.model.StallStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StallEventDto {
    private Long stallId;
    private String code;
    private StallStatus status;
    private String location;
}
