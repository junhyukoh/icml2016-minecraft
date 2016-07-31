package umich.ML.mcwrap.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import umich.ML.mcwrap.MCWrap;
import umich.ML.mcwrap.configuration.ConfigurationHandler;
import umich.ML.mcwrap.event.ShutdownHook;
import umich.ML.mcwrap.goal.GoalFSM;
import umich.ML.mcwrap.map.GoalLocationsAndSpawnpointsManager;
import umich.ML.mcwrap.task.ActionHandler;
import umich.ML.mcwrap.task.PlayerState;
import umich.ML.mcwrap.task.Task;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Tester
{
    private boolean doStartGame = true;

    private boolean initial = true;

    private final Scanner scanner = new Scanner(System.in);

    @SubscribeEvent
    @Mod.EventHandler
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unused")
    public void runTester(TickEvent.RenderTickEvent event)
    {
        GameSettings.Options.FRAMERATE_LIMIT.setValueMax(0);

        if(doStartGame)
        {
            WorldSettings worldSettings = new WorldSettings(0L, WorldSettings.GameType.SPECTATOR, false,
                    false, WorldType.FLAT);
            worldSettings.setWorldName("0;0;0");
            worldSettings.enableCommands();

            String worldDirName = new SimpleDateFormat("yyyy-MM-dd hh-mm-ss").format(new Date());

            ShutdownHook.logWorldDirName(worldDirName);

            Minecraft.getMinecraft().launchIntegratedServer(worldDirName, worldDirName, worldSettings);

            doStartGame = false;
        }

        if(primaryCheckConditions(event)) {
            if (initial) {

                initial = false;

                Initializer.doInitialization();
                MCWrap.player.sendChatMessage("/gamerule doDaylightCycle false");

                Task.reset(true);

                if(ConfigurationHandler.getActionTest())
                    System.out.println("Please enter actions below:");

                else if(!ConfigurationHandler.getActionTest())
                    System.out.println("Enjoy traversing the maps! Maps will be switched randomly every " +
                            ConfigurationHandler.getMapUpdateFreq() + " times.");
            }

            else if(ConfigurationHandler.getActionTest() && MCWrap.player.ticksExisted >= 100)
            {
                // Player has been updated. resetGame is required the first time this if statement is entered
                // for displaying goal blocks, not a problem with the socket based game play
                // (reset is called multiple times in the socket case).
                if(MCWrap.player.ticksExisted == 100) {
                    Task.reset(false);
                    MCWrap.player.ticksExisted++;
                }

                PlayerState.syncPlayer();

                for(int i = 0; i < ActionHandler.getNumActions(); i++)
                    System.out.print(i + ": " + ActionHandler.getDescription(i) + ",");
                int action = scanner.nextInt();
                ActionHandler.completeAction(action);
                System.out.println(PlayerState.getPosition());
                String result = Task.step(action);

                if(result.split(" ")[1].equals("1"))
                {
                    System.out.print("Terminated! Reward: " + result.split(" ")[0] + "\n");
                    Task.reset(false);
                }

                else if(!result.equals(GoalFSM.defaultRewardAndTerminate))
                    System.out.print("Non-default Reward: " + result.split(" ")[0] + "\n");

            }

            else if(ConfigurationHandler.getWorldTest() &&
                    MCWrap.player.ticksExisted % ConfigurationHandler.getMapUpdateFreq() == 0)
                Task.reset(false);
        }
    }

    private Boolean primaryCheckConditions(TickEvent event)
    {
        MCWrap.minecraft = Minecraft.getMinecraft();
        MCWrap.player = Minecraft.getMinecraft().thePlayer;
        MCWrap.world = Minecraft.getMinecraft().theWorld;

        return MCWrap.minecraft.currentScreen == null && MCWrap.world != null &&
                !MCWrap.minecraft.isGamePaused() && MCWrap.player != null &&
                event.phase != TickEvent.Phase.END;
    }
}
