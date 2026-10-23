package com.banking.engine.inbox;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedCommandRepository extends JpaRepository<ProcessedCommand, UUID> {
}
