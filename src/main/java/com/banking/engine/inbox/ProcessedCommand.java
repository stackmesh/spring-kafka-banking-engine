package com.banking.engine.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_command")
public class ProcessedCommand {

    @Id
    @Column(name = "command_id")
    private UUID commandId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedCommand() {
    }

    public static ProcessedCommand of(UUID commandId) {
        ProcessedCommand command = new ProcessedCommand();
        command.commandId = commandId;
        command.processedAt = Instant.now();
        return command;
    }

    public UUID getCommandId() {
        return commandId;
    }
}
