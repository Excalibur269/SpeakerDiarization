package TextGridToRTTM;

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
import java.util.List;
import java.util.StringTokenizer;

public class TextGridToRTTM {

	/**
	 * 获取一个文件夹下的所有文件 要求：后缀名为seg
	 * 
	 * @param file
	 * @return
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
					if (file.isFile()
							&& file.getName().indexOf(".TextGrid") > -1) {
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

	/**
	 * 以UTF-8编码方式读取文件内容
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public String getContentByLocalFile(File path) throws IOException {
		InputStream input = new FileInputStream(path);
		InputStreamReader reader = new InputStreamReader(input, "utf-8");
		BufferedReader br = new BufferedReader(reader);
		StringBuilder builder = new StringBuilder();
		String temp = null;
		while ((temp = br.readLine()) != null) {
			builder.append(temp);
		}
		return builder.toString();
	}

	/**
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public String convertRTTM(File file) throws IOException {
		String path = file.getAbsolutePath();
		InputStream input = new FileInputStream(path);
		InputStreamReader reader = new InputStreamReader(input, "UTF-16BE");
		BufferedReader br = new BufferedReader(reader);
		String filename = file.getName();
		filename = filename.substring(0,filename.lastIndexOf("."));
		String parentPath  = file.getParent();
		//File rttmFile = new File("/media/zhangbihong/学习娱乐/学习/puqiang/RTTM/"+filename + "_r.RTTM");
		//FileWriter fileWriter = new FileWriter(rttmFile);
		String source = "";
		String temp;
		StringTokenizer tokenizer;
		String speakerName = null;
		RTTM rttmLine;
		int line = 0;
		int cycle = 1;
		float start = 0, during = 0;
		while ((temp = br.readLine()) != null) {
			line++;
			//System.out.println(line+": "+temp);
			if (line >= 15) {
				tokenizer = new StringTokenizer(temp);
				if(cycle == 1){
				}else if(cycle == 2){
					tokenizer.nextToken();
					tokenizer.nextToken();
					start = Float.parseFloat(tokenizer.nextToken());
					BigDecimal b = new BigDecimal(start);  
					start = b.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue(); 
				}else if(cycle == 3){
					tokenizer.nextToken();
					tokenizer.nextToken();
					float end = Float.parseFloat(tokenizer.nextToken());
					during = end - start;
					BigDecimal b = new BigDecimal(during);  
					during = b.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue(); 
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
						rttmLine = new RTTM(filename, start, during, speakerName);
						source += rttmLine.toString(false);//speaker
						//System.out.println(source);
					}else {
						rttmLine = new RTTM(filename, start, during, speakerName);
						source += rttmLine.toString(true);//non-speech
					}
					cycle = 0;
				}
					
				cycle++;
			}else{
				continue;
			}
		}
		//fileWriter.write(source);
		//fileWriter.flush();
		//fileWriter.close();
		return source;
	}

	
	public File convertSeg(File file) throws IOException {
		String path = file.getAbsolutePath();
		InputStream input = new FileInputStream(path);
		InputStreamReader reader = new InputStreamReader(input, "UTF-16BE");
		BufferedReader br = new BufferedReader(reader);
		String filename = file.getName();
		filename = filename.substring(0,filename.lastIndexOf("."));
		String parentPath  = file.getParent();
		File segFile = new File(parentPath+"\\"+filename + "_h.mdtm");
		FileWriter fileWriter = new FileWriter(segFile);
		String source = "";
		String temp;
		StringTokenizer tokenizer;
		String speakerName = null;
		RTTM rttmLine;
		ArrayList<Seg> mdtms = new ArrayList<Seg>();
		//int line = 0;
		int cycle = 1;
		double start = 0;
		double during = 0;
		while ((temp = br.readLine()) != null) {
				tokenizer = new StringTokenizer(temp);
				if(!tokenizer.nextToken().equals(";;")){
					tokenizer.nextToken();
					start = Double.parseDouble(tokenizer.nextToken())/100.0;
					during = Double.parseDouble(tokenizer.nextToken())/100.0;
					tokenizer.nextToken();
					tokenizer.nextToken();
					tokenizer.nextToken();
					speakerName = tokenizer.nextToken();
					Seg mdtm = new Seg(filename, start, during, speakerName);
					mdtms.add(mdtm);
				}	
		}
		Collections.sort(mdtms);
		for(Seg seg : mdtms){
			source += seg.toString();
		}
		fileWriter.write(source);
		fileWriter.flush();
		fileWriter.close();
		return segFile;
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		// Test get and read all file
		TextGridToRTTM textGridToRTTM = new TextGridToRTTM();
		List<String> fileList1 = textGridToRTTM.getFileList(new File("E:/KeFu/heli_wav_24"));
//		List<String> fileList1 = textGridToRTTM.getFileList(new File("E:/KeFu/jiangsudianxin"));
//		List<String> fileList2 = textGridToRTTM.getFileList(new File("E:/KeFu/huifang_1h"));
//		List<String> fileList3 = textGridToRTTM.getFileList(new File("E:/KeFu/service_1.3"));
//		List<String> fileList4 = textGridToRTTM.getFileList(new File("E:/KeFu/lenovo"));
//		List<String> fileList5 = textGridToRTTM.getFileList(new File("E:/KeFu/PICC4h"));
//		List<String> fileList6 = textGridToRTTM.getFileList(new File("E:/KeFu/taikang"));
		File file = null;
		String[] content = null;
		String source = "";
		for (String s : fileList1) {
			// 打印文件名
			System.out.println(s);
			// 文件内容
			try {
				source += textGridToRTTM.convertRTTM(new File(s));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
//		for (String s : fileList2) {
//			// 打印文件名
//			System.out.println(s);
//			// 文件内容
//			try {
//				source += textGridToRTTM.convertRTTM(new File(s));
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//
//		}
//		for (String s : fileList3) {
//			// 打印文件名
//			System.out.println(s);
//			// 文件内容
//			try {
//				source += textGridToRTTM.convertRTTM(new File(s));
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//
//		}
//		for (String s : fileList4) {
//			// 打印文件名
//			System.out.println(s);
//			// 文件内容
//			try {
//				source += textGridToRTTM.convertRTTM(new File(s));
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//
//		}
//		for (String s : fileList5) {
//			// 打印文件名
//			System.out.println(s);
//			// 文件内容
//			try {
//				source += textGridToRTTM.convertRTTM(new File(s));
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//
//		}
//		for (String s : fileList6) {
//			// 打印文件名
//			System.out.println(s);
//			// 文件内容
//			try {
//				source += textGridToRTTM.convertRTTM(new File(s));
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//
//		}
	
		File rttmFile = new File("E:/RTTM/Reference_24.rttm");
		FileWriter fileWriter = new FileWriter(rttmFile);
		fileWriter.write(source);
		fileWriter.flush();
		fileWriter.close();
	}

}
