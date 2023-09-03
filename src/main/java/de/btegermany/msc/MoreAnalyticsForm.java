package de.btegermany.msc;

import com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme;
import de.btegermany.msc.gui.LocationListEntry;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;
import org.knowm.xchart.XChartPanel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

public class MoreAnalyticsForm {
    ArrayList<LocationListEntry> foundLocationsListEntryList;
    JDialog loading;

    public MoreAnalyticsForm(JFrame frame, ArrayList<LocationListEntry> foundLocationsListEntryList) {
        this.foundLocationsListEntryList = foundLocationsListEntryList;
        this.sortArrayList();
        //JFreeChart chart = createChart(createDataset( ) );
        loading = new JDialog(frame, "More Analytics", true);
        loading.setContentPane(new XChartPanel<>(createChart()));
        loading.setResizable(false);
        loading.setSize(600, 500);
        loading.setLocationRelativeTo(null);
        loading.toFront();
        loading.repaint();
    }

    public JDialog getMoreAnalyticsDialog() {
        return loading;
    }

    private PieChart createChart() {
        PieChart chart = new PieChartBuilder().title("Server by regions").build();
        chart.getStyler().setLegendVisible(true);
        chart.getStyler().setChartTitleVisible(false);
        Color color = UIManager.getColor("Editor.background");
        chart.getStyler().setLegendBackgroundColor(color);
        chart.getStyler().setChartBackgroundColor(color);
        chart.getStyler().setPlotBackgroundColor(color);
        chart.getStyler().setPlotBorderVisible(false);
        chart.getStyler().setLabelsFontColor(Color.WHITE);
        chart.getStyler().setChartFontColor(Color.WHITE);
        chart.getStyler().setLegendBorderColor(color);
        chart.getStyler().setLabelsFont(UIManager.getFont("Button.font"));
        chart.getStyler().setLegendFont(UIManager.getFont("Button.font"));


        for (int i = 0; i < foundLocationsListEntryList.size(); i++) {
            if (i < 10) {
                chart.addSeries(foundLocationsListEntryList.get(i).getLocation().getText(), Double.parseDouble(String.format(Locale.ENGLISH, "%1.2f", foundLocationsListEntryList.get(i).getPercentage())));
            } else {
                chart.addSeries("Other", Double.parseDouble(String.format(Locale.ENGLISH, "%1.2f", foundLocationsListEntryList.get(i).getPercentage())));
            }
        }

        return chart;
    }


    private void sortArrayList() {
        // sort after amount of region files
        Collections.sort(foundLocationsListEntryList, (o1, o2) -> o2.getAmount().getText().compareTo(o1.getAmount().getText()));
    }
}
