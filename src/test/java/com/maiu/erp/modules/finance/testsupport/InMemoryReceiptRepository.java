package com.maiu.erp.modules.finance.testsupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.maiu.erp.modules.finance.domain.model.Receipt;
import com.maiu.erp.modules.finance.domain.repository.ReceiptRepository;

public final class InMemoryReceiptRepository implements ReceiptRepository {
    private final List<Receipt> receipts = new ArrayList<>();

    @Override
    public Receipt save(Receipt receipt) {
        if (receipt.getId() == null) {
            receipt.setId(UUID.randomUUID());
            receipts.add(receipt);
        } else if (receipts.stream().noneMatch(existing -> receipt.getId().equals(existing.getId()))) {
            receipts.add(receipt);
        } else {
            receipts.replaceAll(existing -> receipt.getId().equals(existing.getId()) ? receipt : existing);
        }
        return receipt;
    }

    @Override
    public Optional<Receipt> findById(UUID id) {
        return receipts.stream().filter(receipt -> id.equals(receipt.getId())).findFirst();
    }

    @Override
    public List<Receipt> findByCustomerId(UUID customerId) {
        return receipts.stream().filter(receipt -> customerId.equals(receipt.getCustomerId())).toList();
    }

    @Override
    public List<Receipt> findAll() {
        return List.copyOf(receipts);
    }

    @Override
    public boolean existsByReceiptNumber(String receiptNumber) {
        return receipts.stream().anyMatch(receipt -> receiptNumber.equals(receipt.getReceiptNumber()));
    }
}
