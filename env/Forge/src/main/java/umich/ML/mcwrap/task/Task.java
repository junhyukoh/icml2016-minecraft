package umich.ML.mcwrap.task;

import umich.ML.mcwrap.configuration.ConfigurationHandler;
import umich.ML.mcwrap.goal.GoalFSM;
import umich.ML.mcwrap.logger.Logger;
import umich.ML.mcwrap.map.*;

import java.util.Random;

/**************************************************
 * Package: umich.ML.mcwrap.task
 * Class: Task
 * Timestamp: 4:16 PM 12/19/15
 * Authors: Valliappa Chockalingam, Junhyuk Oh
 **************************************************/

public class Task {
    private static Boolean initialized = false;

    private Task() {}

    private static String topology_id = null;
    private static String goalAndSpawnID = null;

    private static int indexInTrajectory = 0;
    private static int trajectoryCount = 0;

    public static void init(String RootTaskDir) {
        if(!initialized) {
            System.out.print("[[ Task (( init )) ]] : No singleton, " +
                    "creating an instance for the first time! Proceeding.\n");

            System.out.print("[[ Task (( init )) ]] : Checking for task file: " +
                    RootTaskDir + "/task.xml" + ".\n");
            ConfigurationHandler.setTerminateAfter(RootTaskDir + "/task.xml");
            System.out.print("[[ Task (( init )) ]] : Finished logging task file.\n");

            System.out.print("[[ Task (( init )) ]] : Initializing Action Handler with file: " +
                    RootTaskDir + "/actions.xml" + ".\n");
            ActionHandler.init(RootTaskDir + "/actions.xml");
            System.out.print("[[ Task (( init )) ]] : Finished Action Handler initialization.\n");

            System.out.print("[[ Task (( init )) ]] : Initializing Block Info. Manager with file: " +
                    RootTaskDir + "/blockTypeInfo.xml" + ".\n");
            BlockInfoManager.init(RootTaskDir + "/blockTypeInfo.xml");
            System.out.print("[[ Task (( init )) ]] : Finished Block Info. Manager initialization.\n");

            System.out.print("[[ Task (( init )) ]] : Initializing Topology Manager with directory: " +
                    RootTaskDir + "/maps" + ".\n");
            TopologyManager.init(RootTaskDir + "/maps");
            System.out.print("[[ Task (( init )) ]] : Finished Topology Manager initialization.\n");

            System.out.print("[[ Task (( init )) ]] : Initializing Goal Locations Manager with directory: " +
                    RootTaskDir + "/maps" + ".\n");
            GoalLocationsAndSpawnpointsManager.init(RootTaskDir + "/maps");
            System.out.print("[[ Task (( init )) ]] : Finished Goal Locations Manager initialization.\n");

            System.out.print("[[ Task (( init )) ]] : Initializing Goal FSM with file: " +
                    RootTaskDir + "/goalInfo.xml" + ".\n");
            GoalFSM.init(RootTaskDir + "/goalInfo.xml");
            System.out.print("[[ Task (( init )) ]] : Finished Goal FSM initialization.\n");

            initialized = true;
        }

        else System.out.print("[[ Task (( init )) ]] : Singleton exists, " +
                "skipping init.\n");
    }

    public static void reset(Boolean initial)
    {
        // MCWrap.player.sendChatMessage("/gamemode spectator");

        String topology_id_t = TopologyManager.getRandTopologyID(initial);
        String goalAndSpawnID_t = GoalLocationsAndSpawnpointsManager.getRandGoalAndSpawnID(topology_id_t);
        // System.out.println(goalAndSpawnID_t);

        reset(topology_id_t, goalAndSpawnID_t, (new Random().nextInt(4)));
    }

    public static void reset(String topology_id_t, String goalAndSpawnID_t, int yaw)
    {
        TopologyManager.updateTopology(topology_id, goalAndSpawnID, topology_id_t, goalAndSpawnID_t);

        topology_id = topology_id_t;
        goalAndSpawnID = goalAndSpawnID_t;

        PlayerState.logPosChange(GoalLocationsAndSpawnpointsManager.getSpawn(topology_id, goalAndSpawnID));
        PlayerState.logFlying(false);
        PlayerState.logPitchChange(0.0F);
        PlayerState.logYawChange(yaw * 90.0F);

        PlayerState.syncPlayer();

        GoalFSM.reset();

        // MCWrap.player.sendChatMessage("/gamemode creative");
        // MCWrap.player.setGameType(WorldSettings.GameType.CREATIVE);

        PlayerState.syncPlayer();

        indexInTrajectory = 0;

        trajectoryCount++;

        if(ConfigurationHandler.getLog())
            Logger.logTrajectory(topology_id);
    }

    public static String step(int action)
    {
        indexInTrajectory++;
        String rewardAndTerminate = GoalLocationsAndSpawnpointsManager.getRewardStringWithTopologyAndGoalSpawnID(topology_id, goalAndSpawnID, action);
        String possibleActions = ActionHandler.getPossibleActions();
        if(indexInTrajectory == ConfigurationHandler.getTerminateAfter())
            rewardAndTerminate = rewardAndTerminate.split(" ")[0] + " 1";

        if(ConfigurationHandler.getLog())
        {
            Logger.logReward(Float.parseFloat(rewardAndTerminate.split(" ")[0]));

            if (rewardAndTerminate.split(" ")[1].equals("1")) {
                Logger.logTermination();

                if (trajectoryCount % ConfigurationHandler.getLogFreq() == 0 && ConfigurationHandler.getLog()) {
                    Logger.reinit(TopologyManager.getTopologyIDs());
                    trajectoryCount = 0;
                }
            }
        }

        return rewardAndTerminate + possibleActions;
    }

    public static String getCurrTopologyID()
    {
        assert(topology_id != null);
        return topology_id;
    }

    public static String getCurrGoalAndSpawnID()
    {
        assert(goalAndSpawnID != null);
        return goalAndSpawnID;
    }
}
