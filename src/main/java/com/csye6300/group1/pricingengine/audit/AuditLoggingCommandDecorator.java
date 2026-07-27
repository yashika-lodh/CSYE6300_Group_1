package com.csye6300.group1.pricingengine.audit;

import com.csye6300.group1.pricingengine.command.PricingCommand;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Decorator pattern: adds compliance/audit logging around any PricingCommand
 * without changing the command's own execute()/undo() logic, and without the
 * invoker needing to know the command is being logged. Multiple decorators
 * (e.g. a future NotificationDecorator) could be layered the same way.
 */
public class AuditLoggingCommandDecorator implements PricingCommand {

    private final PricingCommand wrapped;
    private final Logger logger = Logger.getLogger(AuditLoggingCommandDecorator.class.getName());
    private final List<String> auditTrail = new CopyOnWriteArrayList<>();

    public AuditLoggingCommandDecorator(PricingCommand wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void execute() {
        wrapped.execute();
        record("EXECUTE");
    }

    @Override
    public void undo() {
        wrapped.undo();
        record("UNDO");
    }

    private void record(String action) {
        String entry = String.format("[%s] %s sku=%s originalPrice=%.2f newPrice=%.2f",
                Instant.now(), action, wrapped.getSku(), wrapped.getOriginalPrice(), wrapped.getNewPrice());
        auditTrail.add(entry);
        logger.info(entry);
    }

    /** Returns a full compliance report of every logged action for this command. */
    public List<String> generateAuditReport() {
        return Collections.unmodifiableList(auditTrail);
    }

    @Override
    public String getSku() {
        return wrapped.getSku();
    }

    @Override
    public double getOriginalPrice() {
        return wrapped.getOriginalPrice();
    }

    @Override
    public double getNewPrice() {
        return wrapped.getNewPrice();
    }
}
