package umich.ML.mcwrap.map;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.EnumDyeColor;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import umich.ML.mcwrap.util.FileParser;

import java.util.HashMap;
import java.util.List;

/**************************************************
 * Package: umich.ML.mcwrap.map
 * Class: BlockInfoManager
 * Timestamp: 5:36 PM 12/19/15
 * Authors: Valliappa Chockalingam, Junhyuk Oh
 **************************************************/

public class BlockInfoManager {

    private static final HashMap<Integer, IBlockState> blockStateWithGoalID = new HashMap<Integer, IBlockState>();

    static IBlockState defaultBlockState = null;

    public static void init(String filePath)
    {
        Element blockTypeInfo = FileParser.readXML(filePath);

        NodeList nList = blockTypeInfo.getElementsByTagName("block");

        for(int i = 0; i < nList.getLength(); i++)
        {
            NamedNodeMap namedNodeMap = nList.item(i).getAttributes();

            IBlockState blockState = Block.getBlockFromName(namedNodeMap.getNamedItem("type").getNodeValue()).getDefaultState();

            if(namedNodeMap.getNamedItem("color") != null)
            {
                String color = namedNodeMap.getNamedItem("color").getNodeValue();

                List properties = blockState.getProperties().keySet().asList();

                //noinspection ForLoopReplaceableByForEach
                for (int j = 0; j < properties.size(); j++) {
                    IProperty prop = (IProperty) properties.get(j);
                    if (prop.getName().equals("color") && prop.getValueClass() == net.minecraft.item.EnumDyeColor.class)
                        blockState = blockState.withProperty(prop, EnumDyeColor.valueOf(color));
                }
            }

            if(namedNodeMap.getNamedItem("brightness") != null)
            {
                Float brightness = Float.parseFloat(namedNodeMap.getNamedItem("brightness").getNodeValue());

                blockState.getBlock().setLightLevel(brightness);
            }

            if(namedNodeMap.getNamedItem("default") != null)
                defaultBlockState = blockState;

            String goalIDs;

            if(namedNodeMap.getNamedItem("goalID") != null)
            {
                goalIDs = namedNodeMap.getNamedItem("goalID").getNodeValue();

                for (String s : goalIDs.split(" "))
                    blockStateWithGoalID.put(Integer.parseInt(s), blockState);
            }
        }
    }

    public static IBlockState blockTypeWithGoalID(int id)
    {
        return blockStateWithGoalID.get(id);
    }
}
