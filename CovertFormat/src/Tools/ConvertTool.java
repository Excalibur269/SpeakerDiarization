package Tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import TextGridToSeg.Seg;

public class ConvertTool 
{
	/**
	 * 获取一个文件夹下的所有文件 要求：后缀名为TextGrid
	 * 
	 * @param file
	 * @return
	 */
	public static void getFileList(File file,final String suffix, List<File> result)
	{
		if (!file.isDirectory()){
			result.add(file);
		} else	{
			// 内部匿名类，用来过滤文件类型
			File[] directoryList = file.listFiles(new FileFilter()
			{
				public boolean accept(File file)
				{
					if (file.isDirectory() || file.isFile() && file.getName().indexOf(suffix) > -1){
						return true;
					} else {
						return false;
					}
				}
			});
			for (int i = 0; i < directoryList.length; i++)
			{
				File fileTemp = null;
				if ((fileTemp = directoryList[i]).isFile()) {
					result.add(fileTemp);
				}else {
					getFileList(fileTemp,suffix,result);
				}
			}
		}
	}

	/**两种格式的文件一一对应
	 * 将TextGrid转换为Seg格式
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static void convertSeg(File file) throws IOException 
	{
		String path = file.getAbsolutePath();
		InputStream input = new FileInputStream(path);
		InputStreamReader reader = new InputStreamReader(input, "UTF-8");
		BufferedReader br = new BufferedReader(reader);
		
		path = path.replace(".TextGrid", ".seg");
		String showName = file.getName().replace(".TextGrid", "");
		File segFile = new File(path);
		FileWriter fileWriter = new FileWriter(segFile);
		
		String source = "";
		String temp;
		StringTokenizer tokenizer;
		String speakerName = null;
		Seg seg;
		int line = 0;
		int cycle = 1;
		int start = 0, during = 0;
		while ((temp = br.readLine()) != null) {
			line++;
			if (line >= 15) {
				tokenizer = new StringTokenizer(temp);
				if(cycle == 1){
				}else if(cycle == 2){
					tokenizer.nextToken();
					tokenizer.nextToken();
					start = (int) (100*Float.parseFloat(tokenizer.nextToken()));
					//BigDecimal b = new BigDecimal(start);  
					//start = b.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue(); 
				}else if(cycle == 3){
					tokenizer.nextToken();
					tokenizer.nextToken();
					int end = (int)(100*Float.parseFloat(tokenizer.nextToken()));
					during = end - start;
					//BigDecimal b = new BigDecimal(during);  
					//during = b.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue(); 
				}else if(cycle ==4){
					tokenizer.nextToken();
					tokenizer.nextToken();
					String tempSpeakerName = tokenizer.nextToken();
					if(!tempSpeakerName.equals("\"\"")){
						if(tempSpeakerName.equals("\"A\"")){
							speakerName = "A";
						}else if(tempSpeakerName.equals("\"B\"")){
							speakerName = "B";
						}else if(tempSpeakerName.equals("\"A+B\"")){
							speakerName = "A+B";
						}
						seg = new Seg(showName, start, during, speakerName);
						source += seg.toString();
					}
					else {
						seg = new Seg(showName, start, during, "S");
						source += seg.toString();
					}
					cycle = 0;
				}
					
				cycle++;
			}else{
				continue;
			}
		}
		
		fileWriter.write(source);
		fileWriter.flush();
		fileWriter.close();
	}
	public static void convertTextGrid(File file) throws IOException 
	{
		String path = file.getAbsolutePath();
		InputStreamReader reader = new InputStreamReader(new FileInputStream(path), "UTF-8");
		BufferedReader br = new BufferedReader(reader);
		
		path = path.replace(".seg",".TextGrid");
		OutputStreamWriter write = new OutputStreamWriter(new FileOutputStream(path),"UTF-8");   
        BufferedWriter fileWriter = new BufferedWriter(write);
	
		String source = "";	
		float start = 0, end = 0;
		String speakerName = null;
		int index = 0;
		StringTokenizer tokenizer = null;

		String temp;
		while ((temp = br.readLine()) != null) 
		{
			if (temp.startsWith(";;")) 
			{
				continue;
			}
			
			index++;
			
			tokenizer = new StringTokenizer(temp);
			start = Float.parseFloat(tokenizer.nextToken());
			if(end < start)
			{
				source += "\t\tintervals ["+index+++"]:\r\n\t\t\txmin = "+end+"\r\n\t\t\txmax = "+start+"\r\n\t\t\ttext = \"\"\r\n";
			}
			end = Float.parseFloat(tokenizer.nextToken());
			speakerName = tokenizer.nextToken().equals("0") ? "A" : "B";
			source += "\t\tintervals ["+index+"]:\r\n\t\t\txmin = "+start+"\r\n\t\t\txmax = "+end+"\r\n\t\t\ttext = \""+speakerName+"\"\r\n";
		}
		fileWriter.write("File type = \"ooTextFile\"\r\nObject class = \"TextGrid\"\r\n\r\n"
				+ "xmin = 0\r\nxmax = "+ end +" \r\ntiers? <exists> \r\nsize = 1 \r\nitem []:\r\n\titem [1]:\r\n\t\tclass = \"IntervalTier\"\r\n"
				+"\t\tname = \"SPEAKER\"\r\n\t\txmin = 0\r\n\t\txmax = "+ end +"\r\n\t\tintervals: size = "+index+"\r\n");
		fileWriter.append(source);
		fileWriter.flush();
		fileWriter.close();
	}

	public static String convertSeg(File file,boolean getNS) throws IOException
	{
		String path = file.getAbsolutePath();
		InputStream input = new FileInputStream(path);
		InputStreamReader reader = new InputStreamReader(input, "UTF-16BE");
		BufferedReader br = new BufferedReader(reader);
		
//		String showName = file.getAbsolutePath();//seg中记入绝对路径
		String showName = file.getName().replace(".TextGrid", "");
		
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
//				System.out.println(temp);
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
//						if(tempSpeakerName.equals("\"A+B\"")){
//							seg = new Seg(showName, start,during, "OS");
//						}else
						seg = new Seg(showName, start,during, "S");
						source += seg.toString();
					} else if(getNS){
						seg = new Seg(showName, start,during, "N");
						source += seg.toString();
					}
					cycle = 0;
				}

				cycle++;
			}else if (line == 13){
				tokenizer.nextToken();
				tokenizer.nextToken();
			}else{
				continue;
			}
		}
		
		FileWriter fileWriter = new FileWriter(new File("all_200h.seg"),true);
		fileWriter.append(source);
		fileWriter.flush();
		fileWriter.close();
		return source;
	}
}
