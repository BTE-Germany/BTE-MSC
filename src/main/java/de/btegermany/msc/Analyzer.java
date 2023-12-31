package de.btegermany.msc;

import de.btegermany.msc.exceptions.AnalyzerException;
import de.btegermany.msc.gui.*;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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

    private JCheckBox deleteBuggedRegionsCheckBox;

    private JButton selectWorldToMove;

    public Analyzer(File worldFolder,JButton analyzeButton,JFrame frame, JComboBox worldTypeDropdown,JComboBox analyzeCriteriaDropdown,JTable foundLocationsTable, JButton runMSCButton, JLabel totalRegionFileAmount, JLabel totalSpace, JButton moreAnalyticsButton, JCheckBox deleteBuggedRegionsCheckBox){
        this.worldFolder = worldFolder;
        this.analyzeButton = analyzeButton;
        this.worldTypeDropdown = worldTypeDropdown;
        this.analyzeCriteriaDropdown = analyzeCriteriaDropdown;
        this.foundLocationsTable = foundLocationsTable;
        this.totalRegionFileAmount = totalRegionFileAmount;
        this.totalSpace = totalSpace;
        this.moreAnalyticsButton = moreAnalyticsButton;
        this.runMSCButton = runMSCButton;
        this.deleteBuggedRegionsCheckBox = deleteBuggedRegionsCheckBox;
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
        MSC.logger.log(Level.INFO, "MSC will run with "+rowCount + " " +foundLocationsTable.getColumnName(0)+".");
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
                    rowCount--;
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
                    rowCount--;
                }

                MSC.logger.log(Level.INFO,foundLocationsTable.getColumnName(0)+" "+foundLocationsTable.getValueAt(row, 0)+ " -> "+ foundLocationsTable.getValueAt(row, 3) + " "+toWorld);
                MSC.logger.log(Level.INFO,"Manipulated Regionfiles: " + Arrays.toString(locationCache.get(foundLocationsTable.getValueAt(row, 0).toString()).toArray()).replace("[","").replace("]",""));

                if(!isVanilla){
                    MSC.logger.log(Level.INFO,"Manipulated Regionfiles2d" + Arrays.toString(regionFiles2d.toArray()).replace("[","").replace("]",""));
                }
            }
        }

        MSC.logger.log(Level.FINE, "Successfully ran MSC on " + rowCount + " " +foundLocationsTable.getColumnName(0)+".");
    }


    @Override
    protected Void doInBackground() {

        analyzeButton.setEnabled(false);
        analyzeButton.setText("Analyzing...");

        MSC.logger.log(Level.INFO,"Analyzing Files in "+worldTypeDropdown.getSelectedItem()+" Format.");

        analyze(analyzeCriteriaDropdown, foundLocationsTable);

        MSC.logger.log(Level.INFO,"Found "+regionFileSize+" Region Files.");
        totalRegionFileAmount.setText(regionFileSize+" Files");
        totalSpace.setText(Double.parseDouble(String.format(Locale.ENGLISH, "%1.2f", bytesToGB(speicherplatz)))+" GB");

        loadingForm.progressLabel.setText("Finished!");
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


        // TODO: Fix this, broken
        /*MoreAnalyticsForm moreAnalyticsForm = new MoreAnalyticsForm(frame,foundLocationsListEntryList);
        moreAnalyticsButton.addActionListener(e -> {
            moreAnalyticsForm.getMoreAnalyticsDialog().setVisible(true);;
        });*/

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
        // extract osm-location-data.bin before analyzing
        File osmLocationData = new File("osm-location-data.bin");
        if(!osmLocationData.exists()) {
            try (InputStream inputStream = MSC.class.getResourceAsStream("/osm-location-data.bin")) {
                try (FileOutputStream outputStream = new FileOutputStream(osmLocationData)) {
                    int read;
                    byte[] bytes = new byte[1024];
                    while ((read = inputStream.read(bytes)) != -1) {
                        outputStream.write(bytes, 0, read);
                    }
                }
            } catch (IOException e) {
                throw new AnalyzerException("osm-location-data.bin cannot be extracted. Please report this to the BTE Team.");
            }
        }
        for (String regionFileName : getRegionFileNames()) {
            double[] xy = Converter.regionFileToMcCoords(regionFileName);
            if ((xy[0] > 2000 || xy[0] < -2000) && (xy[1] > 2000 || xy[1] < -2000)) {
                regionFileSize++;
                double[] latLon = null;
                try {
                    latLon = Converter.toGeo(xy);
                } catch (OutOfProjectionBoundsException e) {
                    System.out.println(regionFileName + " is out of bounds. Skipping...");
                    // TODO: Substract skipped files from regionFileSize and progressBar
                    loadingForm.progressBar.setMaximum(loadingForm.progressBar.getMaximum()-1);
                    continue;
                    //throw new AnalyzerException("Out of Projection Bounds: "+ xy[0]+","+xy[1] +" This should not happen. Please report this to the BTE Team.");
                }
                InputStream inputStream = MSC.class.getResourceAsStream("/osm-location-data.bin");
                String location = null;
                switch (analyzeCriteriaDropdown.getSelectedItem().toString()) {
                    case "Continent":
                        location = Utils.getOfflineLocation(latLon[0], latLon[1]).getContinent();
                        break;
                    case "Country":
                        location = Utils.getOfflineLocation(latLon[0], latLon[1]).getCountry();
                        break;
                    case "State/Province":
                        location = Utils.getOfflineLocation(latLon[0], latLon[1]).getState();
                        break;
                    case "City":
                        location = Utils.getOfflineLocation(latLon[0], latLon[1]).getCity();
                        break;
                }
                //System.out.println(location);


                if (this.locationCache.get(location) == null) {
                    List<String> tempList = new ArrayList<>();
                    tempList.add(regionFileName);
                    this.locationCache.put(location, tempList);
                } else {
                    List<String> tempList = this.locationCache.get(location);
                    tempList.add(regionFileName);
                    this.locationCache.put(location, tempList);
                }

                //System.out.println(regionFileName +" | "+ xy[0]+" "+xy[1] +" | "+latLon[0]+" "+latLon[1] + " | "+location);
                loadingForm.progressLog.append(regionFileName + " | " + xy[0] + " " + xy[1] + " | " + latLon[0] + " " + latLon[1] + " | " + location + "\n");
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

    /*
        Scan the world folder for region files and return them as a list
     */
    private ArrayList<String> getRegionFileNames(){
        loadingForm.progressLabel.setText("Indexing region files... This process can take a bit.");
        ArrayList<String> regionFileNames = new ArrayList<>();
        if(isVanilla) {
            File regionFolder = new File(worldFolder.getAbsolutePath() + "/region");
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
            AtomicInteger deletedBuggedRegions = new AtomicInteger();
            File regionFolder = new File(worldFolder.getAbsolutePath() + "/region3d");
            try (Stream<Path> paths = Files.list(regionFolder.toPath())) {

                paths.filter(path -> path.toString().endsWith(".3dr"))
                        .forEach(path -> {
                            // TODO: fix 100.000x the same 3dr file
                            String[] parts = path.getFileName().toString().split("\\.");
                            // skips all region files that are higher than 40*256 or lower than -40*256
                            int h = Integer.parseInt(parts[1]);
                            if(h > -40 &&  h < 40) {
                                regionFileNames.add(path.getFileName().toString());
                                speicherplatz += path.toFile().length();
                            } else if(deleteBuggedRegionsCheckBox.isSelected()) {
                                // Delete bugged regions
                                Utils.deleteFile(path.toString());
                                deletedBuggedRegions.getAndIncrement();
                            };
                        });
            } catch (IOException e) {
                e.printStackTrace();
            }
            loadingForm.progressBar.setMaximum(regionFileNames.toArray().length);
            MSC.logger.log(Level.INFO, "Deleted "+deletedBuggedRegions.get()+" bugged regions.");
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
