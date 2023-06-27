package de.btegermany.msc;

import de.btegermany.msc.exceptions.AnalyzerException;
import de.btegermany.msc.gui.*;
import net.buildtheearth.terraminusminus.generator.EarthGeneratorSettings;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class Analyzer {

    private File worldFolder;
    private LoadingForm loadingForm;
    private boolean isVanilla;

    private static final EarthGeneratorSettings bteGeneratorSettings = EarthGeneratorSettings.parse(EarthGeneratorSettings.BTE_DEFAULT_SETTINGS);
    private JButton analyzeButton;
    private JFrame frame;
    private int regionFileSize = 0;

    public Analyzer(File worldFolder,JButton analyzeButton,JFrame frame, JComboBox worldTypeDropdown,JComboBox analyzeCriteriaDropdown,JTable foundLocations){
        this.worldFolder = worldFolder;
        this.analyzeButton = analyzeButton;
        this.frame = frame;

        loadingForm = new LoadingForm(frame);
        loadingForm.progressBar.setValue(50);

        analyzeButton.setEnabled(false);
        analyzeButton.setText("Analyzing...");


        isVanilla = worldTypeDropdown.getSelectedItem().equals("Vanilla/Anvil");


        MSC.logger.log(Level.INFO,"Analyzing Files in "+worldTypeDropdown.getSelectedItem()+" Format.");

        ArrayList<String> foundLocationsList = new ArrayList<>();

        for(String regionFileName : getRegionFileNames()){
            double[] xy = Converter.regionFileToMcCoords(regionFileName);
            if((xy[0] > 2000 || xy[0] < -2000) && (xy[1] > 2000 || xy[1] < -2000)) {
                regionFileSize++;
                double[] latLon;
                try {
                    latLon = Converter.toGeo(xy);
                } catch (OutOfProjectionBoundsException e) {
                    throw new AnalyzerException("Out of Projection Bounds: "+ xy[0]+","+xy[1] +" This should not happen. Please report this to the BTE Team.");
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

                foundLocationsList.add(location);
            }
        }

        ArrayList<String> foundLocationsListReduced = new ArrayList<>();
        for(String location : foundLocationsList){
            if(!foundLocationsListReduced.contains(location)) {
                foundLocationsListReduced.add(location);
            }
        }

        ArrayList<LocationListEntry> foundLocationsListEntryList = new ArrayList<>();
        for(String location : foundLocationsListReduced){
            JComboBox jComboBox = new JComboBox();
            jComboBox.addItem("Do nothing");

            JButton jButton = new JButton("Select world");
            jButton.setEnabled(false);
            int amountFilesForLocation = countFilesForLocation(location,foundLocationsList);
            double percentage = (double) amountFilesForLocation / regionFileSize * 100;
            System.out.println(amountFilesForLocation + " " + regionFileSize + " " + percentage);
            JLabel criteria = new JLabel();
            criteria.setName(analyzeCriteriaDropdown.getSelectedItem().toString());
            criteria.setText(location);
            foundLocationsListEntryList.add(new LocationListEntry(criteria,percentage,new JLabel(amountFilesForLocation + "x"),jComboBox,jButton));
            MSC.logger.log(Level.INFO,+percentage+"% of the region files are in " + location+".");
        }

        LocationListEntryTableModel locationListEntryTableModel = new LocationListEntryTableModel(foundLocationsListEntryList);

        // Sort the table
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
        foundLocations.setModel(locationListEntryTableModel);
        foundLocations.setRowSorter(tableRowSorter);

        // Align the numbers to the right
        DefaultTableCellRenderer rightAlignmentRenderer = new DefaultTableCellRenderer();
        rightAlignmentRenderer.setHorizontalAlignment(SwingConstants.RIGHT);

        //foundLocations.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        JComboBox comboBox = new JComboBox();
        comboBox.addItem("Do nothing");
        comboBox.addItem("Extract location files to");
        comboBox.addItem("Delete location files from world");

        foundLocations.getColumnModel().getColumn(1).setCellRenderer(rightAlignmentRenderer);
        foundLocations.getColumnModel().getColumn(1).setPreferredWidth(15);
        foundLocations.getColumnModel().getColumn(2).setCellRenderer(rightAlignmentRenderer);
        foundLocations.getColumnModel().getColumn(2).setPreferredWidth(25);
        foundLocations.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(comboBox));
        foundLocations.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer());
        foundLocations.getColumnModel().getColumn(4).setCellRenderer(new ButtonRenderer());
        foundLocations.getColumnModel().getColumn(4).setCellEditor(new ButtonEditor());

        MSC.logger.log(Level.INFO,"Found "+regionFileSize+" Region Files.");

        analyzeButton.setEnabled(true);
        analyzeButton.setText("Analyze");
    }

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
                }
            }

        }
        return regionFileNames;
    }






}
