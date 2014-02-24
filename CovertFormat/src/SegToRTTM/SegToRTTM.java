package SegToRTTM;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import SegToRTTM.RTTM;


public class SegToRTTM {

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

	
	
	/**
	 * @param file
	 * @return
	 */
	public String getFilename(File file) {
		String filename = file.getName();
		return filename;
	}
	
	public String convertRTTM(File file) throws IOException {
		String path = file.getAbsolutePath();
		InputStream input = new FileInputStream(path);
		InputStreamReader reader = new InputStreamReader(input, "UTF-8");
		BufferedReader br = new BufferedReader(reader);
		String filename = file.getName();
		filename = filename.substring(0, filename.lastIndexOf(".seg"));
		String source = "";
		String temp;
		StringTokenizer tokenizer;
		String speakerName = null;
		RTTM rttmLine;
		
		float starting = 0;//record start time of each segmentation
		float start = 0;
		float end = 0;
		float during = 0;
		while ((temp = br.readLine()) != null) {
			tokenizer = new StringTokenizer(temp);
			String startString;
			if(!(startString = tokenizer.nextToken()).equals(";;")){
				start = Float.parseFloat(startString);
				end = Float.parseFloat(tokenizer.nextToken());
				BigDecimal b = new BigDecimal(start);  
				start = b.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue(); 
				b = new BigDecimal(end - start);  
				during = b.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue(); 
				speakerName = tokenizer.nextToken();
				if(starting == start){
					rttmLine = new RTTM(filename, start, during, speakerName);
					source += rttmLine.toString(false);
				}else {
					b = new BigDecimal(start-starting);
					float tempDur = b.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue(); 
					if(tempDur > 0){
						rttmLine = new RTTM(filename, starting, tempDur, speakerName);
						source += rttmLine.toString(true);
						rttmLine = new RTTM(filename, start, during, speakerName);
						source += rttmLine.toString(false);
					}
				}
				b = new BigDecimal(end);  
				starting = b.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();			
			}
		}
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
		SegToRTTM segToRTTM = new SegToRTTM();
		String source = "";
		
		List<String> fileList5 = segToRTTM.getFileList(new File("E:/seg_result/"));
		
		for (String s : fileList5) {
			// 打印文件名
			System.out.println(s);
			// 文件内容
			try {
				source += segToRTTM.convertRTTM(new File(s));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
//		List<String> fileList6 = segToRTTM.getFileList(new File("E:/seg_result/huifang_1h"));
//		for (String s : fileList6) {
//			// 打印文件名
//			System.out.println(s);
//			// 文件内容
//			try {
//				source += segToRTTM.convertRTTM(new File(s));
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		List<String> fileList7 = segToRTTM.getFileList(new File("E:/seg_result/service_1.3"));
//		
//		for (String s : fileList7) {
//			// 打印文件名
//			System.out.println(s);
//			// 文件内容
//			try {
//				source += segToRTTM.convertRTTM(new File(s));
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		List<String> fileList1 = segToRTTM.getFileList(new File("E:/seg_result/PICC4h"));
//		
//		for (String s : fileList1) {
//			// 打印文件名
//			System.out.println(s);
//			// 文件内容
//			try {
//				source += segToRTTM.convertRTTM(new File(s));
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		
//		List<String> fileList2 = segToRTTM.getFileList(new File("E:/seg_result/lenovo"));
//		for (String s : fileList2) {
//			// 打印文件名
//			System.out.println(s);
//			// 文件内容
//			try {
//				source += segToRTTM.convertRTTM(new File(s));
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		List<String> fileList3 = segToRTTM.getFileList(new File("E:/seg_result/taikang"));
//		
//		for (String s : fileList3) {
//			// 打印文件名
//			System.out.println(s);
//			// 文件内容
//			try {
//				source += segToRTTM.convertRTTM(new File(s));
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
		File rttmFile = new File("E:/RTTM/Hypothesis.rttm");
		FileWriter fileWriter = new FileWriter(rttmFile);
		fileWriter.write(source);
		fileWriter.flush();
		fileWriter.close();
	}

}
