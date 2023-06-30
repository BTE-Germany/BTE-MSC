package de.btegermany.msc;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class LoadingForm {

    public JPanel LoadingForm;
    public JProgressBar progressBar;
    public JLabel progressLabel;
    public JTextArea progressLog;
    public JButton progressFinishedButton;
    public JScrollPane progressLogScrollPane;

    public LoadingForm(JFrame frame, Analyzer analyzer) {
        progressFinishedButton.setVisible(false);
        SwingUtilities.invokeLater(() -> {
            try {
                analyzer.execute();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                analyzer.cancel(true);
                super.windowClosing(e);
            }
        });
    }


}
