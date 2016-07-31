package umich.ML.mcwrap.task;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import umich.ML.mcwrap.map.GoalLocationsAndSpawnpointsManager;
import umich.ML.mcwrap.map.TopologyManager;
import umich.ML.mcwrap.util.FileParser;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import java.util.HashMap;

public class ActionHandler
{
    private static Boolean initialized = false;
    private static Boolean possibleActionsBool = false;

    private static final HashMap<Integer, Action> actions = new HashMap<Integer, Action>();
    private static Boolean sendPossibleActions = false;

    public static Integer pushAction = null;

    public static void init(String filePath)
    {
        if(initialized)
            return;
        else
            initialized = true;

        Element actionsRoot = FileParser.readXML(filePath);

        NodeList nList = actionsRoot.getElementsByTagName("action");

        for(int i = 0; i < nList.getLength(); i++)
        {
            NamedNodeMap namedNodeMap = nList.item(i).getAttributes();

            Integer id = Integer.parseInt(namedNodeMap.getNamedItem("id").getNodeValue());

            if(actions.containsKey(id))
            {
                System.out.print("Please check for duplicate action ids in actions.xml!");
                FMLCommonHandler.instance().exitJava(-1, false);
            }

            String action = namedNodeMap.getNamedItem("act").getNodeValue();

            final Integer diff;
            if(namedNodeMap.getNamedItem("diff") != null)
                diff = Integer.parseInt(namedNodeMap.getNamedItem("diff").getNodeValue());
            else
                diff = 0;

            if(action.equals("noop"))
                actions.put(id, new Action() {});

            else if(action.equals("push")) {
                actions.put(id, new Action() {
                });
                pushAction = id;
            }

            else if(action.equals("y"))
            {
                actions.put(id, new Action() {
                    @Override
                    @SuppressWarnings("all")
                    Boolean proposedChangeValid() {
                        if(diff > 0)
                            return PlayerState.pos.getY() + diff <= limit;
                        else if(diff < 0)
                            return PlayerState.pos.getY() + diff >= limit;
                        else return true;
                    }

                    @Override
                    void doAction() {
                        if(diff > 0)
                            PlayerState.logPosChange(PlayerState.pos.up(diff));
                        else if(diff < 0)
                            PlayerState.logPosChange(PlayerState.pos.down(Math.abs(diff)));
                        if(PlayerState.pos.getY() > 1)
                            PlayerState.logFlying(true);
                        else PlayerState.logFlying(false);
                    }
                });
            }

            else if(action.equals("yaw"))
            {
                actions.put(id, new Action() {
                    @Override
                    Boolean proposedChangeValid() {
                        return Math.abs(PlayerState.yaw + diff) <= Math.abs(limit);
                    }

                    @Override
                    public void doAction() {
                        PlayerState.logYawChange(PlayerState.yaw + diff);
                    }
                });
            }

            else if(action.equals("pitch"))
            {
                actions.put(id, new Action() {
                    @Override
                    Boolean proposedChangeValid() {
                        return Math.abs(PlayerState.pitch + diff) <= Math.abs(limit);
                    }

                    @Override
                    void doAction() {
                        PlayerState.logPitchChange(PlayerState.pitch + diff);
                    }
                });
            }

            else if(action.equals("forward"))
            {
                actions.put(id, new Action() {
                    @Override
                    Boolean proposedChangeValid() {
                        if(PlayerState.isFlying)
                            return false;

                        int dir = PlayerState.facingDir();

                        if(dir == 0)
                            return Minecraft.getMinecraft().theWorld.isAirBlock(PlayerState.pos.south());
                        if(dir == 1)
                            return Minecraft.getMinecraft().theWorld.isAirBlock(PlayerState.pos.west());
                        if(dir == 2)
                            return Minecraft.getMinecraft().theWorld.isAirBlock(PlayerState.pos.north());
                        if(dir == 3)
                            return Minecraft.getMinecraft().theWorld.isAirBlock(PlayerState.pos.east());
                        else System.out.print("Player in Non-cardinal direction!");

                        return false;
                    }

                    @Override
                    void doAction() {
                        int dir = PlayerState.facingDir();

                        if(dir == 0)
                            PlayerState.logPosChange(PlayerState.pos.south());
                        else if(dir == 1)
                            PlayerState.logPosChange(PlayerState.pos.west());
                        else if(dir == 2)
                            PlayerState.logPosChange(PlayerState.pos.north());
                        else if(dir == 3)
                            PlayerState.logPosChange(PlayerState.pos.east());
                        else System.out.print("Player in Non-cardinal direction!");
                    }
                });
            }

            else if(action.equals("backward"))
            {
                actions.put(id, new Action() {
                    @Override
                    Boolean proposedChangeValid() {
                        if(PlayerState.isFlying)
                            return false;

                        int dir = PlayerState.facingDir();

                        if(dir == 0)
                            return Minecraft.getMinecraft().theWorld.isAirBlock(PlayerState.pos.north());
                        if(dir == 1)
                            return Minecraft.getMinecraft().theWorld.isAirBlock(PlayerState.pos.east());
                        if(dir == 2)
                            return Minecraft.getMinecraft().theWorld.isAirBlock(PlayerState.pos.south());
                        if(dir == 3)
                            return Minecraft.getMinecraft().theWorld.isAirBlock(PlayerState.pos.west());
                        else System.out.print("Player in Non-cardinal direction!");

                        return false;
                    }

                    @Override
                    void doAction() {
                        int dir = PlayerState.facingDir();

                        if(dir == 0)
                            PlayerState.logPosChange(PlayerState.pos.north());
                        else if(dir == 1)
                            PlayerState.logPosChange(PlayerState.pos.east());
                        else if(dir == 2)
                            PlayerState.logPosChange(PlayerState.pos.south());
                        else if(dir == 3)
                            PlayerState.logPosChange(PlayerState.pos.west());
                        else System.out.print("Player in Non-cardinal direction!");
                    }
                });
            }

            else if(action.equals("break"))
            {
                actions.put(id, new Action() {

                    int counter = 0;

                    @Override
                    Boolean proposedChangeValid() {

                        if(counter >= limit)
                            return false;

                        BlockPos pos = Minecraft.getMinecraft().thePlayer.rayTrace(200, 1.0F).getBlockPos();
                        WorldClient theWorld = Minecraft.getMinecraft().theWorld;

                        return (!theWorld.isAirBlock(pos) && pos.getY() > 0 &&
                                pos.getX() > 0 &&
                                pos.getZ() > 0 &&
                                pos.getX() < TopologyManager.getMaxX(Task.getCurrTopologyID())
                                && pos.getZ() < TopologyManager.getMaxZ(Task.getCurrTopologyID()));
                    }

                    @Override
                    void doAction() {
                        BlockPos pos = Minecraft.getMinecraft().thePlayer.rayTrace(200, 1.0F).getBlockPos();
                        Minecraft.getMinecraft().theWorld.setBlockToAir(pos);
                        counter++;
                    }
                });
            }

            else
            {
                System.out.print("Unknown action found!\n");
                FMLCommonHandler.instance().exitJava(-1, false);
            }

            if(namedNodeMap.getNamedItem("lim") != null)
                actions.get(id).limit = Integer.parseInt(namedNodeMap.getNamedItem("lim").getNodeValue());

            actions.get(id).description = namedNodeMap.getNamedItem("desc").getNodeValue();
            actions.get(id).id = id;
        }
    }

    public static String getActionList()
    {
        String actionListStr = "";
        int i;
        for(i = 0; i < actions.size() - 1; i++)
            actionListStr = actionListStr + Integer.toString(i) + ",";
        actionListStr = actionListStr + i;
        return actionListStr;
    }

    public static int getNumActions()
    {
        return actions.size();
    }

    public static void completeAction(int action)
    {
        actions.get(action).handleAction();
    }

    public static String getDescription(int ID)
    {
        return actions.get(ID).description;
    }

    public static void shouldSendPossibleActions(Boolean send) {
        if(!possibleActionsBool)
        {
            possibleActionsBool = true;
            System.out.println("[[ Action Handler  (( shouldSendPossibleActions )) ]] : " +
                    "Possible actions not previously set. Proceeding. " +
                    "Recording actions to be sent? " + send + ".\n");
            sendPossibleActions = send;
        }

        else System.out.println("[[ Action Handler  (( shouldSendPossibleActions )) ]] : " +
                "Possible actions previously set. Ignoring request.\n");
    }

    public static String getPossibleActions()
    {
        if(!sendPossibleActions) return "";

        String actionListStr = " ";
        int i;
        for(i = 0; i < actions.size() - 1; i++)
            actionListStr = actionListStr + (actions.get(i).proposedChangeValid() ? "1" : "0") + ",";
        actionListStr = actionListStr + (actions.get(i).proposedChangeValid() ? "1" : "0");
        return actionListStr;
    }
}
