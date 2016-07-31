package umich.ML.mcwrap.util;


import net.minecraftforge.fml.common.FMLCommonHandler;
import umich.ML.mcwrap.MCWrap;
import umich.ML.mcwrap.configuration.ConfigurationHandler;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.EnumDifficulty;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

@SuppressWarnings("unused")
public class Initializer
{
    public static void doInitialization()
    {
        MCWrap.minecraft.gameSettings.chatVisibility = EntityPlayer.EnumChatVisibility.HIDDEN;
        MCWrap.minecraft.gameSettings.renderDistanceChunks = 16;
        MCWrap.minecraft.gameSettings.fancyGraphics = false;
        MCWrap.minecraft.gameSettings.smoothCamera = false;
        MCWrap.minecraft.gameSettings.showInventoryAchievementHint = false;
        MCWrap.minecraft.gameSettings.clouds = false;
        MCWrap.minecraft.gameSettings.difficulty = EnumDifficulty.PEACEFUL;
        MCWrap.minecraft.gameSettings.hideGUI = true;
        MCWrap.minecraft.gameSettings.limitFramerate = 0;
        MCWrap.minecraft.gameSettings.pauseOnLostFocus = false;
        MCWrap.minecraft.gameSettings.particleSetting = 2;
        MCWrap.minecraft.gameSettings.allowBlockAlternatives = false;
        MCWrap.minecraft.gameSettings.advancedItemTooltips = false;
        MCWrap.minecraft.gameSettings.snooperEnabled = false;
        MCWrap.minecraft.inGameHasFocus = false;
        MCWrap.minecraft.gameSettings.viewBobbing = false;
        MCWrap.minecraft.gameSettings.useVbo = true;
        MCWrap.minecraft.gameSettings.fboEnable = true;
        MCWrap.minecraft.gameSettings.setSoundLevel(SoundCategory.MASTER, 0.0F);
        MinecraftServer.getServer().theProfiler.profilingEnabled = false;
        MCWrap.minecraft.mcProfiler.profilingEnabled = false;

        try {
            Display.setDisplayMode(new DisplayMode(ConfigurationHandler.getScreenWidth(),
                   ConfigurationHandler.getScreenHeight()));
        }

        catch(LWJGLException e) {
            System.out.print("[[ Initializer (( doInitialization )) ]] : Error on resizing window!\n");
            FMLCommonHandler.instance().exitJava(-1, false);
        }

        MCWrap.minecraft.displayHeight = ConfigurationHandler.getScreenHeight();
        MCWrap.minecraft.displayWidth = ConfigurationHandler.getScreenWidth();

        MCWrap.minecraft.gameSettings.overrideHeight = ConfigurationHandler.getScreenHeight();
        MCWrap.minecraft.gameSettings.overrideWidth = ConfigurationHandler.getScreenWidth();

        MCWrap.minecraft.resize(ConfigurationHandler.getScreenHeight(), ConfigurationHandler.getScreenWidth());

        MCWrap.minecraft.gameSettings.saveOptions();
        MCWrap.minecraft.gameSettings.sendSettingsToServer();
        MCWrap.minecraft.setIngameNotInFocus();
    }
}
