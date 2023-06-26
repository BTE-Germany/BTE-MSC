package de.btegermany.msc.gui;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MyComboBoxEditor extends AbstractCellEditor implements TableCellEditor {
    private JComboBox<String> comboBox;

    public MyComboBoxEditor() {
        comboBox = new JComboBox<>();
        comboBox.addItem("Extract location files to");
        comboBox.addItem("Do nothing");
        comboBox.addItem("Delete location files from world");
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        comboBox.setSelectedItem(value);
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopCellEditing();
            }
        });
        return comboBox;
    }

    @Override
    public Object getCellEditorValue() {
        return comboBox.getSelectedItem();
    }
}