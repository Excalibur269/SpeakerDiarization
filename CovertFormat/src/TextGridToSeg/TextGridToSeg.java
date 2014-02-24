package TextGridToSeg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import Tools.ConvertTool;



/**
 * @author Excalibur
 *
 */
public class TextGridToSeg 
{
	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		// TODO Auto-generated method stub
		// Test get and read all file
		String pathString = args[0];
		
		List<File> fileList = new ArrayList<File>();
		ConvertTool.getFileList(new File(pathString),".TextGrid",fileList);
			
		for (File fileTemp : fileList) 
		{
			// ÎÄ¼þÄÚÈÝ
			try {
				System.out.println(fileTemp.getAbsolutePath());
				ConvertTool.convertSeg(fileTemp);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
