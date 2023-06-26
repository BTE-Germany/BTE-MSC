package de.btegermany.msc.logger;

import de.btegermany.msc.MSC;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

public class Logger {

    private java.util.logging.Logger logger;

    public Logger() {
        this.logger = java.util.logging.Logger.getLogger(MSC.class.getName());

        logger.setLevel(Level.ALL);
        FileHandler fileHandler = null;
        try {
            fileHandler = new FileHandler("msc.log", true);
            fileHandler.setFormatter(new LoggerFormatter());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logger.addHandler(fileHandler);

        logger.log(Level.INFO,"\n"+
                "  __  __   ____     ____ \n" +
                " |  \\/  | / ___|   / ___|\n" +
                " | |\\/| | \\___ \\  | |    \n" +
                " | |  | |  ___) | | |___ \n" +
                " |_|  |_| |____/   \\____|\n" +
                "                         ");
        logger.log(Level.INFO,"by BTE Germany");
    }

    public void log(Level level, String msg) {
        logger.log(level,msg);
    }


}
