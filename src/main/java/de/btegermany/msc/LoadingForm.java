package de.btegermany.msc;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class LoadingForm {

    public JPanel LoadingForm;
    public JProgressBar progressBar;
    public JLabel progressLabel;
    public JTextArea textArea1;

    public LoadingForm(JFrame frame) {


        SwingUtilities.invokeLater(() -> {
            try {
                //installTask.execute();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
               // installTask.cancel(true);
                super.windowClosing(e);
            }
        });


    }


}
