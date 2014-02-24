import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class WaveFileWriter {

	private static byte[] RIFF = "RIFF".getBytes();
	private static byte[] RIFF_SIZE = new byte[4];
	private static byte[] RIFF_TYPE = "WAVE".getBytes();

	private static byte[] FORMAT = "fmt ".getBytes();
	private static byte[] FORMAT_SIZE = new byte[4];
	private static byte[] FORMATTAG = new byte[2];
	private static byte[] CHANNELS = new byte[2];
	private static byte[] SamplesPerSec = new byte[4];
	private static byte[] AvgBytesPerSec = new byte[4];
	private static byte[] BlockAlign = new byte[2];
	private static byte[] BitsPerSample = new byte[2];

	private static byte[] DataChunkID = "data".getBytes();
	private static byte[] DataSize = new byte[4];
	public static boolean isrecording = false;

	public static void init() {
		// 这里主要就是设置参数，要注意revers函数在这里的作用
		FORMAT_SIZE = new byte[] { (byte) 16, (byte) 0, (byte) 0, (byte) 0 };
		byte[] tmp = revers(intToBytes(1));
		FORMATTAG = new byte[] { tmp[0], tmp[1] };
		CHANNELS = new byte[] { tmp[0], tmp[1] };
		SamplesPerSec = revers(intToBytes(8000));
		AvgBytesPerSec = revers(intToBytes(16000));
		tmp = revers(intToBytes(2));
		BlockAlign = new byte[] { tmp[0], tmp[1] };
		tmp = revers(intToBytes(16));
		BitsPerSample = new byte[] { tmp[0], tmp[1] };
	}

	public static byte[] revers(byte[] tmp) {
		byte[] reversed = new byte[tmp.length];
		for (int i = 0; i < tmp.length; i++) {
			reversed[i] = tmp[tmp.length - i - 1];
//			reversed[i] = tmp[i];

		}
		return reversed;
	}

	public static byte[] shortToBytes(short num){
		byte[] bytes = new byte[2];
		bytes[0] = (byte) (num >> 8);
		bytes[1] = (byte) (num & 0x000000FF);
		return bytes;
	}
	public static byte[] intToBytes(int num) {
		byte[] bytes = new byte[4];
		bytes[0] = (byte) (num >> 24);
		bytes[1] = (byte) ((num >> 16) & 0x000000FF);
		bytes[2] = (byte) ((num >> 8) & 0x000000FF);
		bytes[3] = (byte) (num & 0x000000FF);
		return bytes;

	}

	public int write(float[] samples, File out) throws IOException {
		// TODO Auto-generated method stub
		ByteArrayOutputStream bytebuff = new ByteArrayOutputStream(96000000);
		byte[] temp = new byte[2];
		for (int i = 0; i < samples.length; i++) {
			temp = revers(shortToBytes((short) (samples[i] * 32767)));
//			temp = revers(shortToBytes((short)samples[i]));
			bytebuff.write(temp);
		}
		DataSize = revers(intToBytes(samples.length * 2));
		RIFF_SIZE = revers(intToBytes(samples.length * 2 + 36 - 8));
		FileOutputStream fileOS = new FileOutputStream(out);
		BufferedOutputStream fw = new BufferedOutputStream(fileOS);
		init();
		fw.write(RIFF);
		fw.write(RIFF_SIZE);
		fw.write(RIFF_TYPE);
		fw.write(FORMAT);
		fw.write(FORMAT_SIZE);
		fw.write(FORMATTAG);
		fw.write(CHANNELS);
		fw.write(SamplesPerSec);
		fw.write(AvgBytesPerSec);
		fw.write(BlockAlign);
		fw.write(BitsPerSample);
		fw.write(DataChunkID);
		fw.write(DataSize);
		fw.write(bytebuff.toByteArray());
		fw.flush();
		return 0;
	}

}
