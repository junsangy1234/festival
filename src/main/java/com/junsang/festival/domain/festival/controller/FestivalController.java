package com.junsang.festival.domain.festival.controller;

import com.junsang.festival.domain.festival.dto.request.FestivalSearchRequest;
import com.junsang.festival.domain.festival.dto.response.CompetingFestivalResponse;
import com.junsang.festival.domain.festival.dto.response.FestivalDetailResponse;
import com.junsang.festival.domain.festival.dto.response.FestivalListResponse;
import com.junsang.festival.domain.festival.service.FestivalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 축제 검색·상세·경합 API
@RestController
@RequestMapping("/api/v1/festivals")
@RequiredArgsConstructor
public class FestivalController {

    private final FestivalService festivalService;

    // 축제 목록 조회
    @GetMapping
    public FestivalListResponse searchFestivals(
            @Valid @ModelAttribute FestivalSearchRequest request) {
        return festivalService.searchFestivals(request);
    }

    // 경합 축제 조회
    @GetMapping("/competing")
    public CompetingFestivalResponse getCompetingFestivals(
            @RequestParam @NotBlank String contentId) {
        return festivalService.getCompetingFestivals(contentId);
    }

    // 축제 상세 조회
    @GetMapping("/{contentId}")
    public FestivalDetailResponse getFestival(
            @PathVariable String contentId) {
        return festivalService.getFestival(contentId);
    }
}
