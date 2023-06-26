package de.btegermany.msc.gui;

import javax.swing.*;

public class LocationListEntry {

    private JLabel location;

    private int percentage;
    private JLabel amount;
    private JComboBox<String> comboBox;


    JButton world;

    public LocationListEntry(JLabel location,int percentage, JLabel amount, JComboBox<String> comboBox, JButton world) {
        this.location = location;
        this.percentage = percentage;
        this.amount = amount;
        this.comboBox = comboBox;
        this.world = world;
    }

    public void setLocation(JLabel location) {
        this.location = location;
    }

    public void setAmount(JLabel amount) {
        this.amount = amount;
    }

    public void setComboBox(JComboBox<String> comboBox) {
        this.comboBox = comboBox;
    }

    public void setWorld(JButton world) {
        this.world = world;
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }

    public int getPercentage() {
        return percentage;
    }

    public JButton getWorld() {
        return world;
    }

    public JLabel getLocation() {
        return location;
    }

    public JLabel getAmount() {
        return amount;
    }

    public JComboBox<String> getComboBox() {
        return comboBox;
    }
}
