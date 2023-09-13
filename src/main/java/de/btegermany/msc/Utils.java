package de.btegermany.msc;

import de.btegermany.msc.exceptions.AnalyzerException;
import de.btegermany.msc.geo.GeoLocation;
import de.btegermany.msc.geo.ReverseGeocoder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.logging.Level;

public class Utils {

    public static boolean moveFile(String sourceFilePath, String destinationFolderPath) {
        Path sourcePath = Path.of(sourceFilePath);
        Path destinationPath = Path.of(destinationFolderPath, sourcePath.getFileName().toString());
        System.out.println("Moving file " + sourceFilePath + " to " + destinationPath);

        // Versuchen, die Datei zu verschieben
        try {
            Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Error while trying to move the file " + sourceFilePath);
        }

        return true;
    }

    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);

        // Überprüfen, ob die Datei existiert
        if (file.exists()) {
            try {
                // Versuchen, die Datei zu löschen
                if (file.delete()) {
                    return true;
                } else {
                    System.err.println("Error while deleting " + filePath);
                    return false;
                }
            } catch (SecurityException e) {
                System.err.println("No permission to delete " + filePath);
                return false;
            }
        } else {
            System.err.println("The file does not exist: " + filePath);
            return false;
        }
    }

    public static Path getMinecraftDir(String mcFolderNanme) {
        String home = System.getProperty("user.home", ".");
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (os.contains("win") && System.getenv("APPDATA") != null) {
            return Paths.get(System.getenv("APPDATA"), "."+mcFolderNanme);
        } else if (os.contains("mac")) {
            return Paths.get(home, "Library", "Application Support", mcFolderNanme);
        }
        return Paths.get(home, "."+mcFolderNanme);
    }

    public static Path getMinecraftDir(String mcFolderNanme, String world) {
        String home = System.getProperty("user.home", ".");
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (os.contains("win") && System.getenv("APPDATA") != null) {
            return Paths.get(System.getenv("APPDATA"), "."+mcFolderNanme);
        } else if (os.contains("mac")) {
            return Paths.get(home, "Library", "Application Support", mcFolderNanme);
        }
        return Paths.get(home, "."+mcFolderNanme+"/"+world);
    }

    public static GeoLocation getOfflineLocation(double lat, double lon) {
        File osmLocationData = null;
        try {
            osmLocationData = new File(MSC.class.getResource("/osm-location-data.bin").toURI());
        } catch (URISyntaxException e) {
            throw new AnalyzerException("osm-location-data.bin file is missing");
        }
        try (ReverseGeocoder rgc = new ReverseGeocoder(osmLocationData.getAbsolutePath())) {
            GeoLocation location = new GeoLocation(lat, lon);
            StringBuilder cityBuilder = new StringBuilder();
            StringBuilder stateBuilder = new StringBuilder();
            StringBuilder countryBuilder = new StringBuilder();
            StringBuilder continentBuilder = new StringBuilder();
            for(String s : rgc.lookup((float) lon, (float) lat)) {
                if(s.endsWith("6")) {
                    for(char c : s.toCharArray()) {
                        if(!String.valueOf(c).matches("[ [^\\s+]]")) {
                            break;
                        }
                        cityBuilder.append(c);
                    }
                }
                if(s.endsWith("4")) {
                    for(char c : s.toCharArray()) {
                        if(!String.valueOf(c).matches("[ [^\\s+]]")) {
                            break;
                        }
                        stateBuilder.append(c);
                    }
                }
                if(s.endsWith("2")) {
                    for(char c : s.toCharArray()) {
                        if(!String.valueOf(c).matches("[ [^\\s+]]")) {
                            break;
                        }
                        countryBuilder.append(c);
                    }
                }
                if (s.endsWith("1")) {
                    for (char c : s.toCharArray()) {
                        if (!String.valueOf(c).matches("[ [^\\s+]]")) {
                            break;
                        }
                        continentBuilder.append(c);
                    }
                }
            }



            if(!cityBuilder.toString().isEmpty()){
                location.setCity(cityBuilder.toString());
            }
            if(!stateBuilder.toString().isEmpty()){
                location.setState(stateBuilder.toString());
            }
            if(!countryBuilder.toString().isEmpty()){
                location.setCountry(countryBuilder.toString());
            }
            if (!continentBuilder.toString().isEmpty()) {
                location.setContinent(continentBuilder.toString());
            }

            return location;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
