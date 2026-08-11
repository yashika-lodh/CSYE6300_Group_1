package com.csye6300.group1.pricingengine.command;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * Command pattern "invoker": runs PricingCommands and keeps a history stack
 * so any change can be undone, and any undone change can be redone.
 *
 * History is kept in batches, not individual commands: a single reprice pushes one
 * PricingCommand per channel (one per SalesChannel), and those must be treated as one
 * atomic unit for undo/redo -- otherwise a single undo() would only reverse one channel's
 * price, or (if grouped by SKU identity instead of by call) could accidentally merge two
 * separate reprices of the same SKU into one undo if they happen to sit back-to-back in
 * the stack with no other SKU's commands in between.
 */
public class PricingCommandInvoker {

    private final Deque<List<PricingCommand>> undoStack = new ArrayDeque<>();
    private final Deque<List<PricingCommand>> redoStack = new ArrayDeque<>();

    /** Runs a single command as its own one-command batch. */
    public void executeCommand(PricingCommand command) {
        executeCommands(List.of(command));
    }

    /** Runs a group of commands (e.g. one reprice's per-channel commands) as one atomic undo/redo unit. */
    public void executeCommands(List<PricingCommand> commands) {
        commands.forEach(PricingCommand::execute);
        undoStack.push(List.copyOf(commands));
        redoStack.clear(); // new action invalidates the redo history
    }

    public boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }
        undoBatch(undoStack.pop());
        return true;
    }

    /**
     * Undoes the given SKU's most recent reprice batch, wherever it sits in the (shared,
     * multi-SKU) undo stack -- not just if it's on top. Other SKUs' reprices (from the
     * Observer, the scheduled sweep, or another manual click) can land on top of this SKU's
     * batch in between a Reprice and an Undo click; this finds the topmost batch that actually
     * belongs to this SKU and undoes only that one batch, leaving every other batch (including
     * this SKU's own older reprices) untouched. Returns false if this SKU has no batch anywhere
     * in the stack.
     */
    public boolean undoLastForSku(String sku) {
        for (Iterator<List<PricingCommand>> iterator = undoStack.iterator(); iterator.hasNext(); ) {
            List<PricingCommand> batch = iterator.next();
            if (!batch.isEmpty() && sku.equals(batch.get(0).getSku())) {
                iterator.remove();
                undoBatch(batch);
                return true;
            }
        }
        return false;
    }

    private void undoBatch(List<PricingCommand> batch) {
        for (int i = batch.size() - 1; i >= 0; i--) { // reverse of execution order
            batch.get(i).undo();
        }
        redoStack.push(batch);
    }

    public boolean redo() {
        if (redoStack.isEmpty()) {
            return false;
        }
        List<PricingCommand> batch = redoStack.pop();
        batch.forEach(PricingCommand::execute);
        undoStack.push(batch);
        return true;
    }

    public int historySize() {
        return undoStack.size();
    }
}
