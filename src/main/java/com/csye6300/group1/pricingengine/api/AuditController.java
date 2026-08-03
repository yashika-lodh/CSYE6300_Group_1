package com.csye6300.group1.pricingengine.api;

import com.csye6300.group1.pricingengine.audit.AuditLoggingCommandDecorator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes the Decorator pattern's compliance log as the audit-report
 * endpoint: every EXECUTE/UNDO logged by AuditLoggingCommandDecorator,
 * across every command the engine has ever run.
 */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    @GetMapping
    public List<String> fullAuditReport() {
        return AuditLoggingCommandDecorator.getGlobalAuditTrail();
    }

    /** Filters the global audit trail down to entries for a single SKU. */
    @GetMapping("/{sku}")
    public List<String> auditReportForSku(@PathVariable String sku) {
        String marker = "sku=" + sku + " ";
        return AuditLoggingCommandDecorator.getGlobalAuditTrail().stream()
                .filter(entry -> entry.contains(marker))
                .toList();
    }
}
