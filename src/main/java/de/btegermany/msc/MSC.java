package de.btegermany.msc;

import com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme;
import de.btegermany.msc.logger.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.IOException;
import java.util.logging.Level;

public class MSC {

    public static Logger logger = new Logger();
    public static JFrame frame;

    public static void main(String[] args){
        logger.log(Level.INFO,"Starting Programm...");
        FlatOneDarkIJTheme.install(new FlatOneDarkIJTheme());
       frame = new JFrame("BTE MSC");
        try {
            frame.setIconImage(ImageIO.read(MainForm.class.getResourceAsStream("/logo.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        frame.setContentPane(new MainForm(frame).MainFormPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
