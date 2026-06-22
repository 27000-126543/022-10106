package com.medical.triage.dto.response;

import com.medical.triage.enums.ScoreDetailType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreDetailResponse {

    private ScoreDetailType type;

    private Integer score;

    private Integer maxScore;

    private String description;

    private List<String> matchedItems;
}
