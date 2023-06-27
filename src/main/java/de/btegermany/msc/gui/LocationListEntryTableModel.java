package de.btegermany.msc.gui;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class LocationListEntryTableModel extends AbstractTableModel {

    ArrayList<LocationListEntry> foundLocationsList;

    public LocationListEntryTableModel(ArrayList<LocationListEntry> foundLocationsList) {
        this.foundLocationsList = foundLocationsList;
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case 0 -> foundLocationsList.get(0).getLocation().getName();
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
            case 1 -> Double.parseDouble(String.format(Locale.ENGLISH, "%1.2f", foundLocationsList.get(rowIndex).getPercentage()));
            case 2 -> foundLocationsList.get(rowIndex).getAmount().getText();
            case 3 -> foundLocationsList.get(rowIndex).getComboBox().getSelectedItem().toString();
            case 4 -> foundLocationsList.get(rowIndex).getComboBox().getSelectedItem().toString() == "Extract location files to"?foundLocationsList.get(rowIndex).getWorld().getText():"";
            default -> throw new IllegalStateException("Unexpected value: " + columnIndex);
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 3 || (columnIndex == 4 && foundLocationsList.get(rowIndex).getComboBox().getSelectedItem().toString() == "Extract location files to");
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if(columnIndex == 3){
            foundLocationsList.get(rowIndex).getComboBox().setSelectedItem(aValue);
        }
        fireTableCellUpdated(rowIndex, columnIndex);
    }


    public static TableRowSorter getTableRowSorter(LocationListEntryTableModel locationListEntryTableModel) {
        TableRowSorter tableRowSorter = new TableRowSorter(locationListEntryTableModel);
        tableRowSorter.setComparator(1, (value1, value2) -> {
            Double number1 = (Double) value1;
            Double number2 = (Double) value2;
            return Double.compare(number1, number2);
        });
        tableRowSorter.setComparator(2, (value1, value2) -> {
            int number1 = Integer.parseInt(((String) value1).replace("x",""));
            int number2 = Integer.parseInt(((String) value2).replace("x",""));
            return Integer.compare(number1, number2);
        });
        tableRowSorter.setComparator(0, (value1, value2) -> {
            String string1 = (String) value1;
            String string2 = (String) value2;
            return string1.compareTo(string2);
        });
        tableRowSorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(1, SortOrder.DESCENDING)));
        return tableRowSorter;
    }
}
