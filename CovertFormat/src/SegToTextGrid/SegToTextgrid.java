package SegToTextGrid;

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
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.stream.events.StartDocument;

import SegToRTTM.RTTM;

public class SegToTextgrid {

	/**
	 * 获取一个文件夹下的所有文件 要求：后缀名为seg
	 * 
	 * @param file
	 * @return _reorder.seg
	 */
	public List<String> getFileList(File file) {
		List<String> result = new ArrayList<String>();
		if (!file.isDirectory()) {
			System.out.println(file.getAbsolutePath());
			result.add(file.getAbsolutePath());
		} else {
			// 内部匿名类，用来过滤文件类型
			File[] directoryList = file.listFiles(new FileFilter() {
				public boolean accept(File file) {
					if (file.isFile() && file.getName().indexOf(".seg") > -1) {
						return true;
					} else {
						return false;
					}
				}
			});
			for (int i = 0; i < directoryList.length; i++) {
				result.add(directoryList[i].getAbsolutePath());
			}
		}
		return result;
	}

	
	public List<String> getFileList2(File file) {
		List<String> result = new ArrayList<String>();
		if (!file.isDirectory()) {
			System.out.println(file.getAbsolutePath());
			result.add(file.getAbsolutePath());
		} else {
			// 内部匿名类，用来过滤文件类型
			File[] directoryList = file.listFiles(new FileFilter() {
				public boolean accept(File file) {
					if (file.isFile() && file.getName().indexOf(".txt") > -1) {
						return true;
					} else {
						return false;
					}
				}
			});
			for (int i = 0; i < directoryList.length; i++) {
				result.add(directoryList[i].getAbsolutePath());
			}
		}
		return result;
	}
	
	/**
	 * @param file
	 * @return
	 */
	public String getFilename(File file) {
		String filename = file.getName();
		return filename;
	}

	public File reOrder(File file) throws IOException{
		String path = file.getAbsolutePath();
		InputStream input = new FileInputStream(path);
		InputStreamReader reader = new InputStreamReader(input, "UTF-8");
		BufferedReader br = new BufferedReader(reader);
		String filename = file.getName();
		filename = filename.substring(0, filename.lastIndexOf(".seg"));
		String parentPath = file.getParent();
		File reOrderFile = new File(parentPath + "/" + filename + ".txt");
		FileWriter fileWriter = new FileWriter(reOrderFile);
		String source = "";
		String temp;
		double start = 0, during = 0;
		ArrayList<RTTM> rttms = new ArrayList<RTTM>();
		while ((temp = br.readLine()) != null) {
			StringTokenizer tokenizer = new StringTokenizer(temp);
			if(!tokenizer.nextToken().equals(";;")){
				tokenizer.nextToken();
				start = Double.parseDouble(tokenizer.nextToken())/100.0;
				during = Double.parseDouble(tokenizer.nextToken())/100.0;
				tokenizer.nextToken();
				tokenizer.nextToken();
				tokenizer.nextToken();
				String speakerName = tokenizer.nextToken();
				RTTM rttm = new RTTM(filename, (float)start, (float)during, speakerName);
				rttms.add(rttm);
			}	
		}
		Collections.sort(rttms);
		
		for(RTTM rttm : rttms){
			source += rttm.toString(false);
		}
		double fileLength = start + during;
		source = fileLength + "\n"+ source;
		fileWriter.write(source);
		fileWriter.flush();
		fileWriter.close();
		return reOrderFile;
	}
	
	
	public void convertTextGrid(File file) throws IOException {
		String path = file.getAbsolutePath();
		InputStream input = new FileInputStream(path);
		InputStreamReader reader = new InputStreamReader(input, "UTF-8");
		BufferedReader br = new BufferedReader(reader);
		String filename = file.getName();
		filename = filename.substring(0, filename.lastIndexOf(".txt"));
		String parentPath = file.getParent();
		File textgridFile = new File(parentPath + "/" + filename + "_h.TextGrid");
		OutputStreamWriter write = new OutputStreamWriter(new FileOutputStream(textgridFile),"UTF-16BE");   
        BufferedWriter fileWriter = new BufferedWriter(write);     
		String source = "﻿File type = \"ooTextFile\"\r\nObject class = \"TextGrid\"\r\n\r\n";
		String specific = "";
		String temp;
		StringTokenizer tokenizer;
		String speakerName = null;
		
		float starting = 0;//record start time of each segmentation
		float start = 0;
		float during = 0;
		float end = 0;
		temp = br.readLine();
		int index = 0;
		double fileLength = Double.parseDouble(temp);
		
		while ((temp = br.readLine()) != null) {
			index++;
			tokenizer = new StringTokenizer(temp);
			tokenizer.nextToken();
			tokenizer.nextToken();
			tokenizer.nextToken();
			start = Float.parseFloat(tokenizer.nextToken());
			during = Float.parseFloat(tokenizer.nextToken());
			BigDecimal b = new BigDecimal(start);  
			start = b.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue(); 
			b = new BigDecimal(during);  
			during = b.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue(); 
			end = start + during;
			tokenizer.nextToken();
			tokenizer.nextToken();
			speakerName = tokenizer.nextToken();
			if(starting == start){
				specific += "\t\tintervals ["+index+"]:\r\n\t\txmin = "+start+"\r\n\t\txmax = "+end+"\r\n\t\ttext = \""+speakerName+"\"\r\n";
			}else {
				b = new BigDecimal(start-starting);
				float tempDur = b.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue(); 
				if(tempDur > 0){
					float tempEnd = starting+tempDur;
					specific += "\t\tintervals ["+index+++"]:\r\n\t\txmin = "+starting+"\r\n\t\txmax = "+tempEnd+"\r\n\t\ttext = \"\"\r\n";
					specific += "\t\tintervals ["+index+"]:\r\n\t\txmin = "+start+"\r\n\t\txmax = "+end+"\r\n\t\ttext = \""+speakerName+"\"\r\n";
				}
			}
			b = new BigDecimal(start + during);  
			starting = b.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();			
		}
		source = source + "xmin = 0\r\nxmax = "+fileLength+" \r\ntiers? <exists> \r\nsize = 1 \r\nitem []:\r\n\titem [1]:\r\n\t\tclass = \"IntervalTier\"\r\n"
				+"\t\tname = \"SPEAKER\"\r\n\t\txmin = 0\r\n\t\txmax = "+fileLength+"\r\n\t\tintervals: size = "+index+"\r\n"+specific;
		fileWriter.write(source);
		fileWriter.flush();
		fileWriter.close();
//		return textgridFile;
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException 
	{
		// TODO Auto-generated method stub
		// Test get and read all file
		SegToTextgrid segToTextGrid = new SegToTextgrid();
		String source = "";
		

		List<String> refileList3 = segToTextGrid.getFileList(new File("E:/DiaResult"));
		for (String s : refileList3) {
			// 打印文件名
			System.out.println(s);
			// 文件内容
			try {
				segToTextGrid.reOrder(new File(s));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		List<String> fileList3 = segToTextGrid.getFileList2(new File("E:/DiaResult"));
		
		for (String s : fileList3) {
			// 打印文件名
			System.out.println(s);
			// 文件内容
			try {
				segToTextGrid.convertTextGrid(new File(s));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

//		File rttmFile = new File("E:/puqiang/RTTM/taikang_all_h.rttm");
//		FileWriter fileWriter = new FileWriter(rttmFile);
//		fileWriter.write(source);
//		fileWriter.flush();
//		fileWriter.close();
	}

}
