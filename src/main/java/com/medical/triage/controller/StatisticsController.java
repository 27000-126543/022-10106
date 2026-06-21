package com.medical.triage.controller;

import com.medical.triage.dto.response.ApiResponse;
import com.medical.triage.dto.response.StatisticsResponse;
import com.medical.triage.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/store/{storeId}")
    public ApiResponse<StatisticsResponse> getStoreStatistics(
            @PathVariable Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        StatisticsResponse statistics = statisticsService.getStoreStatistics(storeId, startDate, endDate);
        return ApiResponse.success(statistics);
    }

    @GetMapping("/all")
    public ApiResponse<List<StatisticsResponse>> getAllStoresStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<StatisticsResponse> statistics = statisticsService.getAllStoresStatistics(startDate, endDate);
        return ApiResponse.success(statistics);
    }
}
