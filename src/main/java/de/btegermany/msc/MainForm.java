package de.btegermany.msc;

import javax.swing.*;
import java.io.File;
import java.util.logging.Level;

public class MainForm {
    public JPanel MainFormPanel;
    private JButton worldSelectorButton;
    private JComboBox worldTypeDropdown;
    private JLabel selectedWorldLabel;
    private JButton analyzeButton;
    private JComboBox analyzerCriteriaDropdown;
    private JTable foundLocations;

    private Analyzer analyzer;

    public MainForm(JFrame frame){
        worldTypeDropdown.addItem("CubicChunks");
        worldTypeDropdown.addItem("Vanilla/Anvil");

        analyzerCriteriaDropdown.addItem("Continent");
        analyzerCriteriaDropdown.addItem("Country");
        analyzerCriteriaDropdown.addItem("State/Province");
        analyzerCriteriaDropdown.addItem("City");

        analyzeButton.setEnabled(false);
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setCurrentDirectory(new File(Utils.getMinecraftDir("buildtheearth").toFile().getAbsolutePath() + "/saves"));

        worldSelectorButton.addActionListener(e -> {
            int returnVal = chooser.showOpenDialog(MainFormPanel);
            if(returnVal == JFileChooser.APPROVE_OPTION) {
                MSC.logger.log(Level.INFO,"Selected world: "+chooser.getSelectedFile().getName());
                selectedWorldLabel.setText(chooser.getSelectedFile().getAbsolutePath());
                worldSelectorButton.setText("Change World");
                analyzeButton.setEnabled(true);
            }
        });

        analyzeButton.addActionListener(e -> {
           analyzer = new Analyzer(chooser.getSelectedFile(),analyzeButton,frame, worldTypeDropdown,analyzerCriteriaDropdown,foundLocations);
        });


    }
}
