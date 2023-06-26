package de.btegermany.msc.exceptions;

import de.btegermany.msc.MSC;

import javax.swing.*;
import java.util.logging.Level;

public class AnalyzerException extends RuntimeException{

    public AnalyzerException(String message) {
        super(message);
        MSC.logger.log(Level.SEVERE,message);
        JOptionPane.showMessageDialog(MSC.frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    public AnalyzerException(Exception exception) {
        super(exception);
        MSC.logger.log(Level.SEVERE,exception.getMessage()+"\n"+exception.getCause());
        JOptionPane.showMessageDialog(MSC.frame, exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
