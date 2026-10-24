package com.banking.engine.api;

import com.banking.engine.transfer.TransferService;
import com.banking.engine.transfer.TransferSubmission;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferService transfers;

    public TransferController(TransferService transfers) {
        this.transfers = transfers;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> submit(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateTransferRequest request) {
        TransferSubmission submission = transfers.submit(idempotencyKey, request);
        TransferResponse body = TransferResponse.from(submission.transfer());
        if (!submission.created()) {
            return ResponseEntity.ok(body);
        }
        return ResponseEntity.accepted().location(URI.create("/api/v1/transfers/" + body.id())).body(body);
    }

    @GetMapping("/{transferId}")
    public TransferResponse get(@PathVariable UUID transferId) {
        return TransferResponse.from(transfers.get(transferId));
    }
}
