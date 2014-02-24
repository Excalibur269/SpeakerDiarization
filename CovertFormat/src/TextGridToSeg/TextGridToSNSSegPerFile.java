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

/**
 * @author Excalibur
 * 
 */
public class TextGridToSNSSegPerFile
{
	/**
	 * 获取一个文件夹下的所有文件 要求：后缀名为TextGrid
	 * 
	 * @param file
	 * @return
	 */
	public static List<String> getFileList(File file)
	{
		List<String> result = new ArrayList<String>();
		if (!file.isDirectory()){
			System.out.println(file.getAbsolutePath());
			result.add(file.getAbsolutePath());
		} else	{
			// 内部匿名类，用来过滤文件类型
			File[] directoryList = file.listFiles(new FileFilter()
			{
				public boolean accept(File file)
				{
					if (file.isFile() && file.getName().indexOf(".TextGrid") > -1){
						return true;
					} else {
						return false;
					}
				}
			});
			for (int i = 0; i < directoryList.length; i++)
			{
				result.add(directoryList[i].getAbsolutePath());
			}
		}
		return result;
	}

	/**
	 * @param file
	 * @return
	 */
	public String getFilename(File file)
	{
		String filename = file.getName();
		return filename;
	}

	/**
	 * 将TextGrid转换为Seg格式
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	static int wholeFileTime = 0;
	public static String convertSeg(String dir,File file) throws IOException
	{
		String path = file.getAbsolutePath();
		InputStream input = new FileInputStream(path);
		InputStreamReader reader = new InputStreamReader(input, "UTF-16BE");
		BufferedReader br = new BufferedReader(reader);
		
		String filename = file.getName();
		filename = filename.substring(0,filename.lastIndexOf("."));
		String showName = dir+"/"+filename;
		
		String source = "";
		String temp;
		StringTokenizer tokenizer;
		Seg seg;
		int line = 0;
		int cycle = 1;
		int start = 0, during = 0, end = 0;
		int globalStart = 0;
		int nonspeechTime = 0;
		while ((temp = br.readLine()) != null)
		{
			line++;
			tokenizer = new StringTokenizer(temp);
			if (line >= 15)
			{

				if (cycle == 1){
					
				} else if (cycle == 2){
					tokenizer.nextToken();
					tokenizer.nextToken();
					start = (int) (100 * Float.parseFloat(tokenizer.nextToken()));
				} else if (cycle == 3){
					tokenizer.nextToken();
					tokenizer.nextToken();
					end = (int) (100 * Float.parseFloat(tokenizer.nextToken()));
					during = end - start;
					globalStart = start - nonspeechTime;
				} else if (cycle == 4){
					tokenizer.nextToken();
					tokenizer.nextToken();
					String tempSpeakerName = tokenizer.nextToken();

					if (during < 20)
					{
						cycle = 1;
						continue;
					}

					if (!tempSpeakerName.equals("\"\"")){
						start = globalStart;
						seg = new Seg(showName, start,during, "S");
						source += seg.toString();
					} else {
						seg = new Seg(showName, start,during, "NS");
						source += seg.toString();
					}
					cycle = 0;
				}

				cycle++;
			}else if (line == 13){
				tokenizer.nextToken();
				tokenizer.nextToken();
				wholeFileTime += (int) (100 * Float.parseFloat(tokenizer.nextToken()));
			}else{
				continue;
			}
		}
		
		FileWriter fileWriter = new FileWriter(new File("all.seg"),true);
		fileWriter.append(source);
		fileWriter.flush();
		fileWriter.close();
		return source;
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException
	{
		// TODO Auto-generated method stub
		// Test get and read all file
		
		String pathString = "../puqiang/All_TextGird/";
		String[]  dirSet = new String[]{
//			"jiangsudianxin",
//			"JiangSuDianXin4h",
			"lenovo",
			"PICC4h",
			"taikang",
			"taikang_huifang4h",
			"TaiKang4h"
		};
		
		for(String dir : dirSet)
		{
			List<String> fileList = TextGridToSNSSegPerFile.getFileList(new File(pathString + dir));
			
			for (String file : fileList)
			{
				// 打印文件名
				System.out.println(dir);
				// 文件内容
				try	{
					TextGridToSNSSegPerFile.convertSeg(dir,new File(file));
				}catch (IOException e){
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		System.out.print(wholeFileTime);
	}

}
