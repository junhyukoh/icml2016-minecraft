package umich.ML.mcwrap.util;


import au.com.bytecode.opencsv.CSVReader;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class FileParser
{
    public static String[][] readCSV(String filePath)
    {
        String[][] dataArr = null;

        try
        {
            CSVReader csvReader = new CSVReader(new java.io.FileReader(new File(filePath)));
            List<String[]> list = csvReader.readAll();

            dataArr = new String[list.size()][];
            dataArr = list.toArray(dataArr);
        }

        catch(FileNotFoundException e)
        {
            System.out.print("Count not find file: " + filePath);
        }

        catch(IOException e)
        {
            System.out.print("IO Error: " + filePath);
        }

        if(dataArr == null) {
            System.out.print("Data Array is null. Reading CSV file: " + filePath);
            FMLCommonHandler.instance().exitJava(-1, false);
        }

        return dataArr;
    }

    public static Element readXML(String filePath)
    {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        Document doc = null;

        Element root = null;
        try
        {
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(new File(filePath));
        }

        catch(ParserConfigurationException e)
        {
            System.out.print("Parser Configuration Exception on reading file: " + filePath);
        }

        catch(IOException e)
        {
            System.out.print("IO Exception on reading file: " + filePath);
        }

        catch(SAXException e)
        {
            System.out.print("SAX Exception on reading file: " + filePath);
        }

        if(doc == null)
        {
            System.out.print("doc. found to be null! Reading XML File: " + filePath);
        }

        else
        {
            doc.getDocumentElement().normalize();

            root = doc.getDocumentElement();
        }

        if(root == null) {
            System.out.print("Root is null! Reading XML File: " + filePath);
            FMLCommonHandler.instance().exitJava(-1, false);
        }

        return root;
    }
}
