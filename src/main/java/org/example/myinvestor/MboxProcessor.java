package org.example.myinvestor;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.example.myinvestor.export.ExcelExporter;
import org.example.myinvestor.mail.MailContent;
import org.example.myinvestor.mail.MailContentExtractor;
import org.example.myinvestor.mbox.MboxReader;
import org.example.myinvestor.model.StockOperation;
import org.example.myinvestor.model.UnparsedEmail;
import org.example.myinvestor.parser.OperationParser;
import org.example.myinvestor.parser.ParseOutcome;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class MboxProcessor {

    private final MboxReader mboxReader = new MboxReader();
    private final OperationParser operationParser = new OperationParser();
    private final ExcelExporter excelExporter = new ExcelExporter();

    public ProcessingResult process(Path mboxFile, Consumer<String> log, IntConsumer onMessageProcessed)
            throws IOException {
        List<byte[]> rawMessages = mboxReader.readMessages(mboxFile);
        log.accept("Mensajes encontrados en el mbox: " + rawMessages.size());

        Session session = Session.getDefaultInstance(new Properties());
        List<StockOperation> operations = new ArrayList<>();
        List<UnparsedEmail> unparsed = new ArrayList<>();
        int mimeErrors = 0;

        int processed = 0;
        for (byte[] raw : rawMessages) {
            processed++;
            try {
                MimeMessage message = new MimeMessage(session, new ByteArrayInputStream(raw));
                MailContent content = MailContentExtractor.extract(message);
                Optional<ParseOutcome> outcome = operationParser.tryParse(content);
                if (outcome.isPresent()) {
                    ParseOutcome result = outcome.get();
                    if (result.isSuccess()) {
                        operations.add(result.operation);
                    } else {
                        unparsed.add(new UnparsedEmail(content.subject(), content.date(), result.errorReason,
                                snippet(content)));
                    }
                }
            } catch (Exception e) {
                mimeErrors++;
            }
            if (onMessageProcessed != null) {
                onMessageProcessed.accept(processed);
            }
        }

        log.accept("Operaciones de compra/venta reconocidas: " + operations.size());
        log.accept("Correos de operación con datos incompletos (ver hoja 'No procesados'): " + unparsed.size());
        if (mimeErrors > 0) {
            log.accept("Mensajes que no se pudieron leer (formato no válido, ignorados): " + mimeErrors);
        }

        return new ProcessingResult(rawMessages.size(), operations, unparsed);
    }

    public void export(Path outputFile, ProcessingResult result) throws IOException {
        excelExporter.export(outputFile, result.operations(), result.unparsed());
    }

    private String snippet(MailContent content) {
        String text = (content.plainText() != null && !content.plainText().isBlank())
                ? content.plainText() : content.htmlText();
        if (text == null) {
            return "";
        }
        String trimmed = text.strip();
        return trimmed.length() > 300 ? trimmed.substring(0, 300) + "..." : trimmed;
    }
}
