package de.btegermany.msc;

import de.btegermany.msc.gui.*;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Stream;

public class Analyzer extends SwingWorker<Void, Integer> {

    private File worldFolder;
    private LoadingForm loadingForm;
    private JDialog loading;
    private boolean isVanilla;
    private JButton analyzeButton;
    private JFrame frame;
    private int regionFileSize = 0;
    private long speicherplatz = 0;

    private JComboBox worldTypeDropdown;
    private JComboBox analyzeCriteriaDropdown;
    private JTable foundLocationsTable;

    private HashMap<String, List<String>> locationCache = new HashMap<>();

    private HashMap<String, List<String>> location2dCache = new HashMap<>();

    private JLabel totalRegionFileAmount;
    private JLabel totalSpace;

    private JButton runMSCButton;

    private JButton moreAnalyticsButton;

    private JButton selectWorldToMove;

    public Analyzer(File worldFolder,JButton analyzeButton,JFrame frame, JComboBox worldTypeDropdown,JComboBox analyzeCriteriaDropdown,JTable foundLocationsTable, JButton runMSCButton, JLabel totalRegionFileAmount, JLabel totalSpace, JButton moreAnalyticsButton){
        this.worldFolder = worldFolder;
        this.analyzeButton = analyzeButton;
        this.worldTypeDropdown = worldTypeDropdown;
        this.analyzeCriteriaDropdown = analyzeCriteriaDropdown;
        this.foundLocationsTable = foundLocationsTable;
        this.totalRegionFileAmount = totalRegionFileAmount;
        this.totalSpace = totalSpace;
        this.moreAnalyticsButton = moreAnalyticsButton;
        this.runMSCButton = runMSCButton;
        this.frame = frame;
        isVanilla = worldTypeDropdown.getSelectedItem().equals("Vanilla/Anvil");
        runMSCButton.setEnabled(true);
        runMSCButton.setText("Run MSC");

        loadingForm = new LoadingForm(frame, this);
        JDialog loading = new JDialog(frame, "Analyzing...", true);
        loading.setContentPane(loadingForm.LoadingForm);
        loading.setResizable(false);
        loading.setSize(500, 350);
        loading.setLocationRelativeTo(null);
        loading.setVisible(true);
        loading.toFront();
        loading.repaint();

        runMSCButton.addActionListener(e -> {
            for( ActionListener al : runMSCButton.getActionListeners() ) {
                runMSCButton.removeActionListener( al );
            }
            MSC.logger.log(Level.INFO, "Started MSC");
            //runMSCButton.setVisible(false);
            runMSCButton.setEnabled(false);
            runMSCButton.setText("Please Run Analyze before running MSC again!");
            runMSC();
        });
    }


    public void runMSC(){
        int rowCount = foundLocationsTable.getRowCount();
        for(int row = 0; row < rowCount; row++){
            String action = "Do nothing";
            if((action = foundLocationsTable.getValueAt(row, 3).toString()) != null){
                String toWorld = foundLocationsTable.getValueAt(row, 4).toString();
                String regionFolder = null;
                ArrayList<String> regionFiles2d = new ArrayList<>();
                if(isVanilla) {
                    regionFolder = "/region";
                }else {
                    regionFolder = "/region3d";
                }

                if(action.equals("Extract location files to")){

                    //Move Files to toWorld file
                    for(String absoluteRegionFile : locationCache.get(foundLocationsTable.getValueAt(row, 0).toString())){
                        if(!isVanilla){
                            String regionFile2dName = Converter.regionFile3dTo2d(absoluteRegionFile);
                            if(!regionFiles2d.contains(regionFile2dName)){
                                regionFiles2d.add(regionFile2dName);
                            }
                        }
                        Utils.moveFile(worldFolder.getAbsolutePath()+regionFolder+"/"+absoluteRegionFile, toWorld + regionFolder);
                    }
                    // move 2d files if cc world
                    if(!regionFiles2d.isEmpty()) {
                        for (String regionFile2dName : regionFiles2d) {
                            Utils.moveFile(worldFolder.getAbsolutePath()+"/region2d/"+regionFile2dName, toWorld + "/region2d");
                        }
                    }
                }else if(action.equals("Delete location files")) {
                    // Delete Files from original world
                    for(String absoluteRegionFile : locationCache.get(foundLocationsTable.getValueAt(row, 0).toString())){
                        if(!isVanilla){
                            String regionFile2dName = Converter.regionFile3dTo2d(absoluteRegionFile);
                            if(!regionFiles2d.contains(regionFile2dName)){
                                regionFiles2d.add(regionFile2dName);
                            }
                        }
                        Utils.deleteFile(worldFolder.getAbsolutePath()+regionFolder+"/"+absoluteRegionFile);
                    }
                    // delete 2d files if cc world
                    if(!regionFiles2d.isEmpty()) {
                        for (String regionFile2dName : regionFiles2d) {
                            Utils.deleteFile(worldFolder.getAbsolutePath() + "/region2d/" + regionFile2dName);
                        }
                    }

                }

                System.out.println(foundLocationsTable.getColumnName(0)+" "+foundLocationsTable.getValueAt(row, 0)+ " -> "+ foundLocationsTable.getValueAt(row, 3) + " "+toWorld);
                System.out.println("Regionfiles" + Arrays.toString(locationCache.get(foundLocationsTable.getValueAt(row, 0).toString()).toArray()));
                if(!isVanilla){
                    System.out.println("Regionfiles2d" + Arrays.toString(regionFiles2d.toArray()) + "\n");
                }
            }
        }

        MSC.logger.log(Level.FINE, "Successfully run msc on " + foundLocationsTable.getRowCount() + " locations");
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

        loadingForm.progressLabel.setText("Finished!");
        moreAnalyticsButton.setVisible(true);
        moreAnalyticsButton.setEnabled(true);
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
            jComboBox.addItem("Delete location files");

            selectWorldToMove = new JButton("Select world");
            selectWorldToMove.setEnabled(false);

            int amountFilesForLocation = countFilesForLocation(location,foundLocationsList);
            double percentage = (double) amountFilesForLocation / regionFileSize * 100;

            JLabel criteria = new JLabel();
            criteria.setName(analyzeCriteriaDropdown.getSelectedItem().toString());
            criteria.setText(location);

            foundLocationsListEntryList.add(new LocationListEntry(criteria,percentage,new JLabel(amountFilesForLocation + "x"),jComboBox,selectWorldToMove));

            MSC.logger.log(Level.INFO,+percentage+"% of the region files are in " + location+".");
        }

        setupTableModel(foundLocationsTable, foundLocationsListEntryList);

        MoreAnalyticsForm moreAnalyticsForm = new MoreAnalyticsForm(frame,foundLocationsListEntryList);
        moreAnalyticsButton.addActionListener(e -> {
            moreAnalyticsForm.getMoreAnalyticsDialog().setVisible(true);;
        });

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
        jComboBox.addItem("Delete location files");

        DefaultCellEditor comboBoxEditor = new DefaultCellEditor(jComboBox);
        comboBoxEditor.setClickCountToStart(1);
        foundLocationsTable.getColumnModel().getColumn(3).setCellEditor(comboBoxEditor);
        foundLocationsTable.getColumnModel().getColumn(3).setCellRenderer(new CheckBoxCellRenderer(jComboBox));

        // Button for selecting world to move files to
        JButton selectWorldToMove = new JButton("Select world");

        foundLocationsTable.getColumnModel().getColumn(4).setCellRenderer(new ButtonRenderer(selectWorldToMove));
        //foundLocationsTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer());
        foundLocationsTable.getColumnModel().getColumn(4).setCellEditor(new ButtonEditor(selectWorldToMove,locationListEntryTableModel, foundLocationsTable.getSelectedColumn()));

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

                //System.out.println(location);


                if(this.locationCache.get(location) == null){
                    List<String> tempList = new ArrayList<>();
                    tempList.add(regionFileName);
                    this.locationCache.put(location, tempList);
                }else{
                    List<String> tempList = this.locationCache.get(location);
                    tempList.add(regionFileName);
                    this.locationCache.put(location, tempList);
                }

                //System.out.println(regionFileName +" | "+ xy[0]+" "+xy[1] +" | "+latLon[0]+" "+latLon[1] + " | "+location);
                loadingForm.progressLog.append(regionFileName +" | "+ xy[0]+" "+xy[1] +" | "+latLon[0]+" "+latLon[1] + " | "+location + "\n");
                loadingForm.progressLogScrollPane.getVerticalScrollBar().setValue(loadingForm.progressLogScrollPane.getVerticalScrollBar().getMaximum());

                foundLocationsList.add(location);
                loadingForm.progressBar.setValue(foundLocationsList.size());

            }
        }
        // adds 2d region files
        if(!isVanilla){
            for(String regionFileName2D : get2DRegionFileNames()){

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
        loadingForm.progressLabel.setText("Indexing region files... This process can take a bit.");
        ArrayList<String> regionFileNames = new ArrayList<>();
        if(isVanilla) {
            File regionFolder = new File(worldFolder.getAbsolutePath() + "/region");
           /*
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
            */
            try (Stream<Path> paths = Files.list(regionFolder.toPath())) {
                paths.filter(path -> path.toString().endsWith(".mca"))
                        .forEach(path -> {
                            regionFileNames.add(path.getFileName().toString());
                            speicherplatz += path.toFile().length();
                        });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            File regionFolder = new File(worldFolder.getAbsolutePath() + "/region3d");
            /*
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
            */
            try (Stream<Path> paths = Files.list(regionFolder.toPath())) {
                paths.filter(path -> path.toString().endsWith(".3dr"))
                        .forEach(path -> {
                            regionFileNames.add(path.getFileName().toString());
                            speicherplatz += path.toFile().length();
                        });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return regionFileNames;
    }

    private ArrayList<String> get2DRegionFileNames(){
        loadingForm.progressLabel.setText("Indexing region files... This process can take a bit.");
        ArrayList<String> regionFileNames = new ArrayList<>();
        File regionFolder = new File(worldFolder.getAbsolutePath() + "/region2d");

        try (Stream<Path> paths = Files.list(regionFolder.toPath())) {
            paths.filter(path -> path.toString().endsWith(".2dr"))
                    .forEach(path -> {
                        regionFileNames.add(path.getFileName().toString());
                    });
        } catch (IOException e) {
            e.printStackTrace();
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
