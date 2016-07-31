package umich.ML.mcwrap.logger;

import java.util.*;

/**************************************************
 * Package: umich.ML.mcwrap.logger
 * Class: Logger
 * Timestamp: 5:53 PM 12/21/15
 * Authors: Valliappa Chockalingam, Junhyuk Oh
 **************************************************/

public class Logger {

    private static Vector<Integer> numTimes = new Vector<Integer>();
    private static Vector<Double> cumulativeReward = new Vector<Double>();

    private static int currTopology = -1;
    private static double currTrajReward = 0.0D;

    public static void init(ArrayList<String> topologyIds)
    {
        for(int tID = 0; tID < topologyIds.size(); tID++)
        {
            numTimes.add(tID, 0);
            cumulativeReward.add(tID, 0.0D);
        }
    }

    public static void logTrajectory(String topology_id)
    {
        currTopology = Integer.parseInt(topology_id, 10);
    }

    public static void logReward(double reward)
    {
        currTrajReward += reward;
    }

    public static void logTermination()
    {
        cumulativeReward.set(currTopology, cumulativeReward.get(currTopology) + currTrajReward);
        currTrajReward = 0;

        numTimes.set(currTopology, numTimes.get(currTopology) + 1);
    }

    public static void reinit(ArrayList<String> topologyIds)
    {
        try
        {
            final String os = System.getProperty("os.name");

            if (os.contains("Windows"))
            {
                Runtime.getRuntime().exec("cls");
            }
            else
            {
                Runtime.getRuntime().exec("clear");
            }
        }

        catch (final Exception e)
        {
            System.out.print("Caught exception on clearing console! Will continue with caution.\n");
        }

        System.out.print("--------- Log ---------\n");

        for(int tID = 0; tID < numTimes.size(); tID++)
        {
            if(numTimes.get(tID) > 0) {
                System.out.print(tID + ": " + cumulativeReward.get(tID) /
                        (double) numTimes.get(tID) + "\n");
            }

            //else System.out.print(topologyID + ": " + "Not used in the last " +
            //ConfigurationHandler.getLogFreq() + " trajectories.\n");
        }

        System.out.print("-----------------------\n");

        for(int tID = 0; tID < topologyIds.size(); tID++)
        {
            numTimes.set(tID, 0);
            cumulativeReward.set(tID, 0.0D);
        }
    }
}
