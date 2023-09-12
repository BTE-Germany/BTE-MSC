package de.btegermany.msc.gui;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;

import java.awt.*;


public class ButtonRenderer extends JButton implements TableCellRenderer {


    private JButton button;

    public ButtonRenderer(JButton jButton) {
        super();
        this.button = new JButton();
        button.setOpaque(true);
    }


    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (isSelected) {
            button.setForeground(table.getForeground());
            button.setBackground(UIManager.getColor("Button.background"));
        } else {
            button.setForeground(table.getSelectionForeground());
            button.setBackground(table.getSelectionBackground());
        }



        button.setText((value == null) ? "" : value.toString());
        return button;
    }
}