package SegToRTTM;
import java.lang.Thread.State;


public class RTTM implements Comparable{
	public static String SPEAKER = "SPEAKER";
	public static String NONSPEACH = "NON-SPEECH"; 
	public static String NA = "<NA>";
	public static int CHANNEL = 1;
	
	private String filename;
	private float startingTime;
	private float duringTime;
	private String speakerName;
	
	public RTTM(String filenane, float startingTime, float duringTime, String speakerName){
		this.filename = filenane;
		this.startingTime = startingTime;
		this.duringTime = duringTime;
		this.speakerName = speakerName;
	}
	
	public String getSpeakerName() {
		return speakerName;
	}
	public void setSpeakerName(String speakerName) {
		this.speakerName = speakerName;
	}
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public float getStartingTime() {
		return startingTime;
	}
	public void setStartingTime(float startingTime) {
		this.startingTime = startingTime;
	}
	public float getDuringTime() {
		return duringTime;
	}
	public void setDuringTime(float duringTime) {
		this.duringTime = duringTime;
	}
	public String toString(boolean slience){
		String string;
		if(slience){
			string = NONSPEACH+" "+filename+" "+CHANNEL+" "+startingTime+" "+duringTime+" "+NA+" other "+NA+" "+NA+"\r\n";
		}else{
			string = SPEAKER+" "+filename+" "+CHANNEL+" "+startingTime+" "+duringTime+" "+NA+" "+NA+" "+speakerName+" "+NA+"\r\n";
		}
		return string;
	}

	@Override
	public int compareTo(Object obj) {
		// TODO Auto-generated method stub
		RTTM rttm = (RTTM)obj;
		if((startingTime - rttm.getStartingTime()) > 0){
			return 1;
		}else{
			return  -1;
		}
	}
}
