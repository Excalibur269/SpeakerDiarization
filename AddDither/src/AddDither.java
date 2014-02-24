import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class AddDither {

	public static List<String> getFileList(File file) {
		List<String> result = new ArrayList<String>();
		if (!file.isDirectory()) {
			System.out.println(file.getAbsolutePath());
			result.add(file.getAbsolutePath());
		} else {
			// 内部匿名类，用来过滤文件类型
			File[] directoryList = file.listFiles(new FileFilter() {
				public boolean accept(File file) {
					if (file.isFile()
							&& file.getName().indexOf(".wav") > -1) {
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
	 * Add dither
	 * Code by 张弼弘
	 * @param featureData
	 */
	public static float[] addDither(float[] featureData){
		int size;
		Random random = new Random(1);
        size = featureData.length;
        for (int i = 0;i < size; i++){
        	float randomNum =  random.nextFloat();//返回[0-1]之间的float数值。
        	featureData[i] += (Math.abs(randomNum) * 2.0f - 1.0f) * 0.01f;
        }
        return featureData;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String pathName = args[0];
		List<String> fileList = getFileList(new File(pathName));
		for (String filename : fileList) {
			// 打印文件名
			if(!filename.contains("add")){
				System.out.println(filename);
				WaveFileReader wavFile = new WaveFileReader(filename);
				WaveFileWriter newWaveFile = new WaveFileWriter();
				float[] samples = new float[wavFile.getDataLen()];
				for(int i = 0;i < samples.length;i++){
					samples[i] = (float)wavFile.getData()[0][i] / 32767.0f;
				}
				samples = addDither(samples);
				File outWav = new File(filename.substring(0, filename.indexOf(".wav"))+"_add.wav");
				try {
					newWaveFile.write(samples, outWav);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
