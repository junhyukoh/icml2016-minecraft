package umich.ML.mcwrap.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import umich.ML.mcwrap.MCWrap;
import umich.ML.mcwrap.communication.CommunicationHandler;
import umich.ML.mcwrap.configuration.ConfigurationHandler;
import umich.ML.mcwrap.map.TopologyManager;
import umich.ML.mcwrap.task.ActionHandler;
import umich.ML.mcwrap.task.PlayerState;
import umich.ML.mcwrap.task.Task;
import umich.ML.mcwrap.util.Initializer;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

/**************************************************
 * Package: umich.ML.mcwrap.event
 * Class: RenderTickEventHandler
 * Timestamp: 2:44 PM 12/20/15
 * Authors: Valliappa Chockalingam, Junhyuk Oh
 **************************************************/

public class RenderTickEventHandler {

    private Boolean doStartGame = true;

    private static Boolean initial = true;

    /* A buffer to hold pixel values returned by OpenGL. */
    private static ByteBuffer pixelBuffer;
    /* The built-up array that contains all the pixel values returned by OpenGL. */
    private static byte[] pixelValues;

    private int width; private int height;

    @SuppressWarnings("unused")
    public RenderTickEventHandler() {}

    @Mod.EventHandler
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unused")
    public void OnRenderTickEvent(TickEvent.RenderTickEvent event)
    {
        GameSettings.Options.FRAMERATE_LIMIT.setValueMax(0);

        if(doStartGame)
        {
            WorldSettings worldSettings = new WorldSettings(0L, WorldSettings.GameType.SPECTATOR, false,
                    false, WorldType.FLAT);
            worldSettings.setWorldName("0;0;0");
            worldSettings.enableCommands();

            String worldDirName = new SimpleDateFormat("yyyy-MM-dd hh-mm-ss").format(new Date());

            Minecraft.getMinecraft().launchIntegratedServer(worldDirName, worldDirName, worldSettings);

            ShutdownHook.logWorldDirName(worldDirName);

            doStartGame = false;
        }

        if(primaryCheckConditions(event))
        {
            if(initial)
            {
                initial = false;

                Initializer.doInitialization();
                MCWrap.player.sendChatMessage("/gamerule doDaylightCycle false");

                Task.reset(true);

                CommunicationHandler.handshake(MCWrap.minecraft.displayHeight, MCWrap.minecraft.displayWidth, 3);
            }

            else if (MCWrap.player.ticksExisted > 100) {
                PlayerState.syncPlayer();
  
                while (true) {
                    String message = CommunicationHandler.receive();
                    if (message == null) {
                        System.out.println("Socket is closed");
                        FMLCommonHandler.instance().exitJava(-1, false);
                    }
                    // if (message != null) {
                        if (message.startsWith("act ")) {
                            int action = Integer.parseInt(message.substring("act ".length()));
                            ActionHandler.completeAction(action);
                            CommunicationHandler.send(Task.step(action) + "\n");
                            break;
                        } else if (message.contains("pos")) {
                            CommunicationHandler.send(PlayerState.getPosition().getX() + " " +
                                    PlayerState.getPosition().getZ() + " " + Integer.toString(PlayerState.facingDir()) + "\n");
                        } else if (message.contains("reset_game")) {
                            if (message.split(" ").length > 1) {
                                assert (message.split(" ").length == 4);
                                String topology_str = message.split(" ")[1];
                                String goal_ID_str = message.split(" ")[2];
                                int yaw = Integer.parseInt(message.split(" ")[3].replace("\n", ""));

                                Task.reset(topology_str, goal_ID_str, yaw);
                            } else
                                Task.reset(false);

                            CommunicationHandler.send("reset\n");
                            break;
                        } else if (message.startsWith("get_height"))
                            CommunicationHandler.send(Integer.toString(MCWrap.minecraft.displayHeight) + "\n");

                        else if (message.startsWith("get_width"))
                            CommunicationHandler.send(Integer.toString(MCWrap.minecraft.displayWidth) + "\n");

                        else if (message.contains("set_screen ")) {
                            ConfigurationHandler.setScreenRes(message.replace("set_screen ", ""));
                            Initializer.doInitialization();
                            CommunicationHandler.send("ack\n");
                            break;
                        } else if (message.contains("get_screen")) {
                            CommunicationHandler.send(screenCapture());
                            break;
                        }

                        else if (message.contains("get_actions")) {
                            CommunicationHandler.send(ActionHandler.getActionList() + "\n");
                            break;
                        }

                        else if (message.contains("num_actions")) {
                            CommunicationHandler.send(Integer.toString(ActionHandler.getNumActions()) + "\n");
                            break;
                        }

                        else if (message.contains("get_topology"))
                            CommunicationHandler.send(Task.getCurrTopologyID() + "\n");

                        else if (message.contains("get_goal_id"))
                            CommunicationHandler.send(Task.getCurrGoalAndSpawnID() + "\n");

                        else if (message.contains("action_desc")) {
                            CommunicationHandler.send(ActionHandler.
                                    getDescription(Integer.parseInt(message.split(" ")[1].replace("\n", ""))) + "\n");
                            break;
                        }

                        else if (message.contains("ack_trial_ctr_inc"))
                            CommunicationHandler.send(TopologyManager.topologySequenceCompleted() + "\n");
                    // }
                }
            }
        }
    }

    private Boolean primaryCheckConditions(TickEvent event)
    {
        if(initial) {
            MCWrap.minecraft = Minecraft.getMinecraft();
            MCWrap.player = Minecraft.getMinecraft().thePlayer;
            MCWrap.world = Minecraft.getMinecraft().theWorld;
        }

        return MCWrap.minecraft.currentScreen == null && MCWrap.minecraft.theWorld != null &&
                !MCWrap.minecraft.isGamePaused() && MCWrap.player != null &&
                event.phase != TickEvent.Phase.END;
    }

    private byte[] screenCapture()
    {
        try
        {
            Framebuffer buffer = MCWrap.minecraft.getFramebuffer();

            if (OpenGlHelper.isFramebufferEnabled())
            {
                width = buffer.framebufferTextureWidth;
                height = buffer.framebufferTextureHeight;
            }

            int k = width * height * 3;

            if (pixelBuffer == null || pixelBuffer.capacity() < k)
            {
                pixelBuffer = BufferUtils.createByteBuffer(k);
                pixelValues = new byte[k];
            }

            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
            pixelBuffer.clear();

            if (OpenGlHelper.isFramebufferEnabled())
            {
                GlStateManager.bindTexture(buffer.framebufferTexture);
                GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, GL11.GL_BYTE, pixelBuffer);
            }
            else
            {
                GL11.glReadPixels(0, 0, width, height, GL11.GL_RGB, GL11.GL_BYTE, pixelBuffer);
            }

            pixelBuffer.get(pixelValues);
        }

        catch (Exception e)
        {
            System.out.println("Caught Exception on capturing screenshot. Stack trace:");
            e.printStackTrace();
            System.out.println("Now aborting!");
            FMLCommonHandler.instance().exitJava(-1, false);
        }

        return pixelValues;
    }

    @SuppressWarnings("unused")
    public static void updateDisplay()
    {
        if(initial)
            Minecraft.getMinecraft().updateDisplay();
    }
}
