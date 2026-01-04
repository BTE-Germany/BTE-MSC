package de.btegermany.msc;

import de.btegermany.msc.exceptions.AnalyzerException;
import de.btegermany.msc.gui.*;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
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
    private boolean isVanilla;
    private JButton analyzeButton;
    private int regionFileSize = 0;
    private long speicherplatz = 0;

    private JComboBox worldTypeDropdown;
    private JComboBox analyzeCriteriaDropdown;
    private JTable foundLocationsTable;

    private HashMap<String, List<String>> locationCache = new HashMap<>();

    private JLabel totalRegionFileAmount;
    private JLabel totalSpace;

    private JButton runMSCButton;

    private JButton moreAnalyticsButton;

    private JCheckBox deleteBuggedRegionsCheckBox;

    private LocationListEntryTableModel currentTableModel;

    public Analyzer(File worldFolder, JButton analyzeButton, JFrame frame, JComboBox worldTypeDropdown, JComboBox analyzeCriteriaDropdown, JTable foundLocationsTable, JButton runMSCButton, JLabel totalRegionFileAmount, JLabel totalSpace, JButton moreAnalyticsButton, JCheckBox deleteBuggedRegionsCheckBox) {
        this.worldFolder = worldFolder;
        this.analyzeButton = analyzeButton;
        this.worldTypeDropdown = worldTypeDropdown;
        this.analyzeCriteriaDropdown = analyzeCriteriaDropdown;
        this.foundLocationsTable = foundLocationsTable;
        this.totalRegionFileAmount = totalRegionFileAmount;
        this.totalSpace = totalSpace;
        this.runMSCButton = runMSCButton;
        this.moreAnalyticsButton = moreAnalyticsButton;
        this.deleteBuggedRegionsCheckBox = deleteBuggedRegionsCheckBox;
        isVanilla = worldTypeDropdown.getSelectedItem().equals("Vanilla/Anvil");
        runMSCButton.setEnabled(true);
        runMSCButton.setText("Run MSC");

        // Set the current analyzed world folder for smart extract destination navigation
        ButtonEditor.setCurrentAnalyzedWorldFolder(worldFolder);

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
            for (ActionListener al : runMSCButton.getActionListeners()) {
                runMSCButton.removeActionListener(al);
            }
            MSC.logger.log(Level.INFO, "Started MSC");
            runMSCButton.setEnabled(false);
            runMSCButton.setText("Please Run Analyze before running MSC again!");
            runMSC();
        });
    }


    public void runMSC() {
        // Get the actual table model (not filtered view) to process ALL locations
        LocationListEntryTableModel tableModel = currentTableModel;
        if (tableModel == null) {
            MSC.logger.log(Level.WARNING, "Cannot run MSC: table model is null");
            JOptionPane.showMessageDialog(
                null,
                "Cannot run MSC: No analysis data available.\nPlease analyze a world first.",
                "No Data",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int totalLocations = tableModel.getTotalRowCount(); // All locations, not filtered
        MSC.logger.log(Level.INFO, "MSC will run with " + totalLocations + " total " + foundLocationsTable.getColumnName(0) + " (ignoring search filter).");

        for (int row = 0; row < totalLocations; row++) {
            String action = "Do nothing";
            // Get data directly from table model, not from filtered view
            LocationListEntry entry = tableModel.getEntryAt(row);
            if (entry == null) continue;

            Object actionObj = entry.getComboBox().getSelectedItem();
            if (actionObj != null) {
                action = actionObj.toString();
            }

            String toWorld = "";
            if (entry.getWorld() != null) {
                String worldText = entry.getWorld().getText();
                toWorld = worldText != null ? worldText : "";
            }
            String regionFolder;
            ArrayList<String> regionFiles2d = new ArrayList<>();
            if (isVanilla) {
                regionFolder = "/region";
            } else {
                regionFolder = "/region3d";
            }

            if (action.equals("Extract location files to")) {
                // Get location name from entry
                String locationName = entry.getLocation() != null ? entry.getLocation().getText() : null;
                if (locationName == null || locationName.isEmpty()) {
                    MSC.logger.log(Level.WARNING, "Skipping row " + row + ": Location name is empty");
                    continue;
                }

                // Validate that location exists in cache
                List<String> regionFiles = locationCache.get(locationName);
                if (regionFiles == null || regionFiles.isEmpty()) {
                    MSC.logger.log(Level.WARNING, "Skipping row " + row + ": No region files found for location " + locationName);
                    JOptionPane.showMessageDialog(
                        null,
                        "No region files found for location: " + locationName,
                        "Location Not Found",
                        JOptionPane.WARNING_MESSAGE
                    );
                    continue;
                }

                // Validate that a world was selected
                if (toWorld == null || toWorld.isEmpty() || toWorld.equals("Select world")) {
                    MSC.logger.log(Level.WARNING, "Skipping row " + row + ": No destination world selected for location " + locationName);
                    JOptionPane.showMessageDialog(
                        null,
                        "Please select a destination world for location: " + locationName,
                        "No Destination Selected",
                        JOptionPane.WARNING_MESSAGE
                    );
                    continue;
                }

                // Validate that destination world folder exists
                File destWorldFolder = new File(toWorld);
                if (!destWorldFolder.exists() || !destWorldFolder.isDirectory()) {
                    MSC.logger.log(Level.WARNING, "Skipping row " + row + ": Destination world does not exist: " + toWorld);
                    JOptionPane.showMessageDialog(
                        null,
                        "Destination world folder does not exist:\n" + toWorld,
                        "Invalid Destination",
                        JOptionPane.ERROR_MESSAGE
                    );
                    continue;
                }

                if (!new File(toWorld + regionFolder).exists() && !new File(toWorld + regionFolder).mkdir()) {
                    MSC.logger.log(Level.WARNING, "Skipping row " + row + ": Could not create region folder in destination world: " + toWorld);
                    JOptionPane.showMessageDialog(
                        null,
                        "Could not create region folder in destination world:\n" + toWorld,
                        "Invalid Destination",
                        JOptionPane.ERROR_MESSAGE
                    );
                    continue;
                }

                MSC.logger.log(Level.INFO, "Extracting files from " + locationName + " to " + toWorld);

                // Move Files to toWorld file
                for (String absoluteRegionFile : regionFiles) {
                    if (!isVanilla) {
                        String regionFile2dName = Converter.regionFile3dTo2d(absoluteRegionFile);
                        if (!regionFiles2d.contains(regionFile2dName)) {
                            regionFiles2d.add(regionFile2dName);
                        }
                    }
                    Utils.moveFile(worldFolder.getAbsolutePath() + regionFolder + "/" + absoluteRegionFile, toWorld + regionFolder);
                }
                // move 2d files if cc world
                if (!regionFiles2d.isEmpty()) {
                    for (String regionFile2dName : regionFiles2d) {
                        Utils.moveFile(worldFolder.getAbsolutePath() + "/region2d/" + regionFile2dName, toWorld + "/region2d");
                    }
                }
            } else if (action.equals("Delete location files")) {
                // Get location name from entry
                String locationName = entry.getLocation() != null ? entry.getLocation().getText() : null;
                if (locationName == null || locationName.isEmpty()) {
                    MSC.logger.log(Level.WARNING, "Skipping row " + row + ": Location name is empty");
                    continue;
                }

                // Validate that location exists in cache
                List<String> regionFiles = locationCache.get(locationName);
                if (regionFiles == null || regionFiles.isEmpty()) {
                    MSC.logger.log(Level.WARNING, "Skipping row " + row + ": No region files found for location " + locationName);
                    JOptionPane.showMessageDialog(
                        null,
                        "No region files found for location: " + locationName,
                        "Location Not Found",
                        JOptionPane.WARNING_MESSAGE
                    );
                    continue;
                }

                // Delete Files from original world
                for (String absoluteRegionFile : regionFiles) {
                    if (!isVanilla) {
                        String regionFile2dName = Converter.regionFile3dTo2d(absoluteRegionFile);
                        if (!regionFiles2d.contains(regionFile2dName)) {
                            regionFiles2d.add(regionFile2dName);
                        }
                    }
                    Utils.deleteFile(worldFolder.getAbsolutePath() + regionFolder + "/" + absoluteRegionFile);
                }
                // delete 2d files if cc world
                if (!regionFiles2d.isEmpty()) {
                    for (String regionFile2dName : regionFiles2d) {
                        Utils.deleteFile(worldFolder.getAbsolutePath() + "/region2d/" + regionFile2dName);
                    }
                }
            }

            // Log the operation if location exists in cache
            if (!action.equals("Do nothing")) {
                String locationName = entry.getLocation() != null ? entry.getLocation().getText() : null;
                if (locationName != null) {
                    List<String> regionFilesForLog = locationCache.get(locationName);

                    MSC.logger.log(Level.INFO, foundLocationsTable.getColumnName(0) + " " + locationName + " -> " + action + " " + toWorld);

                    if (regionFilesForLog != null && !regionFilesForLog.isEmpty()) {
                        MSC.logger.log(Level.INFO, "Manipulated Regionfiles: " + Arrays.toString(regionFilesForLog.toArray()).replace("[", "").replace("]", ""));
                    } else {
                        MSC.logger.log(Level.WARNING, "No region files found in cache for location: " + locationName);
                    }

                    if (!isVanilla && !regionFiles2d.isEmpty()) {
                        MSC.logger.log(Level.INFO, "Manipulated Regionfiles2d: " + Arrays.toString(regionFiles2d.toArray()).replace("[", "").replace("]", ""));
                    }
                }
            }
        }

        MSC.logger.log(Level.INFO, "Successfully ran MSC on " + totalLocations + " total locations.");
    }


    @Override
    protected Void doInBackground() {

        analyzeButton.setEnabled(false);
        analyzeButton.setText("Analyzing...");

        MSC.logger.log(Level.INFO, "Analyzing Files in " + worldTypeDropdown.getSelectedItem() + " Format.");

        analyze(analyzeCriteriaDropdown, foundLocationsTable);

        MSC.logger.log(Level.INFO, "Found " + regionFileSize + " Region Files.");
        totalRegionFileAmount.setText(regionFileSize + " Files");
        totalSpace.setText(Double.parseDouble(String.format(Locale.ENGLISH, "%1.2f", bytesToGB(speicherplatz))) + " GB");

        loadingForm.progressLabel.setText("Finished!");
        loadingForm.progressFinishedButton.setVisible(true);
        analyzeButton.setEnabled(true);
        analyzeButton.setText("Analyze");
        runMSCButton.setVisible(true);
        return null;
    }

    public void analyze(JComboBox analyzeCriteriaDropdown, JTable foundLocationsTable) {
        ArrayList<String> foundLocationsList = new ArrayList<>();
        ArrayList<String> outOfBoundsRegions = initialLoad(analyzeCriteriaDropdown, foundLocationsList);

        // Save foundLocationsList without duplicates, and filter out null/empty values
        ArrayList<String> foundLocationsListReduced = new ArrayList<>();
        for (String location : foundLocationsList) {
            if (location != null && !location.trim().isEmpty() && !foundLocationsListReduced.contains(location)) {
                foundLocationsListReduced.add(location);
            }
        }

        // List for the table with dataclass LocationListEntry
        ArrayList<LocationListEntry> foundLocationsListEntryList = new ArrayList<>();
        for (String location : foundLocationsListReduced) {
            JComboBox jComboBox = new JComboBox();
            jComboBox.addItem("Do nothing");
            jComboBox.addItem("Extract location files to");
            jComboBox.addItem("Delete location files");

            JButton selectWorldToMove = new JButton("Select world");
            selectWorldToMove.setEnabled(false);

            int amountFilesForLocation = countFilesForLocation(location, foundLocationsList);
            double percentage = (double) amountFilesForLocation / regionFileSize * 100;

            JLabel criteria = new JLabel();
            criteria.setName(analyzeCriteriaDropdown.getSelectedItem().toString());
            criteria.setText(location);

            foundLocationsListEntryList.add(new LocationListEntry(criteria, percentage, new JLabel(amountFilesForLocation + "x"), jComboBox, selectWorldToMove));

            MSC.logger.log(Level.INFO, +percentage + "% of the region files are in " + location + ".");
        }

        // Add "Out of Bounds" entry if there are any out-of-bounds regions
        if (!outOfBoundsRegions.isEmpty()) {
            this.locationCache.put("Out of Bounds", outOfBoundsRegions);

            JComboBox outOfBoundsComboBox = new JComboBox();
            outOfBoundsComboBox.addItem("Do nothing");
            outOfBoundsComboBox.addItem("Extract location files to");
            outOfBoundsComboBox.addItem("Delete location files");

            JButton outOfBoundsSelectWorld = new JButton("Select world");
            outOfBoundsSelectWorld.setEnabled(false);

            int outOfBoundsCount = outOfBoundsRegions.size();
            double outOfBoundsPercentage = (double) outOfBoundsCount / regionFileSize * 100;

            JLabel outOfBoundsCriteria = new JLabel();
            outOfBoundsCriteria.setName("Out of Bounds");
            outOfBoundsCriteria.setText("Out of Bounds");

            foundLocationsListEntryList.add(new LocationListEntry(outOfBoundsCriteria, outOfBoundsPercentage,
                new JLabel(outOfBoundsCount + "x"), outOfBoundsComboBox, outOfBoundsSelectWorld));

            MSC.logger.log(Level.INFO, outOfBoundsPercentage + "% of the region files are Out of Bounds (" + outOfBoundsCount + " files).");
        }

        setupTableModel(foundLocationsTable, foundLocationsListEntryList);

        // Setup More Analytics button
        if (moreAnalyticsButton != null) {
            moreAnalyticsButton.addActionListener(e -> {
                MoreAnalyticsForm moreAnalyticsForm = new MoreAnalyticsForm(
                    (JFrame) SwingUtilities.getWindowAncestor(moreAnalyticsButton),
                    foundLocationsListEntryList
                );
                moreAnalyticsForm.getMoreAnalyticsDialog().setVisible(true);
            });
        }

    }

    /**
     * Sets up the table model for foundLocationsTable
     */
    private void setupTableModel(JTable foundLocationsTable, ArrayList<LocationListEntry> foundLocationsListEntryList) {

        // Create the table model and insert data via parameter
        LocationListEntryTableModel locationListEntryTableModel = new LocationListEntryTableModel(foundLocationsListEntryList);
        this.currentTableModel = locationListEntryTableModel;
        foundLocationsTable.setModel(locationListEntryTableModel);

        // Sort the table
        TableRowSorter tableRowSorter = locationListEntryTableModel.getTableRowSorter(locationListEntryTableModel);
        foundLocationsTable.setRowSorter(tableRowSorter);

        // Table formatting
        DefaultTableCellRenderer rightAlignmentRenderer = new DefaultTableCellRenderer();
        rightAlignmentRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        DefaultTableCellRenderer centerAlignmentRenderer = new DefaultTableCellRenderer();
        centerAlignmentRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        CopyableTableCellRenderer copyableRenderer = new CopyableTableCellRenderer();
        copyableRenderer.setHorizontalAlignment(SwingConstants.LEFT);

        foundLocationsTable.getColumnModel().getColumn(0).setCellRenderer(copyableRenderer);
        foundLocationsTable.getColumnModel().getColumn(1).setCellRenderer(rightAlignmentRenderer);
        foundLocationsTable.getColumnModel().getColumn(1).setPreferredWidth(5);
        foundLocationsTable.getColumnModel().getColumn(2).setCellRenderer(rightAlignmentRenderer);
        foundLocationsTable.getColumnModel().getColumn(2).setPreferredWidth(5);
        foundLocationsTable.getColumnModel().getColumn(4).setCellRenderer(centerAlignmentRenderer);

        // Add right-click context menu for copying
        addTableContextMenu(foundLocationsTable);

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
        foundLocationsTable.getColumnModel().getColumn(4).setCellEditor(new ButtonEditor(selectWorldToMove));

    }

    /**
     * Gets location from region files and adds them to the foundLocationsList
     * @return List of out-of-bounds region file names
     */
    private ArrayList<String> initialLoad(JComboBox analyzeCriteriaDropdown, ArrayList<String> foundLocationsList) {
        // extract osm-location-data.bin before analyzing
        File osmLocationData = new File("osm-location-data.bin");
        if (!osmLocationData.exists()) {
            try (InputStream inputStream = MSC.class.getResourceAsStream("/osm-location-data.bin")) {
                try (FileOutputStream outputStream = new FileOutputStream(osmLocationData)) {
                    byte[] bytes = new byte[2048];
                    int read;
                    while ((read = inputStream.read(bytes)) != -1) {
                        outputStream.write(bytes, 0, read);
                    }
                }
            } catch (IOException e) {
                throw new AnalyzerException("osm-location-data.bin cannot be extracted. Please report this to the BTE Team.");
            }
        }

        // Track out-of-bounds regions separately
        ArrayList<String> outOfBoundsRegions = new ArrayList<>();

        for (String regionFileName : getRegionFileNames()) {
            double[] xy = Converter.regionFileToMcCoords(regionFileName);
            if ((xy[0] > 2000 || xy[0] < -2000) && (xy[1] > 2000 || xy[1] < -2000)) {
                regionFileSize++;
                double[] latLon = null;
                try {
                    latLon = Converter.toGeo(xy);
                } catch (OutOfProjectionBoundsException e) {
                    MSC.logger.log(Level.WARNING, regionFileName + " is out of bounds. Adding to Out of Bounds category...");
                    outOfBoundsRegions.add(regionFileName);
                    loadingForm.progressBar.setValue(loadingForm.progressBar.getValue() + 1);
                    continue;
                }

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

                if (this.locationCache.get(location) == null) {
                    List<String> tempList = new ArrayList<>();
                    tempList.add(regionFileName);
                    this.locationCache.put(location, tempList);
                } else {
                    List<String> tempList = this.locationCache.get(location);
                    tempList.add(regionFileName);
                    this.locationCache.put(location, tempList);
                }

                // Safely append to text area on EDT with length check
                final String logEntry = regionFileName + " | " + xy[0] + " " + xy[1] + " | " + latLon[0] + " " + latLon[1] + " | " + location + "\n";
                SwingUtilities.invokeLater(() -> {
                    try {
                        // Limit log size to prevent layout issues
                        if (loadingForm.progressLog != null && loadingForm.progressLog.getDocument().getLength() < 100000) {
                            loadingForm.progressLog.append(logEntry);
                            if (loadingForm.progressLogScrollPane != null) {
                                JScrollBar vertical = loadingForm.progressLogScrollPane.getVerticalScrollBar();
                                if (vertical != null) {
                                    vertical.setValue(vertical.getMaximum());
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Ignore text append errors
                    }
                });

                foundLocationsList.add(location);
                loadingForm.progressBar.setValue(foundLocationsList.size());

            }
        }

        return outOfBoundsRegions;
    }

    /*
        Counts the amount of files for a specific location
    */
    private int countFilesForLocation(String countlocation, ArrayList<String> foundLocationsList) {
        int count = 0;
        for (String location : foundLocationsList) {
            if (location == null) continue;
            if (location.equals(countlocation)) {
                count++;
            }
        }
        return count;
    }

    /*
        Scan the world folder for region files and return them as a list
     */
    private ArrayList<String> getRegionFileNames() {
        loadingForm.progressLabel.setText("Indexing region files... This process can take a bit.");
        ArrayList<String> regionFileNames = new ArrayList<>();
        if (isVanilla) {
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
        } else {
            AtomicInteger deletedBuggedRegions = new AtomicInteger();
            File regionFolder = new File(worldFolder.getAbsolutePath() + "/region3d");
            try (Stream<Path> paths = Files.list(regionFolder.toPath())) {

                paths.filter(path -> path.toString().endsWith(".3dr"))
                        .forEach(path -> {
                            // TODO: fix 100.000x the same 3dr file
                            String[] parts = path.getFileName().toString().split("\\.");
                            // skips all region files that are higher than 40*256 or lower than -40*256
                            int h = Integer.parseInt(parts[1]);
                            if (h > -40 && h < 40) {
                                regionFileNames.add(path.getFileName().toString());
                                speicherplatz += path.toFile().length();
                            } else if (deleteBuggedRegionsCheckBox.isSelected()) {
                                // Delete bugged regions
                                Utils.deleteFile(path.toString());
                                deletedBuggedRegions.getAndIncrement();
                            }
                        });
            } catch (IOException e) {
                e.printStackTrace();
            }
            loadingForm.progressBar.setMaximum(regionFileNames.toArray().length);
            MSC.logger.log(Level.INFO, "Deleted " + deletedBuggedRegions.get() + " bugged regions.");
        }
        return regionFileNames;
    }


    public static double bytesToGB(long bytes) {
        double kilobytes = bytes / 1024.0;
        double megabytes = kilobytes / 1024.0;
        double gigabytes = megabytes / 1024.0;
        return gigabytes;
    }

    private void addTableContextMenu(JTable table) {
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem copyItem = new JMenuItem("Copy");
        copyItem.addActionListener(e -> {
            int row = table.getSelectedRow();
            int column = table.getSelectedColumn();
            if (row >= 0 && column >= 0) {
                Object value = table.getValueAt(row, column);
                if (value != null) {
                    String text = value.toString();
                    StringSelection selection = new StringSelection(text);
                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                }
            }
        });
        contextMenu.add(copyItem);

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    int column = table.columnAtPoint(e.getPoint());
                    table.setRowSelectionInterval(row, row);
                    table.setColumnSelectionInterval(column, column);
                    contextMenu.show(table, e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    int column = table.columnAtPoint(e.getPoint());
                    table.setRowSelectionInterval(row, row);
                    table.setColumnSelectionInterval(column, column);
                    contextMenu.show(table, e.getX(), e.getY());
                }
            }
        });
    }

    public void applyTableFilter(String filterText, LocationListEntryTableModel model) {
        model.setFilterText(filterText);
    }

    public LocationListEntryTableModel getCurrentTableModel() {
        return currentTableModel;
    }


    class CheckBoxCellRenderer implements TableCellRenderer {
        JComboBox combo;

        public CheckBoxCellRenderer(JComboBox comboBox) {
            this.combo = new JComboBox();
            for (int i = 0; i < comboBox.getItemCount(); i++) {
                combo.addItem(comboBox.getItemAt(i));
            }
        }

        public Component getTableCellRendererComponent(JTable jtable, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            combo.setSelectedItem(value);
            return combo;
        }
    }
}
