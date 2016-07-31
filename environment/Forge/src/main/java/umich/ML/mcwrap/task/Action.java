package umich.ML.mcwrap.task;

/**************************************************
 * Package: umich.ML.mcwrap.task.action
 * Class: Action
 * Timestamp: 4:48 PM 12/19/15
 * Authors: Valliappa Chockalingam, Junhyuk Oh
 **************************************************/

abstract class Action
{
    String description = null;
    Integer limit = Integer.MAX_VALUE;
    Integer id = null;

    void handleAction()
    {
        // BlockPos oldPos = PlayerState.getPosition();

        if(proposedChangeValid())
            doAction();

        PlayerState.syncPlayer();

        // if(ConfigurationHandler.debug)
        //    BlockDataLogger.logVisit(oldPos, id, PlayerState.getPosition());
    }

    void doAction() {}
    Boolean proposedChangeValid() {return true;}
}
