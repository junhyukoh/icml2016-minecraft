package umich.ML.mcwrap.map;

import net.minecraft.util.BlockPos;
import umich.ML.mcwrap.task.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**************************************************
 * Package: umich.ML.mcwrap.map
 * Class: TopologyInfo
 * Timestamp: 5:38 PM 12/20/15
 * Authors: Valliappa Chockalingam, Junhyuk Oh
 **************************************************/

class GoalLocationsSpawnpointsPair {
    private HashMap<BlockPos, Integer> goalLocations;
    private ArrayList<BlockPos> spawnpoints;

    public GoalLocationsSpawnpointsPair(HashMap<BlockPos, Integer> goalLocations_, ArrayList<BlockPos> spawnpoints_)
    {
        goalLocations = goalLocations_;
        spawnpoints = spawnpoints_;
    }

    public HashMap<BlockPos, Integer> getGoalLocations()
    {
        return goalLocations;
    }

    public BlockPos getRandSpawnPoint()
    {
        return spawnpoints.get(new Random().nextInt(spawnpoints.size()));
    }
}
