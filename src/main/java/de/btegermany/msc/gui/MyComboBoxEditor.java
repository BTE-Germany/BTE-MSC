package de.btegermany.msc.gui;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MyComboBoxEditor extends AbstractCellEditor implements TableCellEditor {
    private JComboBox<String> comboBox;


    public MyComboBoxEditor() {
        comboBox = new JComboBox<>();
        comboBox.addItem("Do nothing");
        comboBox.addItem("Extract location files to");
        comboBox.addItem("Delete location files from world");

        comboBox.addActionListener(e -> stopCellEditing());
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        comboBox.setSelectedItem(value);
        return comboBox;
    }

    @Override
    public boolean stopCellEditing() {
        Object newValue = comboBox.getSelectedItem();
        comboBox.setSelectedItem(newValue);
        fireEditingStopped();
        return super.stopCellEditing();
    }

    @Override
    public Object getCellEditorValue() {
        return comboBox.getSelectedItem();
    }
}