package de.btegermany.msc;

import net.buildtheearth.terraminusminus.generator.EarthGeneratorSettings;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;


public class Converter {
    private static final EarthGeneratorSettings bteGeneratorSettings = EarthGeneratorSettings.parse(EarthGeneratorSettings.BTE_DEFAULT_SETTINGS);

    // VA: r . 5621 . -8513 .mca
    // CC: 13133 . 0 . -19714 .3dr
    public static double[] regionFileToMcCoords(String fileName) {
        int regionX;
        int regionZ;
        if(fileName.startsWith("r.")) {
            String[] parts = fileName.split("\\.");
            regionX = Integer.parseInt(parts[1]);
            regionZ = Integer.parseInt(parts[2]);
        }else{
            String[] parts = fileName.split("\\.");
            regionX = Integer.parseInt(parts[0]);
            regionZ = Integer.parseInt(parts[2]);
        }

        int chunkX = regionX << 5;
        int chunkZ = regionZ << 5;
        double x = (chunkX << 4);
        double z = (chunkZ << 4);
        if(fileName.startsWith("r.")) {
            return new double[]{x, z};
        }else{
            return new double[]{x/2, z/2};
        }
    }

    public static String regionFile3dTo2d(String fileName) {

        String[] parts = fileName.split("\\.");
        double regionX = Double.parseDouble(parts[0]);
        double regionZ = Double.parseDouble(parts[2]);

        int roundedRegionX = (int) Math.floor(regionX / 2);
        int roundedRegionZ = (int) Math.floor(regionZ / 2);

        return roundedRegionX + "." + roundedRegionZ + ".2dr";
    }

    public static String mcCoordsToRegionFile(double x, double z, boolean isVanilla){
        int chunkX = (int) x >> 4;
        int chunkZ = (int) z >> 4;
        int regionX = chunkX >> 5;
        int regionZ = chunkZ >> 5;
        return isVanilla ? "r."+regionX+"."+regionZ+".mca" : regionX+"."+"<height>"+"."+regionZ+".3dr";
    }


    public static double[] fromGeo(double[] coordinates) throws OutOfProjectionBoundsException {
        return bteGeneratorSettings.projection().fromGeo(coordinates[0], coordinates[1]);
    }

    public static double[] toGeo(double[] mccoordinates) throws OutOfProjectionBoundsException {
        double[] coords = bteGeneratorSettings.projection().toGeo(mccoordinates[0], mccoordinates[1]);
        return new double[]{coords[1],coords[0]};
    }

}
