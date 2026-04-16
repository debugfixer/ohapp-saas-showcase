package ru.soat.ohapp.saas.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import ru.soat.ohapp.saas.dto.ReferralRequest;
import ru.soat.ohapp.saas.service.ReferralService;

@RestController
@RequestMapping("/api/referrals")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generate(@Valid @RequestBody ReferralRequest request) {
        var result = referralService.generate(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encode(result.fileName()))
                .contentType(MediaType.parseMediaType(result.contentType()))
                .body(result.data());
    }

    private String encode(String filename) {
        return java.net.URLEncoder.encode(filename, java.nio.charset.StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
    }
}
