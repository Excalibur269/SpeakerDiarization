package TextGridToSeg;

public class Seg implements Comparable{
	private String filename;
	//private final String SPEAKER = "speaker";
	private final String NA = "NA";
	private String speakerName;
	private int startingtime;
	private int duringtime;
	
	public Seg(String filename, int startingtime, int duringtme, String speakerName){
		this.filename = filename;
		this.startingtime = startingtime;
		this.duringtime = duringtme;
		this.speakerName = speakerName;
	}
	
	public String toString(){
		return filename+" 1 "+startingtime+" "+duringtime+" F T U "+speakerName+"\n";
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

	public int getStartingtime() {
		return startingtime;
	}

	public void setStartingtime(int startingtime) {
		this.startingtime = startingtime;
	}

	public int getDuringtime() {
		return duringtime;
	}

	public void setDuringtime(int duringtime) {
		this.duringtime = duringtime;
	}
}
