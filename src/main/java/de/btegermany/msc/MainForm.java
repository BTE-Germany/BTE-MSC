package de.btegermany.msc;

import de.btegermany.msc.gui.LocationListEntryTableModel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.io.File;
import java.util.logging.Level;
import java.util.prefs.Preferences;

public class MainForm {
    public JPanel MainFormPanel;
    private JButton worldSelectorButton;
    private JComboBox worldTypeDropdown;
    private JLabel selectedWorldLabel;
    private JButton analyzeButton;
    private JComboBox analyzerCriteriaDropdown;
    private JTable foundLocations;
    private JButton runMSCButton;
    private JLabel totalRegionFileAmount;
    private JLabel totalSpace;
    private JButton moreAnalyticsButton;
    private JCheckBox deleteBuggedRegionsCheckBox;
    private JTextField searchField;

    private Analyzer analyzer;
    private LocationListEntryTableModel tableModel;

    // Preferences for storing last used values
    private static final Preferences prefs = Preferences.userNodeForPackage(MainForm.class);
    private static final String PREF_LAST_FOLDER = "lastFolder";
    private static final String PREF_WORLD_TYPE = "worldType";
    private static final String PREF_ANALYZER_CRITERIA = "analyzerCriteria";

    public MainForm(JFrame frame){
        //moreAnalyticsButton.setVisible(false);
        runMSCButton.setVisible(false);
        worldTypeDropdown.addItem("CubicChunks");
        worldTypeDropdown.addItem("Vanilla/Anvil");

        // Load saved world type preference
        String savedWorldType = prefs.get(PREF_WORLD_TYPE, "CubicChunks");
        worldTypeDropdown.setSelectedItem(savedWorldType);

        // Save preference when changed
        worldTypeDropdown.addActionListener(e -> {
            if (worldTypeDropdown.getSelectedItem() != null) {
                prefs.put(PREF_WORLD_TYPE, worldTypeDropdown.getSelectedItem().toString());
                MSC.logger.log(Level.INFO, "Saved world type preference: " + worldTypeDropdown.getSelectedItem());
            }
        });

        analyzerCriteriaDropdown.addItem("Continent");
        analyzerCriteriaDropdown.addItem("Country");
        analyzerCriteriaDropdown.addItem("State/Province");
        analyzerCriteriaDropdown.addItem("City");

        // Load saved analyzer criteria preference
        String savedCriteria = prefs.get(PREF_ANALYZER_CRITERIA, "Country");
        analyzerCriteriaDropdown.setSelectedItem(savedCriteria);

        // Save preference when changed
        analyzerCriteriaDropdown.addActionListener(e -> {
            if (analyzerCriteriaDropdown.getSelectedItem() != null) {
                prefs.put(PREF_ANALYZER_CRITERIA, analyzerCriteriaDropdown.getSelectedItem().toString());
                MSC.logger.log(Level.INFO, "Saved analyzer criteria preference: " + analyzerCriteriaDropdown.getSelectedItem());
            }
        });

        // Initialize search field listener if bound from form
        if (searchField != null) {
            MSC.logger.log(Level.INFO, "Search field initialized and listener attached");
            searchField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    applyFilter();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    applyFilter();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    applyFilter();
                }

                private void applyFilter() {
                    String filterText = searchField.getText();

                    // Always get the current table model from the table to handle re-analysis
                    if (foundLocations != null && foundLocations.getModel() instanceof LocationListEntryTableModel) {
                        tableModel = (LocationListEntryTableModel) foundLocations.getModel();
                    }

                    MSC.logger.log(Level.INFO, "Applying filter: '" + filterText + "', tableModel=" + (tableModel != null ? "present" : "null"));
                    if (tableModel != null) {
                        tableModel.setFilterText(filterText);
                        MSC.logger.log(Level.INFO, "Filter applied, row count: " + tableModel.getRowCount());
                    } else {
                        MSC.logger.log(Level.WARNING, "Cannot apply filter - table model is null. Analyze a world first!");
                    }
                }
            });
        } else {
            MSC.logger.log(Level.WARNING, "Search field is NULL - form bindings not initialized. Rebuild in IntelliJ!");
        }

        analyzeButton.setEnabled(false);
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        // Load last used folder from preferences
        String lastFolder = prefs.get(PREF_LAST_FOLDER, null);
        File lastFolderFile = null;

        if (lastFolder != null && new File(lastFolder).exists()) {
            lastFolderFile = new File(lastFolder);
            chooser.setCurrentDirectory(lastFolderFile.getParentFile()); // Set parent as current dir
            chooser.setSelectedFile(lastFolderFile); // Pre-select the actual world folder

            MSC.logger.log(Level.INFO, "Loaded last folder: " + lastFolder);

            // Update GUI to reflect the loaded world
            if (selectedWorldLabel != null) {
                selectedWorldLabel.setText(lastFolder);
            }
            if (worldSelectorButton != null) {
                worldSelectorButton.setText("Change World");
            }
            if (analyzeButton != null) {
                analyzeButton.setEnabled(true);
            }

            MSC.logger.log(Level.INFO, "GUI updated with last folder, analyze button enabled");
        } else {
            // Fallback to default Minecraft saves folder
            chooser.setCurrentDirectory(new File(Utils.getMinecraftDir("buildtheearth").toFile().getAbsolutePath() + "/saves"));
            if (lastFolder != null) {
                MSC.logger.log(Level.WARNING, "Last folder no longer exists: " + lastFolder + ", using default");
            }
        }

        // Store the last folder file for the analyzer
        final File initialSelectedFile = lastFolderFile;

        worldSelectorButton.addActionListener(e -> {
            int returnVal = chooser.showOpenDialog(MainFormPanel);
            if(returnVal == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();
                MSC.logger.log(Level.INFO,"Selected world: " + selectedFile.getName());

                // Save the selected folder to preferences
                prefs.put(PREF_LAST_FOLDER, selectedFile.getAbsolutePath());
                MSC.logger.log(Level.INFO, "Saved folder preference: " + selectedFile.getAbsolutePath());

                selectedWorldLabel.setText(selectedFile.getAbsolutePath());
                worldSelectorButton.setText("Change World");
                analyzeButton.setEnabled(true);
            }
        });

        analyzeButton.addActionListener(e -> {
            // Use the selected file from chooser, or the initial loaded file
            File fileToAnalyze = chooser.getSelectedFile();
            if (fileToAnalyze == null) {
                fileToAnalyze = initialSelectedFile;
            }

            if (fileToAnalyze != null && fileToAnalyze.exists()) {
                analyzer = new Analyzer(fileToAnalyze, analyzeButton, frame, worldTypeDropdown, analyzerCriteriaDropdown, foundLocations, runMSCButton, totalRegionFileAmount, totalSpace, moreAnalyticsButton, deleteBuggedRegionsCheckBox);
                // Set table model after analyzer finishes
                analyzer.addPropertyChangeListener(evt -> {
                    if ("state".equals(evt.getPropertyName()) && evt.getNewValue() == javax.swing.SwingWorker.StateValue.DONE) {
                        tableModel = analyzer.getCurrentTableModel();
                        MSC.logger.log(Level.INFO, "Analyzer finished, table model set: " + (tableModel != null ? "present" : "null"));
                        if (tableModel != null && searchField != null) {
                            searchField.setText(""); // Clear search when new analysis is done
                            MSC.logger.log(Level.INFO, "Search field cleared and ready for filtering");
                        }
                        if (tableModel != null) {
                            MSC.logger.log(Level.INFO, "Table has " + tableModel.getRowCount() + " total rows");
                        }
                    }
                });
            } else {
                MSC.logger.log(Level.WARNING, "No valid world folder selected for analysis");
                JOptionPane.showMessageDialog(MainFormPanel,
                        "Please select a valid world folder first!",
                        "No World Selected",
                        JOptionPane.WARNING_MESSAGE);
            }
        });

    }


}
