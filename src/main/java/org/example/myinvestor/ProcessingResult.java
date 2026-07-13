package org.example.myinvestor;

import org.example.myinvestor.model.StockOperation;
import org.example.myinvestor.model.UnparsedEmail;

import java.util.List;

public record ProcessingResult(int totalMessages, List<StockOperation> operations, List<UnparsedEmail> unparsed) {
}
