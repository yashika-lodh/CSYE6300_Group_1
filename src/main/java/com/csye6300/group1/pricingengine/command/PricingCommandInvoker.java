package com.csye6300.group1.pricingengine.command;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Command pattern "invoker": runs PricingCommands and keeps a history stack
 * so any change can be undone, and any undone change can be redone.
 */
public class PricingCommandInvoker {

    private final Deque<PricingCommand> undoStack = new ArrayDeque<>();
    private final Deque<PricingCommand> redoStack = new ArrayDeque<>();

    public void executeCommand(PricingCommand command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear(); // new action invalidates the redo history
    }

    public boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }
        PricingCommand command = undoStack.pop();
        command.undo();
        redoStack.push(command);
        return true;
    }

    public boolean redo() {
        if (redoStack.isEmpty()) {
            return false;
        }
        PricingCommand command = redoStack.pop();
        command.execute();
        undoStack.push(command);
        return true;
    }

    public int historySize() {
        return undoStack.size();
    }
}
