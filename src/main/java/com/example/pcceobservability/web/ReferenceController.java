package com.example.pcceobservability.web;

import com.example.pcceobservability.model.DispositionCode;
import com.example.pcceobservability.service.DispositionCodeService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reference")
public class ReferenceController {

    private final DispositionCodeService dispositionCodeService;

    public ReferenceController(DispositionCodeService dispositionCodeService) {
        this.dispositionCodeService = dispositionCodeService;
    }

    @GetMapping("/disposition-codes")
    @PreAuthorize("hasAnyAuthority('PERM_CALL_METRICS_READ', 'PERM_DROPPED_CALLS_READ')")
    public List<DispositionCode> dispositionCodes() {
        return dispositionCodeService.codes();
    }
}
