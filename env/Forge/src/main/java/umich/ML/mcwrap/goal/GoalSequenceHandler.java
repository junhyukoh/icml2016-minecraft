package umich.ML.mcwrap.goal;

import net.minecraftforge.fml.common.FMLCommonHandler;
import umich.ML.mcwrap.task.ActionHandler;

import java.util.ArrayList;

/**************************************************
 * Package: umich.ML.mcwrap.fsm
 * Class: GoalSequenceHandler
 * Timestamp: 8:10 PM 12/19/15
 * Authors: Valliappa Chockalingam, Junhyuk Oh
 **************************************************/

class GoalSequenceHandler {
    private final String rewardAndTerminateString;
    private int indexInGoalSequence = -1;
    private final ArrayList<Integer> goalSequence;
    private final Boolean commit;
    private final Boolean push;
    private final String rewardForDeviatingFromTrajectory;

    public GoalSequenceHandler(ArrayList<Integer> goalSeq, String rewardWithTerminate, Boolean commit_,
                               Boolean push_, String rewardForDeviatingFromTrajectory_)
    {
        rewardAndTerminateString = rewardWithTerminate;
        goalSequence = goalSeq;

        if(rewardAndTerminateString.split(" ").length > 2)
            assert(rewardAndTerminateString.split(" ").length == goalSequence.size() + 1);
        else assert(rewardWithTerminate.split(" ").length == 2);

        commit = commit_;
        push = push_;

        if(push && ActionHandler.pushAction == null)
        {
            System.out.print("Please check that a push action is listed in the actions.xml file!\n");
            FMLCommonHandler.instance().exitJava(-1, false);
        }

        rewardForDeviatingFromTrajectory = rewardForDeviatingFromTrajectory_;
    }

    public void reset()
    {
        indexInGoalSequence = -1;
    }

    public String step(int goalID, int action)
    {
        if((goalSequence.get(indexInGoalSequence + 1) == goalID && !push) ||
                (goalSequence.get(indexInGoalSequence + 1) == goalID && push &&
                        action == ActionHandler.pushAction)) {
            String to_ret = "UNASSIGNED";
            indexInGoalSequence++;

            if(rewardAndTerminateString.split(" ").length > 2)
            to_ret = rewardAndTerminateString.split(" ")[indexInGoalSequence] + " " +
                    (indexInGoalSequence == goalSequence.size() - 1 ?
                    rewardAndTerminateString.split(" ")[goalSequence.size()] : "0");

            else if(!(rewardAndTerminateString.split(" ").length > 2) &&
                    indexInGoalSequence != goalSequence.size() - 1)
                to_ret = null;

            else if(!(rewardAndTerminateString.split(" ").length > 2) &&
                    indexInGoalSequence == goalSequence.size() - 1)
                to_ret = rewardAndTerminateString;

            if((!(rewardAndTerminateString.split(" ").length > 2) &&
                    rewardAndTerminateString.split(" ")[1].equals("0")) ||
               (rewardAndTerminateString.split(" ").length > 2 &&
                       rewardAndTerminateString.split(" ")[goalSequence.size()].equals("0")))
                    indexInGoalSequence = -1;

            assert((to_ret != null && !to_ret.equals("UNASSIGNED")) || to_ret == null);
            return to_ret;
        }

        else if(commit && goalSequence.get(indexInGoalSequence + 1) != goalID && goalSequence.contains(goalID) &&
                (!push || action == ActionHandler.pushAction))
        {
            indexInGoalSequence = -1;
            if(rewardForDeviatingFromTrajectory == null)
                return null;
            else return rewardForDeviatingFromTrajectory + " 1";
        }

        else return null;
    }
}
