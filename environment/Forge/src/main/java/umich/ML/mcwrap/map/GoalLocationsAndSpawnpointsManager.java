package umich.ML.mcwrap.map;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.commons.io.FilenameUtils;
import umich.ML.mcwrap.goal.GoalFSM;
import umich.ML.mcwrap.task.PlayerState;
import umich.ML.mcwrap.task.Task;
import umich.ML.mcwrap.util.FileParser;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**************************************************
 * Package: umich.ML.mcwrap.map
 * Class: GoalLocationsManager
 * Timestamp: 7:38 PM 12/19/15
 * Authors: Valliappa Chockalingam, Junhyuk Oh
 **************************************************/

public class GoalLocationsAndSpawnpointsManager {
    private static final HashMap<String, HashMap<String, GoalLocationsSpawnpointsPair>>
            goalLocationsAndSpawnpointsWithTopologyID =
                new HashMap<String, HashMap<String, GoalLocationsSpawnpointsPair>>();

    public static void init(String mapsDirStr)
    {
        File mapsDir = new File(mapsDirStr);

        File[] filesInMapsDir = mapsDir.listFiles();

        if(filesInMapsDir == null)
        {
            System.out.print("Maps folder is null! Aborting!");
            FMLCommonHandler.instance().exitJava(-1, false);
            return;
        }

        for(File f : filesInMapsDir)
        {
            goalLocationsAndSpawnpointsWithTopologyID.put(f.getName(),
                    new HashMap<String, GoalLocationsSpawnpointsPair>());
            if(f.isDirectory())
            {
                for(String name : f.list()) {
                    if(name.contains("goal")) {
                        String id = FilenameUtils.removeExtension(name.replace("goal_", ""));
                        HashMap<BlockPos, Integer> blockLocationListWithID = logGoalMap(f.getPath() + "/" + name);

                        ArrayList<BlockPos> spawnpointsListWithID =
                                logSpawnPoints((f.getPath() + "/" + name).replace("goal_", "spawn_"));

                        goalLocationsAndSpawnpointsWithTopologyID.get(f.getName()).put(id,
                                new GoalLocationsSpawnpointsPair(blockLocationListWithID, spawnpointsListWithID));
                    }
                }
            }
        }
    }

    private static HashMap<BlockPos, Integer> logGoalMap(String filePath)
    {
        String[][] goalMap = FileParser.readCSV(filePath);
        HashMap<BlockPos, Integer> blockPosIntegerHashMap = new HashMap<BlockPos, Integer>();

        try {
            for (int i = 0; i < goalMap.length; i++)
                for (int j = 0; j < goalMap[i].length; j++)
                    if (!goalMap[i][j].equals(" ") && !goalMap[i][j].equals("0"))
                        blockPosIntegerHashMap.put(new BlockPos(i, 0, j), Integer.parseInt(goalMap[i][j]));
        } catch (NumberFormatException e) {
            System.out.print("Number format exception during goal map reading!");
            FMLCommonHandler.instance().exitJava(-1, false);
        }

        return blockPosIntegerHashMap;
    }

    private static ArrayList<BlockPos> logSpawnPoints(String filePath)
    {
//        System.out.print("Load spawn: " + filePath + "\n");
        String[][] spawnPointsArr = FileParser.readCSV(filePath);

        ArrayList<BlockPos> spawnpoints = new ArrayList<BlockPos>();

        for(int i = 0; i < spawnPointsArr.length; i++)
            for(int j = 0; j < spawnPointsArr[i].length; j++)
                if(spawnPointsArr[i][j].equals("1"))
                    spawnpoints.add(new BlockPos(i, 1, j));

        if(spawnpoints.size() == 0)
        {
            System.out.print("No spawnpoints found!");
            FMLCommonHandler.instance().exitJava(-1, false);
        }

        return spawnpoints;
    }

    public static String getRandGoalAndSpawnID(String topology_id)
    {
        return new ArrayList<String>(goalLocationsAndSpawnpointsWithTopologyID.get(topology_id).keySet()).
                get(new Random().nextInt(goalLocationsAndSpawnpointsWithTopologyID.get(topology_id).keySet().size()));
    }

    public static HashMap<BlockPos, IBlockState> getGoalLocationsListWithBlockTypeInfo(String topology_id,
                                                                                       String goalAndSpawn_id)
    {
        HashMap<BlockPos, Integer> goalLocationsToBlockTypeID =
                goalLocationsAndSpawnpointsWithTopologyID.get(topology_id).get(goalAndSpawn_id).getGoalLocations();

        HashMap<BlockPos, IBlockState> blockStateWithGoalPos = new HashMap<BlockPos, IBlockState>();

        for(BlockPos pos : goalLocationsToBlockTypeID.keySet())
        {
            blockStateWithGoalPos.put(pos, BlockInfoManager.blockTypeWithGoalID(goalLocationsToBlockTypeID.get(pos)));
        }

        return blockStateWithGoalPos;
    }

    public static BlockPos getSpawn(String topologyID, String goalAndSpawnID)
    {
        return goalLocationsAndSpawnpointsWithTopologyID.get(topologyID).get(goalAndSpawnID).getRandSpawnPoint();
    }

    public static String getRewardStringWithTopologyAndGoalSpawnID(String topology_id, String goalAndSpawnID, int action)
    {
        if(goalLocationsAndSpawnpointsWithTopologyID.get(topology_id).get(goalAndSpawnID).getGoalLocations().containsKey(PlayerState.getPosition()))
            return GoalFSM.step(goalLocationsAndSpawnpointsWithTopologyID.get(topology_id).get(goalAndSpawnID).getGoalLocations().get(PlayerState.getPosition()), action);
        else return GoalFSM.step(goalLocationsAndSpawnpointsWithTopologyID.get(topology_id).get(goalAndSpawnID).getGoalLocations().get(null), action);
    }
}
