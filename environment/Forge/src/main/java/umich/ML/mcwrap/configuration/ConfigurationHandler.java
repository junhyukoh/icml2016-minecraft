package umich.ML.mcwrap.configuration;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import umich.ML.mcwrap.util.FileParser;

import java.io.File;
import java.lang.reflect.Field;

/**************************************************
 * Package: umich.ML.mcwrap.configuration
 * Class: ConfigurationHandler
 * Timestamp: 3:52 PM 12/19/15
 * Authors: Valliappa Chockalingam, Junhyuk Oh
 **************************************************/

// NOTE: Singleton Class (zero or one instances are present at any given time)
public class ConfigurationHandler {
    private static Configuration configuration = null;

    private static int port;

    private static Boolean worldTest;

    private static Boolean actionTest;

    private static int screenHeight;

    private static int screenWidth;

    private static Boolean log;

    private static int terminateAfter;

    private static Boolean roofEnable;

    private static int mapUpdateFreq;

    private static int logFreq;

    private ConfigurationHandler() {}

    private static Boolean initialized = false;

    // REQUIRES: configFile to be set to the file containing the configuration
    // MODIFIES: configuration and all private static variables associated with this class
    // EFFECTS:  loads configuration file and hence the variables associated with it
    // NOTE: Incorrect file could lead to simply using defaults
    public static void init(File configFile)
    {
        // If the configuration instance is assigned, do nothing
        if(!initialized)
        {
            System.out.print("[[ Configuration Handler  (( init )) ]] : No singleton, " +
                    "creating an instance for the first time! Proceeding.\n");

            configuration = new Configuration(configFile);

            port = configuration.get("Network", "port", 0,
                    "Choose Port No. Random port is chosen by default (0).").getInt();

            worldTest = configuration.get("Tester", "worldTestEnable", false,
                    "Set to true for testing world.").getBoolean();

            actionTest = configuration.get("Tester", "actionTestEnable", false,
                    "Set to true for testing actions through loop-based play.").getBoolean();

            mapUpdateFreq = configuration.get("Tester", "mapUpdateFreq", 200,
                    "Set to frequency of changing maps (in number of ticks).").getInt();

            if(actionTest && worldTest) {
                System.out.print("World test and Action Test flags cannot both be true!");
                FMLCommonHandler.instance().exitJava(-1, false);
            }

            screenHeight = configuration.get("Screen Dimension", "height", 32, "Screen Height").getInt();

            screenWidth = configuration.get("Screen Dimension", "width", 32, "Screen Width").getInt();

            log = configuration.get("Debug", "log", true, "Log useful info.").getBoolean();

            logFreq = configuration.get("Debug", "logFreq", 1000, "Log frequency.").getInt();

            terminateAfter = configuration.get("Task", "terminateAfter", 100,
                    "Number of actions to terminate after.").getInt();

            roofEnable = configuration.get("Task", "roofEnable", true, "Enable Roof?").getBoolean();

            if(configuration.hasChanged())
            {
                System.out.print("[[ Configuration Handler (( init )) ]] : Configuration has changed. Saving.\n");
                configuration.save();
                System.out.print("[[ Configuration Handler (( init )) ]] : Completed save.\n");
            }

            initialized = true;
        }

        else System.out.print("[[ Configuration Handler (( init )) ]] : Singleton exists, " +
                "skipping init.\n");
    }

    /* Getters for configuration variables */

    public static int getPort() {
        return port;
    }

    public static Boolean getWorldTest() {
        return worldTest;
    }

    public static int getScreenHeight() {
        return screenHeight;
    }

    public static int getScreenWidth() {
        return screenWidth;
    }

    public static void setScreenRes(String screenRes)
    {
        screenHeight = Integer.parseInt(screenRes.replace("\n", "").split(" ")[1]);
        screenWidth = Integer.parseInt(screenRes.replace("\n", "").split(" ")[2]);
    }

    public static Boolean getActionTest() {
        return actionTest;
    }

    public static Boolean getLog() {
        return log;
    }

    public static int getTerminateAfter() {
        return terminateAfter;
    }

    public static void setTerminateAfter(String term_cond_file)
    {
        Element term_cond_root = FileParser.readXML(term_cond_file);

        NodeList nList = term_cond_root.getElementsByTagName("terminate");

        NamedNodeMap namedNodeMap = nList.item(0).getAttributes();

        terminateAfter = Integer.parseInt(namedNodeMap.getNamedItem("after").getNodeValue());
    }

    public static int getMapUpdateFreq() {
        return mapUpdateFreq;
    }

    public static Boolean getRoofEnable() {
        return roofEnable;
    }

    public static int getLogFreq() { return logFreq; }

    public static void printInfo()
    {
        if(!initialized) {
            System.out.print("[[ Configuration Handler (( printInfo )) ]] : No singleton exists, " +
                    "cannot proceed in printing info. Aborting!\n");
            FMLCommonHandler.instance().exitJava(-1, false);
        }

        else
        {
            System.out.print("\n\n\n");

            for(Field field : ConfigurationHandler.class.getDeclaredFields())
            {
                try {
                    System.out.print(field.getName() + " : " + field.get(ConfigurationHandler.class) + "\n");
                }

                catch(IllegalAccessException e)
                {
                    FMLCommonHandler.instance().exitJava(-1, false);
                }
            }

            System.out.print("\n\n\n");
        }
    }
}
