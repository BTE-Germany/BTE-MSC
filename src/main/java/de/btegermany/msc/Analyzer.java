package de.btegermany.msc;

import de.btegermany.msc.exceptions.AnalyzerException;
import de.btegermany.msc.gui.*;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class Analyzer extends SwingWorker<Void, Integer> {

    private File worldFolder;
    private LoadingForm loadingForm;
    private boolean isVanilla;
    private JButton analyzeButton;
    private JFrame frame;
    private int regionFileSize = 0;
    private long speicherplatz = 0;

    private JComboBox worldTypeDropdown;
    private JComboBox analyzeCriteriaDropdown;
    private JTable foundLocationsTable;
    private JLabel totalRegionFileAmount;
    private JLabel totalSpace;

    private JButton runMSCButton;

    public Analyzer(File worldFolder,JButton analyzeButton,JFrame frame, JComboBox worldTypeDropdown,JComboBox analyzeCriteriaDropdown,JTable foundLocationsTable, JButton runMSCButton, JLabel totalRegionFileAmount, JLabel totalSpace){
        this.worldFolder = worldFolder;
        this.analyzeButton = analyzeButton;
        this.worldTypeDropdown = worldTypeDropdown;
        this.analyzeCriteriaDropdown = analyzeCriteriaDropdown;
        this.foundLocationsTable = foundLocationsTable;
        this.totalRegionFileAmount = totalRegionFileAmount;
        this.totalSpace = totalSpace;
        this.runMSCButton = runMSCButton;
        this.frame = frame;
        isVanilla = worldTypeDropdown.getSelectedItem().equals("Vanilla/Anvil");

        loadingForm = new LoadingForm(frame, this);
        JDialog loading = new JDialog(frame, "Analyzing...", true);
        loading.setContentPane(loadingForm.LoadingForm);
        loading.setResizable(false);
        loading.setSize(500, 350);
        loading.setLocationRelativeTo(null);
        loading.setVisible(true);
        loading.toFront();
        loading.repaint();

        loadingForm.progressFinishedButton.addActionListener(e ->{
            loading.dispose();
            loading.setVisible(false);
        });
    }


    @Override
    protected Void doInBackground() throws Exception {
        loadingForm.progressBar.setMaximum(getRegionDirFileCount());

        analyzeButton.setEnabled(false);
        analyzeButton.setText("Analyzing...");

        MSC.logger.log(Level.INFO,"Analyzing Files in "+worldTypeDropdown.getSelectedItem()+" Format.");

        analyze(analyzeCriteriaDropdown, foundLocationsTable);

        MSC.logger.log(Level.INFO,"Found "+regionFileSize+" Region Files.");
        totalRegionFileAmount.setText(regionFileSize+" Files");
        totalSpace.setText(Double.parseDouble(String.format(Locale.ENGLISH, "%1.2f", bytesToGB(speicherplatz)))+" GB");

        loadingForm.progressFinishedButton.setVisible(true);
        analyzeButton.setEnabled(true);
        analyzeButton.setText("Analyze");
        runMSCButton.setVisible(true);
        return null;
    }

    public void analyze(JComboBox analyzeCriteriaDropdown, JTable foundLocationsTable) {
        ArrayList<String> foundLocationsList = new ArrayList<>();
        initialLoad(analyzeCriteriaDropdown, foundLocationsList);

        // Save foundLocationsList without duplicates
        ArrayList<String> foundLocationsListReduced = new ArrayList<>();
        for(String location : foundLocationsList){
            if(!foundLocationsListReduced.contains(location)) {
                foundLocationsListReduced.add(location);
            }
        }

        // List for the table with dataclass LocationListEntry
        ArrayList<LocationListEntry> foundLocationsListEntryList = new ArrayList<>();
        for(String location : foundLocationsListReduced){
            JComboBox jComboBox = new JComboBox();
            jComboBox.addItem("Do nothing");
            jComboBox.addItem("Extract location files to");
            jComboBox.addItem("Delete location files from world");

            JButton jButton = new JButton("Select world");
            jButton.setEnabled(false);

            int amountFilesForLocation = countFilesForLocation(location,foundLocationsList);
            double percentage = (double) amountFilesForLocation / regionFileSize * 100;

            JLabel criteria = new JLabel();
            criteria.setName(analyzeCriteriaDropdown.getSelectedItem().toString());
            criteria.setText(location);

            foundLocationsListEntryList.add(new LocationListEntry(criteria,percentage,new JLabel(amountFilesForLocation + "x"),jComboBox,jButton));

            MSC.logger.log(Level.INFO,+percentage+"% of the region files are in " + location+".");
        }

        setupTableModel(foundLocationsTable, foundLocationsListEntryList);
        new MoreAnalyticsForm(frame,foundLocationsListEntryList);
    }

    /*
        Sets up the table model for foundLocationsTable
     */
    private void setupTableModel(JTable foundLocationsTable, ArrayList<LocationListEntry> foundLocationsListEntryList) {

        // Create the table model and insert data via parameter
        LocationListEntryTableModel locationListEntryTableModel = new LocationListEntryTableModel(foundLocationsListEntryList);
        foundLocationsTable.setModel(locationListEntryTableModel);

        // Sort the table
        TableRowSorter tableRowSorter = locationListEntryTableModel.getTableRowSorter(locationListEntryTableModel);
        foundLocationsTable.setRowSorter(tableRowSorter);

        // Table formatting
        DefaultTableCellRenderer rightAlignmentRenderer = new DefaultTableCellRenderer();
        rightAlignmentRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        DefaultTableCellRenderer centerAlignmentRenderer = new DefaultTableCellRenderer();
        centerAlignmentRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        foundLocationsTable.getColumnModel().getColumn(1).setCellRenderer(rightAlignmentRenderer);
        foundLocationsTable.getColumnModel().getColumn(1).setPreferredWidth(5);
        foundLocationsTable.getColumnModel().getColumn(2).setCellRenderer(rightAlignmentRenderer);
        foundLocationsTable.getColumnModel().getColumn(2).setPreferredWidth(5);
        foundLocationsTable.getColumnModel().getColumn(4).setCellRenderer(centerAlignmentRenderer);


        // Set action dropdown
        JComboBox jComboBox = new JComboBox();
        jComboBox.addItem("Do nothing");
        jComboBox.addItem("Extract location files to");
        jComboBox.addItem("Delete location files from world");

        DefaultCellEditor comboBoxEditor = new DefaultCellEditor(jComboBox);
        comboBoxEditor.setClickCountToStart(1);
        foundLocationsTable.getColumnModel().getColumn(3).setCellEditor(comboBoxEditor);
        foundLocationsTable.getColumnModel().getColumn(3).setCellRenderer(new CheckBoxCellRenderer(jComboBox));


        // Button for selecting world
        //foundLocationsTable.getColumnModel().getColumn(4).setCellRenderer(new ButtonRenderer());
        foundLocationsTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer());
        foundLocationsTable.getColumnModel().getColumn(4).setCellEditor(new ButtonEditor(locationListEntryTableModel, foundLocationsTable.getSelectedColumn()));


    }

    /*
        Gets location from region files and adds them to the foundLocationsList
     */
    private void initialLoad(JComboBox analyzeCriteriaDropdown, ArrayList<String> foundLocationsList) {
        for(String regionFileName : getRegionFileNames()){
            double[] xy = Converter.regionFileToMcCoords(regionFileName);
            if((xy[0] > 2000 || xy[0] < -2000) && (xy[1] > 2000 || xy[1] < -2000)) {
                regionFileSize++;
                double[] latLon = null;
                try {
                    latLon = Converter.toGeo(xy);
                } catch (OutOfProjectionBoundsException e) {
                    System.out.println(regionFileName + " is out of bounds. Skipping...");
                    continue;
                    //throw new AnalyzerException("Out of Projection Bounds: "+ xy[0]+","+xy[1] +" This should not happen. Please report this to the BTE Team.");
                }
                String location = null;
                switch (analyzeCriteriaDropdown.getSelectedItem().toString()){
                    case "Continent":
                        location = Utils.getOfflineLocation(latLon[0],latLon[1]).getContinent();
                        break;
                    case "Country":
                        location = Utils.getOfflineLocation(latLon[0],latLon[1]).getCountry();
                        break;
                    case "State/Province":
                        location = Utils.getOfflineLocation(latLon[0],latLon[1]).getState();
                        break;
                    case "City":
                        location = Utils.getOfflineLocation(latLon[0],latLon[1]).getCity();
                        break;
                }

                System.out.println(regionFileName +" | "+ xy[0]+" "+xy[1] +" | "+latLon[0]+" "+latLon[1] + " | "+location);
                loadingForm.progressLog.append(regionFileName +" | "+ xy[0]+" "+xy[1] +" | "+latLon[0]+" "+latLon[1] + " | "+location + "\n");
                loadingForm.progressLogScrollPane.getVerticalScrollBar().setValue(loadingForm.progressLogScrollPane.getVerticalScrollBar().getMaximum());

                foundLocationsList.add(location);
                loadingForm.progressBar.setValue(foundLocationsList.size());

            }
        }
    }

    /*
        Counts the amount of files for a specific location
    */
    private int countFilesForLocation(String countlocation, ArrayList<String> foundLocationsList){
        int count = 0;
        for(String location : foundLocationsList){
            if(location == null) continue;
            if(location.equals(countlocation)){
                count++;
            }
        }
        return count;
    }

    private int getRegionDirFileCount(){
        File regionFolder;
        if(isVanilla) {
            regionFolder = new File(worldFolder.getAbsolutePath() + "/region");
        } else {
            regionFolder = new File(worldFolder.getAbsolutePath() + "/region3d");
        }
        return regionFolder.listFiles().length;
    }
    /*
        Scan the world folder for region files and return them as a list
     */
    private ArrayList<String> getRegionFileNames(){
        ArrayList<String> regionFileNames = new ArrayList<>();
        if(isVanilla) {
            File regionFolder = new File(worldFolder.getAbsolutePath() + "/region");
            File[] files = regionFolder.listFiles();
            if(files == null) {
                MSC.logger.log(Level.SEVERE,"No region Files found in current world folder. Are you sure this is an Vanilla/Anvil World?");
                throw new AnalyzerException("No region Files found in current world folder. Are you sure this is an Vanilla/Anvil World?");
            }
            for (File file : files) {
                if (file.getName().endsWith(".mca")) {
                    regionFileNames.add(file.getName());
                    speicherplatz += file.length();
                }
            }
        }else{
            File regionFolder = new File(worldFolder.getAbsolutePath() + "/region3d");
            File[] files = regionFolder.listFiles();
            if(files == null) {
                MSC.logger.log(Level.SEVERE,"No region3d Files found in current world folder. Are you sure this is an CubicChunks World?");
                throw new AnalyzerException("No region3d Files found in current world folder. Are you sure this is an CubicChunks World?");
            }
            for (File file : files) {
                if (file.getName().endsWith(".3dr")) {
                    regionFileNames.add(file.getName());
                    speicherplatz += file.length();
                }
            }

        }
        return regionFileNames;
    }

    public static double bytesToGB(long bytes) {
        double kilobytes = bytes / 1024.0;
        double megabytes = kilobytes / 1024.0;
        double gigabytes = megabytes / 1024.0;
        return gigabytes;
    }



    class CheckBoxCellRenderer implements TableCellRenderer {
        JComboBox combo;
        public CheckBoxCellRenderer(JComboBox comboBox) {
            this.combo = new JComboBox();
            for (int i=0; i<comboBox.getItemCount(); i++){
                combo.addItem(comboBox.getItemAt(i));
            }
        }
        public Component getTableCellRendererComponent(JTable jtable, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            combo.setSelectedItem(value);
            return combo;
        }
    }

}
