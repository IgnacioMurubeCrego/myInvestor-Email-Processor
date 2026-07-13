package org.example.myinvestor.parser;

import org.example.myinvestor.model.StockOperation;

public class ParseOutcome {
    public final StockOperation operation;
    public final String errorReason;

    private ParseOutcome(StockOperation operation, String errorReason) {
        this.operation = operation;
        this.errorReason = errorReason;
    }

    public static ParseOutcome success(StockOperation operation) {
        return new ParseOutcome(operation, null);
    }

    public static ParseOutcome failure(String reason) {
        return new ParseOutcome(null, reason);
    }

    public boolean isSuccess() {
        return operation != null;
    }
}
