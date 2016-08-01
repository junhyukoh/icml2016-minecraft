package umich.ML.mcwrap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import umich.ML.mcwrap.communication.CommunicationHandler;
import umich.ML.mcwrap.configuration.ConfigurationHandler;
import umich.ML.mcwrap.event.RenderTickEventHandler;
import umich.ML.mcwrap.logger.Logger;
import umich.ML.mcwrap.task.Task;
import umich.ML.mcwrap.util.Reference;
import umich.ML.mcwrap.util.Tester;
import umich.ML.mcwrap.map.*;

import java.util.Scanner;

/**************************************************
 * Package: umich.ML.mcwrap
 * Class: MCWrap
 * Timestamp: 3:08 PM 12/19/15
 * Authors: Valliappa Chockalingam, Junhyuk Oh
 **************************************************/

// Mod Annotation used by Forge Mod Loader
@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.VERSION)

public class MCWrap {

    public static Minecraft minecraft = null;
    public static EntityPlayerSP player = null;
    public static World world = null;


    // MCWrap instance for use by other classes
    @SuppressWarnings("unused")
    @Mod.Instance(Reference.MOD_ID)
    public static MCWrap instance = new MCWrap();

    // Forge Mod Loader Pre-Initialization Event Handler
    // EFFECTS: Loads configuration file
    @SuppressWarnings("unused")
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        System.out.print("\n\n\n[[ MCWrap ((Forge Mod Loader Pre-Initializer)) ]] : Entering preInit()\n");

        System.out.print("[[ MCWrap ((Forge Mod Loader Pre-Initializer)) ]] : About to load configuration. Config. file: " +
                event.getSuggestedConfigurationFile() + "\n");
        ConfigurationHandler.init(event.getSuggestedConfigurationFile());
        System.out.print("[[ MCWrap (( Forge Mod Loader Pre-Initializer )) ]] : Completed loading configuration.\n");

        System.out.print("[[ MCWrap (( Forge Mod Loader Pre-Initializer )) ]] :  Configuration Info:\n");
        ConfigurationHandler.printInfo();

        System.out.print("[[ MCWrap (( Forge Mod Loader Pre-Initializer )) ]] : Exiting preInit()\n\n\n");
    }

    // Forge Mod Loader Initialization Event Handler
    @SuppressWarnings("unused")
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        System.out.print("\n\n\n[[ MCWrap (( Forge Mod Loader Initializer )) ]] : Entering init()\n");

        String taskStr;

        if (ConfigurationHandler.getWorldTest() || ConfigurationHandler.getActionTest()) {
            System.out.print("[[ MCWrap (( Forge Mod Loader Initializer )) ]] : " +
                    "Found world test or action test set to true!\n");

            System.out.print("[[ MCWrap (( Forge Mod Loader Initializer )) ]] : " +
                    "Please enter the data structure folder name: ");
            Scanner scan = new Scanner(System.in);
            taskStr = scan.next();

            System.out.print("[[ MCWrap (( Forge Mod Loader Initializer )) ]] : Parsed Task string: " + taskStr + "\n");
        }

        else {
            System.out.print("[[ MCWrap (( Forge Mod Loader Initializer )) ]] : " +
                    "Neither world test nor action test set to true. Initializing Communication Handler.\n");
            CommunicationHandler.init(ConfigurationHandler.getPort());
            System.out.print("[[ MCWrap (( Forge Mod Loader Initializer )) ]] : " +
                    "Finished initializing Communication handler.");

            System.out.print("[[ MCWrap (( Forge Mod Loader Initializer )) ]] : " +
                    "Getting task string through Comm. Handler.\n");
            taskStr = CommunicationHandler.getTask();
            System.out.print("[[ MCWrap (( Forge Mod Loader Initializer )) ]] : Parsed Task string: " + taskStr + "\n");
        }

        taskStr = "tasks/" + taskStr;
        System.out.print("[[ MCWrap (( Forge Mod Loader Initializer )) ]] : Initializing Task.\n");
        Task.init(taskStr);
        System.out.print("[[ MCWrap (( Forge Mod Loader Initializer )) ]] : Finished Initializing Task.\n");

        if(ConfigurationHandler.getWorldTest() || ConfigurationHandler.getActionTest())
            FMLCommonHandler.instance().bus().register(new Tester());
        else
            FMLCommonHandler.instance().bus().register(new RenderTickEventHandler());

        if(ConfigurationHandler.getLog())
            Logger.init(TopologyManager.getTopologyIDs());

        System.out.print("[[ MCWrap (( Forge Mod Loader Initializer )) ]] : Exiting init()\n\n\n");
    }

    // Forge Mod Loader Post-Initialization Event Handler
    @SuppressWarnings("unused")
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
    }
}
