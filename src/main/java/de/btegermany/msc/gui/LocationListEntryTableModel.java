package de.btegermany.msc.gui;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class LocationListEntryTableModel extends AbstractTableModel {

    ArrayList<LocationListEntry> foundLocationsList;

    public LocationListEntryTableModel(ArrayList<LocationListEntry> foundLocationsList) {
        this.foundLocationsList = foundLocationsList;
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case 0 -> "Location";
            case 1 -> "%";
            case 2 -> "Amount";
            case 3 -> "Action";
            case 4 -> "World";
            default -> throw new IllegalStateException("Unexpected value: " + column);
        };
    }

    @Override
    public int getRowCount() {
        return foundLocationsList.size();
    }

    @Override
    public int getColumnCount() {
        return 5;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return switch (columnIndex) {
            case 0 -> foundLocationsList.get(rowIndex).getLocation().getText();
            case 1 -> foundLocationsList.get(rowIndex).getPercentage();
            case 2 -> foundLocationsList.get(rowIndex).getAmount().getText();
            case 3 -> foundLocationsList.get(rowIndex).getComboBox().getSelectedItem();
            case 4 -> foundLocationsList.get(rowIndex).getWorld().getText();
            default -> throw new IllegalStateException("Unexpected value: " + columnIndex);
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 3 || columnIndex == 4;
    }

}
