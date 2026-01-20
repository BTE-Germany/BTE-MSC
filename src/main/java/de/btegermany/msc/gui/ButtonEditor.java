package de.btegermany.msc.gui;

import de.btegermany.msc.MSC;
import de.btegermany.msc.Utils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.logging.Level;
import java.util.prefs.Preferences;

public class ButtonEditor extends DefaultCellEditor {

    private final JButton button;
    private String selectedDirectory = null;
    private static File currentAnalyzedWorldFolder = null;

    // Preferences for storing last extract destination
    private static final Preferences prefs = Preferences.userNodeForPackage(ButtonEditor.class);
    private static final String PREF_LAST_EXTRACT_DEST = "lastExtractDestination";

    public ButtonEditor(JButton button) {
        super(new JCheckBox());
        this.button = button;
        button.setOpaque(true);
        button.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            // Smart directory initialization:
            // 1. If there's a saved extract destination that exists, use its parent
            // 2. Else if we have the current analyzed world folder, use its parent
            // 3. Else fall back to default Minecraft saves folder
            String lastExtractDest = prefs.get(PREF_LAST_EXTRACT_DEST, null);
            File initialDir = null;

            if (lastExtractDest != null && new File(lastExtractDest).exists()) {
                initialDir = new File(lastExtractDest).getParentFile();
                MSC.logger.log(Level.INFO, "Using last extract destination parent: " + initialDir);
            } else if (currentAnalyzedWorldFolder != null && currentAnalyzedWorldFolder.getParentFile() != null) {
                initialDir = currentAnalyzedWorldFolder.getParentFile();
                MSC.logger.log(Level.INFO, "Using analyzed world parent folder: " + initialDir);
            } else {
                initialDir = new File(Utils.getMinecraftDir("buildtheearth").toFile().getAbsolutePath() + "/saves");
                MSC.logger.log(Level.INFO, "Using default Minecraft saves folder");
            }

            chooser.setCurrentDirectory(initialDir);

            // Pre-select last extract destination if it exists
            if (lastExtractDest != null && new File(lastExtractDest).exists()) {
                chooser.setSelectedFile(new File(lastExtractDest));
            }

            int returnVal = chooser.showOpenDialog(null);
            if(returnVal == JFileChooser.APPROVE_OPTION) {
                selectedDirectory = chooser.getSelectedFile().getAbsolutePath();
                button.setText(selectedDirectory);
                button.setName(selectedDirectory);

                // Save the selected destination for next time
                prefs.put(PREF_LAST_EXTRACT_DEST, selectedDirectory);
                MSC.logger.log(Level.INFO, "Selected destination world: " + selectedDirectory);
            } else {
                MSC.logger.log(Level.INFO, "World selection cancelled");
            }
            // Stop cell editing to save the changes
            fireEditingStopped();
        });
    }

    /**
     * Set the current analyzed world folder for smart directory navigation
     */
    public static void setCurrentAnalyzedWorldFolder(File worldFolder) {
        currentAnalyzedWorldFolder = worldFolder;
        MSC.logger.log(Level.INFO, "Set current analyzed world folder: " + worldFolder);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        // Initialize with current value if available
        if (value != null && !value.toString().isEmpty() && !value.toString().equals("Select world")) {
            selectedDirectory = value.toString();
            button.setText(selectedDirectory);
        } else {
            button.setText("Select world");
        }
        return button;
    }

    @Override
    public Object getCellEditorValue() {
        MSC.logger.log(Level.FINE, "ButtonEditor returning value: " + selectedDirectory);
        return selectedDirectory;
    }


}