package TextGridToRTTM;

public class Seg implements Comparable{
	private String filename;
	private final String SPEAKER = "speaker";
	private final String NA = "NA";
	private String speakerName;
	private double startingtime;
	private double duringtime;
	
	public Seg(String filename, double startingtime, double duringtme, String speakerName){
		this.filename = filename;
		this.startingtime = startingtime;
		this.duringtime = duringtme;
		this.speakerName = speakerName;
	}
	
	public String toString(){
		return filename+" 1 "+startingtime+" "+duringtime+" "+SPEAKER+" "+NA+" "+speakerName+"\n";
	}

	@Override
	public int compareTo(Object obj) {
		// TODO Auto-generated method stub
		Seg other = (Seg)obj;
		return (int)startingtime - (int)other.getStartingtime();
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getSpeakerName() {
		return speakerName;
	}

	public void setSpeakerName(String speakerName) {
		this.speakerName = speakerName;
	}

	public double getStartingtime() {
		return startingtime;
	}

	public void setStartingtime(double startingtime) {
		this.startingtime = startingtime;
	}

	public double getDuringtime() {
		return duringtime;
	}

	public void setDuringtime(double duringtime) {
		this.duringtime = duringtime;
	}
}
