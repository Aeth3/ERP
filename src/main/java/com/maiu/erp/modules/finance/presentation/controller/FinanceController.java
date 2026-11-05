package com.maiu.erp.modules.finance.presentation.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.maiu.erp.modules.finance.application.dto.AccountDto;
import com.maiu.erp.modules.finance.application.dto.AccountLedgerDto;
import com.maiu.erp.modules.finance.application.dto.AccountMappingDto;
import com.maiu.erp.modules.finance.application.dto.ActionResponse;
import com.maiu.erp.modules.finance.application.dto.CreateAccountRequest;
import com.maiu.erp.modules.finance.application.dto.CreateJournalEntryRequest;
import com.maiu.erp.modules.finance.application.dto.FiscalPeriodDto;
import com.maiu.erp.modules.finance.application.dto.InventoryReconciliationDto;
import com.maiu.erp.modules.finance.application.dto.JournalEntryDto;
import com.maiu.erp.modules.finance.application.dto.ReverseJournalEntryRequest;
import com.maiu.erp.modules.finance.application.dto.TrialBalanceDto;
import com.maiu.erp.modules.finance.application.dto.UpdateAccountMappingRequest;
import com.maiu.erp.modules.finance.application.dto.UpdateAccountRequest;
import com.maiu.erp.modules.finance.application.service.FinanceAdminService;
import com.maiu.erp.modules.finance.application.service.FinanceQueryService;
import com.maiu.erp.modules.finance.application.service.FiscalPeriodService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/finance")
public class FinanceController {
    private final FinanceQueryService financeQueryService;
    private final FinanceAdminService financeAdminService;
    private final FiscalPeriodService fiscalPeriodService;

    public FinanceController(
            FinanceQueryService financeQueryService,
            FinanceAdminService financeAdminService,
            FiscalPeriodService fiscalPeriodService) {
        this.financeQueryService = financeQueryService;
        this.financeAdminService = financeAdminService;
        this.fiscalPeriodService = fiscalPeriodService;
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<AccountDto>> getAccounts() {
        return ResponseEntity.ok(financeQueryService.getAccounts());
    }

    @PostMapping("/accounts")
    public ResponseEntity<Map<String, UUID>> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.ok(Map.of("id", financeAdminService.createAccount(request).getId()));
    }

    @PutMapping("/accounts/{id}")
    public ResponseEntity<ActionResponse> updateAccount(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAccountRequest request) {
        financeAdminService.updateAccount(id, request);
        return ResponseEntity.ok(new ActionResponse("Account updated successfully"));
    }

    @GetMapping("/accounts/{id}/ledger")
    public ResponseEntity<AccountLedgerDto> getAccountLedger(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(financeQueryService.getAccountLedger(id, startDate, endDate));
    }

    @GetMapping("/account-mappings")
    public ResponseEntity<List<AccountMappingDto>> getAccountMappings() {
        return ResponseEntity.ok(financeQueryService.getAccountMappings());
    }

    @PutMapping("/account-mappings/{key}")
    public ResponseEntity<ActionResponse> updateAccountMapping(
            @PathVariable String key,
            @Valid @RequestBody UpdateAccountMappingRequest request) {
        financeAdminService.updateAccountMapping(key, request.accountId());
        return ResponseEntity.ok(new ActionResponse("Account mapping updated successfully"));
    }

    @GetMapping("/journal-entries")
    public ResponseEntity<List<JournalEntryDto>> getJournalEntries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) UUID sourceId) {
        if (sourceId != null) {
            return ResponseEntity.ok(financeQueryService.getJournalEntriesForSource(sourceId));
        }
        return ResponseEntity.ok(financeQueryService.getJournalEntries(startDate, endDate, sourceType));
    }

    @GetMapping("/journal-entries/{id}")
    public ResponseEntity<JournalEntryDto> getJournalEntry(@PathVariable UUID id) {
        return ResponseEntity.ok(financeQueryService.getJournalEntry(id));
    }

    @PostMapping("/journal-entries")
    public ResponseEntity<Map<String, UUID>> createJournalEntry(@Valid @RequestBody CreateJournalEntryRequest request) {
        return ResponseEntity.ok(Map.of("id", financeAdminService.createManualEntry(request).getId()));
    }

    @PostMapping("/journal-entries/{id}/reverse")
    public ResponseEntity<Map<String, UUID>> reverseJournalEntry(
            @PathVariable UUID id,
            @RequestBody(required = false) ReverseJournalEntryRequest request) {
        return ResponseEntity.ok(Map.of("id", financeAdminService.reverseEntry(id, request).getId()));
    }

    @GetMapping("/trial-balance")
    public ResponseEntity<TrialBalanceDto> getTrialBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(financeQueryService.getTrialBalance(startDate, endDate));
    }

    @GetMapping("/periods")
    public ResponseEntity<List<FiscalPeriodDto>> getPeriods() {
        return ResponseEntity.ok(financeQueryService.getPeriods());
    }

    @PostMapping("/periods/{year}/{month}/close")
    public ResponseEntity<ActionResponse> closePeriod(@PathVariable int year, @PathVariable int month) {
        fiscalPeriodService.closePeriod(year, month);
        return ResponseEntity.ok(new ActionResponse("Fiscal period closed successfully"));
    }

    @PostMapping("/periods/{year}/{month}/reopen")
    public ResponseEntity<ActionResponse> reopenPeriod(@PathVariable int year, @PathVariable int month) {
        fiscalPeriodService.reopenPeriod(year, month);
        return ResponseEntity.ok(new ActionResponse("Fiscal period reopened successfully"));
    }

    @GetMapping("/inventory-reconciliation")
    public ResponseEntity<InventoryReconciliationDto> getInventoryReconciliation() {
        return ResponseEntity.ok(financeQueryService.getInventoryReconciliation());
    }
}
