package org.example.myinvestor.ui;

import org.example.myinvestor.MboxProcessor;
import org.example.myinvestor.ProcessingResult;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainFrame extends JFrame {

    private final JTextField mboxPathField = new JTextField();
    private final JTextField outputPathField = new JTextField();
    private final JButton processButton = new JButton("Procesar");
    private final JTextArea logArea = new JTextArea();
    private final JProgressBar progressBar = new JProgressBar();

    public MainFrame() {
        super("myInvestor Email Processor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildFormPanel(), BorderLayout.NORTH);
        add(buildLogPanel(), BorderLayout.CENTER);
        add(progressBar, BorderLayout.SOUTH);

        setSize(720, 480);
        setLocationRelativeTo(null);
    }

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        mboxPathField.setEditable(false);
        outputPathField.setEditable(false);

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Archivo .mbox:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(mboxPathField, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        JButton browseMbox = new JButton("Examinar...");
        browseMbox.addActionListener(e -> chooseMboxFile());
        panel.add(browseMbox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Excel de salida:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(outputPathField, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        JButton browseOutput = new JButton("Guardar como...");
        browseOutput.addActionListener(e -> chooseOutputFile());
        panel.add(browseOutput, gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        processButton.addActionListener(e -> process());
        panel.add(processButton, gbc);

        return panel;
    }

    private JPanel buildLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Registro"));
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        panel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        return panel;
    }

    private void chooseMboxFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Archivos mbox (*.mbox)", "mbox"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            mboxPathField.setText(file.getAbsolutePath());
            if (outputPathField.getText().isBlank()) {
                String base = file.getName().replaceFirst("\\.[^.]+$", "");
                File suggested = new File(file.getParentFile(), base + "_operaciones.xlsx");
                outputPathField.setText(suggested.getAbsolutePath());
            }
        }
    }

    private void chooseOutputFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Libro de Excel (*.xlsx)", "xlsx"));
        if (!outputPathField.getText().isBlank()) {
            chooser.setSelectedFile(new File(outputPathField.getText()));
        }
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            String path = file.getAbsolutePath();
            if (!path.toLowerCase().endsWith(".xlsx")) {
                path = path + ".xlsx";
            }
            outputPathField.setText(path);
        }
    }

    private void process() {
        String mboxText = mboxPathField.getText();
        String outputText = outputPathField.getText();
        if (mboxText.isBlank() || outputText.isBlank()) {
            JOptionPane.showMessageDialog(this, "Selecciona el archivo .mbox y el destino del Excel.",
                    "Faltan datos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path mboxPath = Path.of(mboxText);
        Path outputPath = Path.of(outputText);

        processButton.setEnabled(false);
        logArea.setText("");
        progressBar.setIndeterminate(true);

        new ProcessWorker(mboxPath, outputPath).execute();
    }

    private class ProcessWorker extends SwingWorker<ProcessingResult, String> {
        private final Path mboxPath;
        private final Path outputPath;

        ProcessWorker(Path mboxPath, Path outputPath) {
            this.mboxPath = mboxPath;
            this.outputPath = outputPath;
        }

        @Override
        protected ProcessingResult doInBackground() throws Exception {
            MboxProcessor processor = new MboxProcessor();
            ProcessingResult result = processor.process(mboxPath, this::publish, count -> {
            });
            processor.export(outputPath, result);
            return result;
        }

        @Override
        protected void process(List<String> chunks) {
            for (String chunk : chunks) {
                logArea.append(chunk + "\n");
            }
        }

        @Override
        protected void done() {
            progressBar.setIndeterminate(false);
            processButton.setEnabled(true);
            try {
                ProcessingResult result = get();
                logArea.append("Excel guardado en: " + outputPath + "\n");
                JOptionPane.showMessageDialog(MainFrame.this,
                        "Proceso completado.\n" +
                                "Operaciones exportadas: " + result.operations().size() + "\n" +
                                "Correos no reconocidos: " + result.unparsed().size() + "\n\n" +
                                "Excel: " + outputPath,
                        "Completado", JOptionPane.INFORMATION_MESSAGE);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                logArea.append("ERROR: " + e.getCause() + "\n");
                JOptionPane.showMessageDialog(MainFrame.this,
                        "Ha ocurrido un error durante el procesado:\n" + e.getCause(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

}
