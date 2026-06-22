package com.medical.triage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendStatisticsResponse {

    private String dateRange;

    private List<DailyTrendItem> dailyTrends;

    private List<ChannelStats> channelStats;

    private List<ProjectStats> projectStats;

    private List<RiskStats> riskStats;
}
