package umich.ML.mcwrap.map;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import umich.ML.mcwrap.MCWrap;
import umich.ML.mcwrap.configuration.ConfigurationHandler;
import umich.ML.mcwrap.task.Task;
import umich.ML.mcwrap.util.FileParser;

import java.io.File;
import java.util.*;

/**************************************************
 * Package: umich.ML.mcwrap.map
 * Class: TopologyManager
 * Timestamp: 5:17 PM 12/19/15
 * Authors: Valliappa Chockalingam, Junhyuk Oh
 **************************************************/

@SuppressWarnings("unused")
public class TopologyManager {

    private static HashMap<String, ArrayList<BlockPos>> topologyBlocks = new HashMap<String, ArrayList<BlockPos>>();

    private static HashMap<String, BlockPos> maxIndexBlockInTopology = new HashMap<String, BlockPos>();

    private static ArrayList<String> topologiesPermutation = null;

    private static int topology_counter = 0;

    public static String topologySequenceCompleted()
    {
        return (topology_counter == topologyBlocks.size()) ? "1" : "0";
    }

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
            if(f.isDirectory())
            {
                ArrayList<BlockPos> blocksInTopology = logTopology(f.getPath() + "/topology.csv");
                topologyBlocks.put(f.getName(), blocksInTopology);
            }
        }
    }

    public static int getMaxX(String topologyID)
    {
        return maxIndexBlockInTopology.get(topologyID).getX();
    }

    public static int getMaxY(String topologyID)
    {
        return maxIndexBlockInTopology.get(topologyID).getY();
    }

    public static int getMaxZ(String topologyID)
    {
        return maxIndexBlockInTopology.get(topologyID).getZ();
    }

    private static ArrayList<BlockPos> logTopology(String filePath)
    {
        String[][] TopologyGrid = FileParser.readCSV(filePath);

        ArrayList<BlockPos> blocksInTopology = new ArrayList<BlockPos>();

        int maxX = 0;
        int maxY = 0;
        int maxZ = 0;

        try
        {
            for (int i = 0; i < TopologyGrid.length; i++)
                for (int j = 0; j < TopologyGrid[i].length; j++)
                    for (int k = 0; k < Integer.parseInt(TopologyGrid[i][j]); k++) {
                        blocksInTopology.add(new BlockPos(i, k, j));
                        if(Integer.parseInt(TopologyGrid[i][j]) > maxY)
                            maxY = Integer.parseInt(TopologyGrid[i][j]);
                    }

            if(ConfigurationHandler.getRoofEnable())
                for (int i = 0; i < TopologyGrid.length; i++)
                    for (int j = 0; j < TopologyGrid[i].length; j++) {
                        blocksInTopology.add(new BlockPos(i, maxY, j));
                        if (maxX < i) maxX = i;
                        if (maxZ < j) maxZ = j;
                    }

            maxIndexBlockInTopology.put(filePath.split("/")[filePath.split("/").length - 2], new BlockPos(maxX, maxY, maxZ));
        }

        catch(NumberFormatException e)
        {
            System.out.print("Number format exception on parsing Topology!");
            System.out.print("Note: File path for topology is " + filePath + ". Aborting!");
            FMLCommonHandler.instance().exitJava(-1, false);
        }

        return blocksInTopology;
    }

    public static String getRandTopologyID(Boolean initial)
    {
        if(topologiesPermutation == null) {
            topologiesPermutation = new ArrayList<String>(topologyBlocks.keySet());
            Collections.shuffle(topologiesPermutation);
        }

        if(topology_counter == topologyBlocks.size()) {
            Collections.shuffle(topologiesPermutation);
            topology_counter = 0;
        }

        String id = topologiesPermutation.get(topology_counter);
        if (!initial) {
            topology_counter++;
        }

        return id;
    }

    public static void updateTopology(String oldTopology, String oldGoalAndSpawnID,
                                     String newTopology, String newGoalAndSpawnID)
    {
        assert(topologyBlocks.containsKey(newTopology));

        ArrayList<BlockPos> newTopologyBlocks = topologyBlocks.get(newTopology);
        ArrayList<BlockPos> oldTopologyBlocks = new ArrayList<BlockPos>();

        HashMap<BlockPos, IBlockState> goalPosBlockTypeNew =
                GoalLocationsAndSpawnpointsManager.getGoalLocationsListWithBlockTypeInfo(newTopology,
                        newGoalAndSpawnID);

        HashMap<BlockPos, IBlockState> goalPosBlockTypeOld = new HashMap<BlockPos, IBlockState>();

        for(BlockPos pos : goalPosBlockTypeNew.keySet())
            newTopologyBlocks.remove(pos);

        if(oldTopology != null && oldGoalAndSpawnID != null)
        {
            oldTopologyBlocks = topologyBlocks.get(oldTopology);

            goalPosBlockTypeOld = GoalLocationsAndSpawnpointsManager.getGoalLocationsListWithBlockTypeInfo(oldTopology,
                            oldGoalAndSpawnID);

            for(BlockPos pos : goalPosBlockTypeOld.keySet())
                oldTopologyBlocks.remove(pos);

            for(BlockPos pos : oldTopologyBlocks)
            {
                if(!newTopologyBlocks.contains(pos) && !goalPosBlockTypeNew.containsKey(pos))
                    MCWrap.world.setBlockToAir(pos);

                else if(!newTopologyBlocks.contains(pos) && goalPosBlockTypeNew.containsKey(pos))
                    MCWrap.world.setBlockState(pos, goalPosBlockTypeNew.get(pos));

                else if(newTopologyBlocks.contains(pos)  && !goalPosBlockTypeNew.containsKey(pos))
                    MCWrap.world.setBlockState(pos, BlockInfoManager.defaultBlockState);

                else FMLCommonHandler.instance().exitJava(-1, false);

                MCWrap.world.markBlockForUpdate(pos);
            }

            for(BlockPos pos : goalPosBlockTypeOld.keySet())
            {
                if(!newTopologyBlocks.contains(pos) && !goalPosBlockTypeNew.containsKey(pos))
                    MCWrap.world.setBlockToAir(pos);

                else if(!newTopologyBlocks.contains(pos) && goalPosBlockTypeNew.containsKey(pos) &&
                        !goalPosBlockTypeNew.get(pos).equals(goalPosBlockTypeOld.get(pos)))
                    MCWrap.world.setBlockState(pos, goalPosBlockTypeNew.get(pos));

                else if(newTopologyBlocks.contains(pos))
                    MCWrap.world.setBlockState(pos, BlockInfoManager.defaultBlockState);

                else if(!newTopologyBlocks.contains(pos) && goalPosBlockTypeNew.containsKey(pos) &&
                        goalPosBlockTypeNew.get(pos).equals(goalPosBlockTypeOld.get(pos)))
                    MCWrap.world.setBlockState(pos, goalPosBlockTypeNew.get(pos));

                else FMLCommonHandler.instance().exitJava(-1, false);

                MCWrap.world.markBlockForUpdate(pos);
            }
        }

        for(BlockPos pos : newTopologyBlocks)
        {
            if(!oldTopologyBlocks.contains(pos) && !goalPosBlockTypeOld.containsKey(pos))
                MCWrap.world.setBlockState(pos, BlockInfoManager.defaultBlockState);

            MCWrap.world.markBlockForUpdate(pos);
        }

        for(BlockPos pos : goalPosBlockTypeNew.keySet())
        {
            if(!oldTopologyBlocks.contains(pos) && !goalPosBlockTypeOld.containsKey(pos))
                MCWrap.world.setBlockState(pos, goalPosBlockTypeNew.get(pos));

            MCWrap.world.markBlockForUpdate(pos);
        }
    }

    public static ArrayList<String> getTopologyIDs()
    {
        return new ArrayList<String>(topologyBlocks.keySet());
    }
}
