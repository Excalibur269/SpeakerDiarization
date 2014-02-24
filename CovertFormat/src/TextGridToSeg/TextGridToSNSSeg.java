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
public class TextGridToSNSSeg
{	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException
	{
		// TODO Auto-generated method stub
		// Test get and read all file

		String pathString = args[0];
		
		/*收集所有的 textGrid 文件*/
		List<File> fileList = new ArrayList<File>();
		ConvertTool.getFileList(new File(pathString),".TextGrid",fileList);
			
		for (File file : fileList)
		{
			// 打印文件名
			// 文件内容
			try	{
				System.out.println(file);
				ConvertTool.convertSeg(file,true);
			}catch (IOException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
