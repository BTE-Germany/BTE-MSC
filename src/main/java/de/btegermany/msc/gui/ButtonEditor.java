package de.btegermany.msc.gui;

import de.btegermany.msc.MSC;
import de.btegermany.msc.Utils;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.logging.Level;

public class ButtonEditor extends DefaultCellEditor {

    private JButton button;
    private String label;
    private String selectedDirectory;
    private boolean isPushed;

    public ButtonEditor(JButton button,LocationListEntryTableModel locationListEntryTableModel, int selectedRow){
        super(new JCheckBox());
        this.button = button;
        button.setOpaque(true);
        button.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setCurrentDirectory(new File(Utils.getMinecraftDir("buildtheearth").toFile().getAbsolutePath() + "/saves"));


            int returnVal = chooser.showOpenDialog(null);
            if(returnVal == JFileChooser.APPROVE_OPTION) {
                MSC.logger.log(Level.INFO,"Selected world: "+chooser.getSelectedFile().getName());
                selectedDirectory = chooser.getSelectedFile().getAbsolutePath();
                button.setText(chooser.getSelectedFile().getAbsolutePath());
                button.setName(chooser.getSelectedFile().getAbsolutePath());
            }
            // Stop cell editing to save the changes
            stopCellEditing();
        });
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        return button;
    }

    @Override
    public Object getCellEditorValue() {
        return selectedDirectory;
    }


}