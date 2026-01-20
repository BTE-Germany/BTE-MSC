package de.btegermany.msc.gui;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class LocationListEntryTableModel extends AbstractTableModel {

    ArrayList<LocationListEntry> foundLocationsList;
    private String filterText = "";

    public LocationListEntryTableModel(ArrayList<LocationListEntry> foundLocationsList) {
        this.foundLocationsList = foundLocationsList;
    }

    public void setFilterText(String filterText) {
        this.filterText = filterText != null ? filterText.toLowerCase().trim() : "";
        fireTableDataChanged();
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case 0 -> {
                if (foundLocationsList.isEmpty() || foundLocationsList.get(0).getLocation() == null) {
                    yield "Location";
                }
                String name = foundLocationsList.get(0).getLocation().getName();
                yield name != null ? name : "Location";
            }
            case 1 -> "%";
            case 2 -> "Amount";
            case 3 -> "Action";
            case 4 -> "World";
            default -> throw new IllegalStateException("Unexpected value: " + column);
        };
    }

    @Override
    public int getRowCount() {
        if (filterText.isEmpty()) {
            return foundLocationsList.size();
        }

        return (int) foundLocationsList.stream()
                .filter(this::entryMatchesFilter)
                .count();
    }

    @Override
    public int getColumnCount() {
        return 5;
    }

    private LocationListEntry getFilteredEntry(int filteredIndex) {
        if (filterText.isEmpty()) {
            if (filteredIndex >= 0 && filteredIndex < foundLocationsList.size()) {
                return foundLocationsList.get(filteredIndex);
            }
            return null;
        }

        int count = 0;
        for (LocationListEntry entry : foundLocationsList) {
            if (entryMatchesFilter(entry)) {
                if (count == filteredIndex) {
                    return entry;
                }
                count++;
            }
        }
        return null;
    }

    /**
     * Check if an entry matches the current filter text
     * Searches across all columns (Location, Amount, %, Action, World)
     */
    private boolean entryMatchesFilter(LocationListEntry entry) {
        if (filterText.isEmpty()) {
            return true;
        }

        if (entry == null) {
            return false;
        }

        // Search location column (column 0)
        if (entry.getLocation() != null && entry.getLocation().getText() != null) {
            if (entry.getLocation().getText().toLowerCase().contains(filterText)) {
                return true;
            }
        }

        // Search amount column (column 2)
        if (entry.getAmount() != null && entry.getAmount().getText() != null) {
            if (entry.getAmount().getText().toLowerCase().contains(filterText)) {
                return true;
            }
        }

        // Search percentage column (column 1)
        String percentStr = String.format(Locale.ENGLISH, "%1.2f", entry.getPercentage()).toLowerCase();
        if (percentStr.contains(filterText)) {
            return true;
        }

        // Search action column (column 3)
        if (entry.getComboBox() != null && entry.getComboBox().getSelectedItem() != null) {
            if (entry.getComboBox().getSelectedItem().toString().toLowerCase().contains(filterText)) {
                return true;
            }
        }

        // Search world column (column 4)
        if (entry.getWorld() != null && entry.getWorld().getText() != null) {
            return entry.getWorld().getText().toLowerCase().contains(filterText);
        }

        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        LocationListEntry entry = getFilteredEntry(rowIndex);
        if (entry == null) return "";

        return switch (columnIndex) {
            case 0 -> {
                if (entry.getLocation() == null) yield "";
                String text = entry.getLocation().getText();
                yield text != null ? text : "";
            }
            case 1 -> Double.parseDouble(String.format(Locale.ENGLISH, "%1.2f", entry.getPercentage()));
            case 2 -> {
                if (entry.getAmount() == null) yield "";
                String amount = entry.getAmount().getText();
                yield amount != null ? amount : "";
            }
            case 3 -> {
                if (entry.getComboBox() == null || entry.getComboBox().getSelectedItem() == null) yield "";
                yield entry.getComboBox().getSelectedItem().toString();
            }
            case 4 -> {
                if (entry.getComboBox() == null || entry.getComboBox().getSelectedItem() == null) yield "";
                if (entry.getComboBox().getSelectedItem().toString().equals("Extract location files to")) {
                    if (entry.getWorld() == null) yield "";
                    String world = entry.getWorld().getText();
                    yield world != null ? world : "";
                }
                yield "";
            }
            default -> throw new IllegalStateException("Unexpected value: " + columnIndex);
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        LocationListEntry entry = getFilteredEntry(rowIndex);
        if (entry == null || entry.getComboBox() == null) return false;
        Object selectedItem = entry.getComboBox().getSelectedItem();
        if (selectedItem == null) return false;
        return columnIndex == 3 || (columnIndex == 4 && selectedItem.toString().equals("Extract location files to"));
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        LocationListEntry entry = getFilteredEntry(rowIndex);
        if (entry == null) return;

        if(columnIndex == 3){
            entry.getComboBox().setSelectedItem(aValue);
        }else if(columnIndex == 4){
            entry.getWorld().setText(aValue == null ? "Select world" : aValue.toString());
        }
        fireTableCellUpdated(rowIndex, columnIndex);
    }


    /**
     * Get total row count ignoring any filters (for MSC operations)
     */
    public int getTotalRowCount() {
        return foundLocationsList.size();
    }

    /**
     * Get entry at index from the full list (ignoring filter, for MSC operations)
     */
    public LocationListEntry getEntryAt(int index) {
        if (index >= 0 && index < foundLocationsList.size()) {
            return foundLocationsList.get(index);
        }
        return null;
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
        tableRowSorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(2, SortOrder.DESCENDING)));
        return tableRowSorter;
    }
}
