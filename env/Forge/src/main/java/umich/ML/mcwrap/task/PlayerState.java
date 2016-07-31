package umich.ML.mcwrap.task;

import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import umich.ML.mcwrap.MCWrap;

public class PlayerState
{
    static BlockPos pos = new BlockPos(0, 0, 0);

    static float yaw = 0.0F;
    static float pitch = 0.0F;

    static Boolean isFlying = false;

    public static void logYawChange(float yaw_)
    {
        yaw = yaw_ % 360.0F;
    }

    public static void logPitchChange(float pitch_)
    {
        pitch = pitch_ % 90.0F;
    }

    public static void logPosChange(BlockPos pos_)
    {
        pos = pos_;
    }

    public static void syncPlayer()
    {
        MCWrap.player.capabilities.isFlying = isFlying;

        MCWrap.player.motionX = 0;
        MCWrap.player.motionY = 0;
        MCWrap.player.motionZ = 0;

        while(Math.abs(MCWrap.player.lastTickPosX - (pos.getX() + 0.5)) > 0.01 ||
                Math.abs(MCWrap.player.lastTickPosY - pos.getY()) > 0.01 ||
                Math.abs(MCWrap.player.lastTickPosZ - (pos.getZ() + 0.5)) > 0.01 ||
                Math.abs(MCWrap.player.posX - (pos.getX() + 0.5)) > 0.01 ||
                Math.abs(MCWrap.player.posY - pos.getY()) > 0.01 ||
                Math.abs(MCWrap.player.posZ - (pos.getZ() + 0.5)) > 0.01 ||
                Math.abs(MCWrap.player.prevRotationYaw - yaw) > 0.01 ||
                Math.abs(MCWrap.player.prevRotationYawHead - yaw) > 0.01 ||
                Math.abs(MCWrap.player.prevRotationPitch - pitch) > 0.01 ||
                Math.abs(MCWrap.player.rotationYawHead - yaw) > 0.01 ||
                Math.abs(MCWrap.player.rotationYaw - yaw) > 0.01 ||
                Math.abs(MCWrap.player.rotationPitch - pitch) > 0.01)
        {
            MCWrap.player.setPositionAndUpdate(pos.getX() + 0.5,
                    pos.getY(), pos.getZ() + 0.5);
            MCWrap.player.rotationYaw = yaw;
            MCWrap.player.prevRotationYaw = yaw;
            MCWrap.player.rotationPitch = pitch;
            MCWrap.player.prevRotationPitch = pitch;
            MCWrap.player.rotationYawHead = yaw;
            MCWrap.player.prevRotationYawHead = yaw;

            MCWrap.player.onEntityUpdate();
            MCWrap.player.onLivingUpdate();
            MCWrap.player.onUpdate();
            MCWrap.player.onUpdateWalkingPlayer();
        }
    }

    public static void logFlying(Boolean flying)
    {
        isFlying = flying;
    }

    public static BlockPos getPosition()
    {
        return pos.down();
    }

    public static int facingDir(){
        return MathHelper.floor_double((double) (PlayerState.yaw * 4.0F / 360.0F) + 0.5D) & 3;
    }
}
