package umich.ML.mcwrap.event;

import umich.ML.mcwrap.MCWrap;

/**************************************************
 * Package: umich.ML.mcwrap.event
 * Class: ShutdownHook
 * Timestamp: 2:45 PM 12/20/15
 * Authors: Valliappa Chockalingam, Junhyuk Oh
 **************************************************/

public class ShutdownHook {
    public static void logWorldDirName(final String WorldDirName)
    {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                MCWrap.minecraft.getSaveLoader().deleteWorldDirectory(WorldDirName);
            }
        });
    }
}
