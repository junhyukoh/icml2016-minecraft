package umich.ML.mcwrap.goal;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import umich.ML.mcwrap.util.FileParser;

import java.util.ArrayList;

/**************************************************
 * Package: umich.ML.mcwrap.fsm
 * Class: GoalFSM
 * Timestamp: 7:56 PM 12/19/15
 * Authors: Valliappa Chockalingam, Junhyuk Oh
 **************************************************/

public class GoalFSM {

    private static final ArrayList<GoalSequenceHandler> goalTrajectories = new ArrayList<GoalSequenceHandler>();
    public static String defaultRewardAndTerminate = null;

    public static void init(String goalInfoFile)
    {
        Element goalInfo = FileParser.readXML(goalInfoFile);

        NodeList nList = goalInfo.getElementsByTagName("goal");

        for (int i = 0; i < nList.getLength(); i++)
        {
            NamedNodeMap namedNodeMap = nList.item(i).getAttributes();

            String reward = namedNodeMap.getNamedItem("reward").getNodeValue();
            String terminate = namedNodeMap.getNamedItem("terminate").getNodeValue();

            ArrayList<Integer> goalTrajectory = new ArrayList<Integer>();

            String objectIDStrings;

            if(namedNodeMap.getNamedItem("objectID") != null)
            {
                objectIDStrings = namedNodeMap.getNamedItem("objectID").getNodeValue();

                for (String s : objectIDStrings.split(" "))
                    goalTrajectory.add(Integer.parseInt(s));
            }

            Boolean commit = false;
            Boolean push = false;
            String rewardForDeviatingFromTrajectory = null;

            if(namedNodeMap.getNamedItem("push") != null)
            {
                String push_str = namedNodeMap.getNamedItem("push").getNodeValue();
                push = Boolean.parseBoolean(push_str);
            }

            if(namedNodeMap.getNamedItem("commit") != null)
            {
                String commit_str = namedNodeMap.getNamedItem("commit").getNodeValue();
                commit = Boolean.parseBoolean(commit_str);
            }

            if(namedNodeMap.getNamedItem("rewardForDeviatingFromTrajectory") != null)
            {
                rewardForDeviatingFromTrajectory = namedNodeMap.
                        getNamedItem("rewardForDeviatingFromTrajectory").getNodeValue();
            }

            if(goalTrajectory.size() != 0)
                goalTrajectories.add(new GoalSequenceHandler(goalTrajectory, reward + " " + terminate, commit, push,
                        rewardForDeviatingFromTrajectory));

            if(goalTrajectory.size() == 0)
            {
                assert(defaultRewardAndTerminate == null);
                defaultRewardAndTerminate = reward + " " + terminate;
            }
        }
    }

    public static void reset()
    {
        for(GoalSequenceHandler goalTrajectory : goalTrajectories)
            goalTrajectory.reset();
    }

    public static String step(Integer goalID, Integer action)
    {
        if(goalID == null) return defaultRewardAndTerminate;

        else {
            ArrayList<String> rewardAndTerminateList = new ArrayList<String>();

            for (GoalSequenceHandler goalTrajectory : goalTrajectories) {
                String rewardAndTerminate = goalTrajectory.step(goalID, action);

                if (rewardAndTerminate != null)
                    rewardAndTerminateList.add(rewardAndTerminate);
            }

            if(rewardAndTerminateList.size() > 0) {
                double reward = 0;
                int terminate = 0;
                for(String rewardAndTerminate : rewardAndTerminateList)
                {
                    reward += Double.parseDouble(rewardAndTerminate.split(" ")[0]);
                    terminate += Integer.parseInt(rewardAndTerminate.split(" ")[1]);
                }

                assert(terminate == 0 || terminate == 1);

                return Double.toString(reward) + " " + Integer.toString(terminate);
            }

            else return defaultRewardAndTerminate;
        }
    }

}
