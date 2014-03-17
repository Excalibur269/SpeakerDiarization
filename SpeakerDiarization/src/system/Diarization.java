/**
 * 
 
 * <p>
 * Speaker Diarization for Telephone conversation
 * </p>
 * 
 * @author Zhang Bihong, Yang Hongzhi
 * @version v1.5.3
 * 
 * 
 * 
 * 
 */

package system;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import lib.DiarizationException;
import lib.IOFile;
import lib.MainTools;
import lib.SpkDiarizationLogger;
import libClusteringData.Cluster;
import libClusteringData.ClusterSet;
import libClusteringData.Segment;
import libFeature.AudioFeatureSet;
import libModel.gaussian.GMM;
import libModel.gaussian.GMMArrayList;

import org.xml.sax.SAXException;

import parameter.Parameter;
import parameter.ParameterBNDiarization;
import parameter.ParameterSegmentation;
import programs.MClust;
import programs.MDecode;
import programs.MSeg;
import programs.MSegInit;
import programs.MTrainEM;
import programs.MTrainInit;
import tools.WaveFileReader;
import tools.WaveFileWriter;
import tools.WaveHeader;

/**
 * The Class Diarization.
 */
public class Diarization{

	/** The Constant logger. */
	private final static Logger logger = Logger
			.getLogger(Diarization.class.getName());

	/** The arguments. */
	String[] arguments;

	/** The nb treated job. */
	int nbTreatedJob = 0;
	/** The list of cluster set. */
	ArrayList<ClusterSet> listOfClusterSet;

	/** 判断是否是长音频 */
	boolean needCutWav = false;
	double clusterThrehold = 0;
	long wavFileLength = 0;
	
	/**
	 * ------------------------------功能框架--------------------------------------
	 * ---
	 **/
	/**
	 * called
	 * 加载 feature (mfcc) Load feature.
	 * 
	 * @param parameter
	 *            the parameter
	 * @param clusterSet
	 *            the cluster set
	 * @param descriptor
	 *            the descriptor
	 * @return the audio feature set
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws DiarizationException
	 *             the diarization exception
	 */
	public static AudioFeatureSet loadFeature(Parameter parameter,
			ClusterSet clusterSet, String descriptor) throws IOException,
			DiarizationException {
		String oldDescriptor = parameter.getParameterInputFeature()
				.getFeaturesDescriptorAsString();
		parameter.getParameterInputFeature().setFeaturesDescription(descriptor);
		AudioFeatureSet result = MainTools
				.readFeatureSet(parameter, clusterSet);
		parameter.getParameterInputFeature().setFeaturesDescription(
				oldDescriptor);
		return result;
	}

	/**
	 * called
	 * @param featureSet
	 * @param parameter
	 * @param clusterSet
	 * @param descriptor
	 * @return
	 * @throws IOException
	 * @throws DiarizationException
	 */
	private AudioFeatureSet loadFeature(AudioFeatureSet featureSet,
			Parameter parameter, ClusterSet clusterSet, String descriptor)
			throws IOException, DiarizationException {
		String oldDescriptor = parameter.getParameterInputFeature()
				.getFeaturesDescriptorAsString();
		parameter.getParameterInputFeature().setFeaturesDescription(descriptor);
		AudioFeatureSet result = MainTools.readFeatureSet(parameter,
				clusterSet, featureSet);
		parameter.getParameterInputFeature().setFeaturesDescription(
				oldDescriptor);
		return result;
	}

	/**
	 * called
	 * Initialize.
	 * 
	 * @param parameter
	 *            the parameter
	 * @return the cluster set
	 * @throws DiarizationException
	 *             the diarization exception
	 * @throws Exception
	 *             the exception
	 */
	public ClusterSet initialize(Parameter parameter)
			throws DiarizationException, Exception {
		// ** get the first diarization
		// logger.info("Initialize segmentation");
		ClusterSet clusterSet = null;
		if (parameter.getParameterDiarization().isLoadInputSegmentation()) {
			clusterSet = MainTools.readClusterSet(parameter);
		} else {
			clusterSet = new ClusterSet();
			Cluster cluster = clusterSet.createANewCluster("init");
			Segment segment = new Segment(parameter.show, 0, 1, cluster,
					parameter.getParameterSegmentationInputFile().getRate());
			cluster.addSegment(segment);
		}
		return clusterSet;
	}

	/**
	 * called
	 * Sanity check.
	 * 
	 * @param clusterSet
	 *            the cluster set
	 * @param featureSet
	 *            the feature set
	 * @param parameter
	 *            the parameter
	 * @return the cluster set
	 * @throws DiarizationException
	 *             the diarization exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws ParserConfigurationException
	 *             the parser configuration exception
	 * @throws SAXException
	 *             the sAX exception
	 * @throws TransformerException
	 *             the transformer exception
	 */
	public ClusterSet sanityCheck(ClusterSet clusterSet,
			AudioFeatureSet featureSet, Parameter parameter)
			throws DiarizationException, IOException,
			ParserConfigurationException, SAXException, TransformerException {
		String mask = parameter.getParameterSegmentationOutputFile().getMask();

		ClusterSet clustersSegInit = new ClusterSet();
		MSegInit.make(featureSet, clusterSet, clustersSegInit, parameter);
		clustersSegInit.collapse();
		if (parameter.getParameterDiarization().isSaveAllStep()) {
			parameter.getParameterSegmentationOutputFile().setMask(
					mask.replace(".seg", "") + ".i.seg");
			MainTools.writeClusterSet(parameter, clustersSegInit, false);
		}
		parameter.getParameterSegmentationOutputFile().setMask(mask);

		return clustersSegInit;
	}

	/**
	 * called
	 * change detection 分段 Segmentation.
	 * 
	 * @param method
	 *            the method
	 * @param kind
	 *            the kind
	 * @param clusterSet
	 *            the cluster set
	 * @param featureSet
	 *            the feature set
	 * @param parameter
	 *            the parameter
	 * @return the cluster set
	 * @throws Exception
	 *             the exception
	 */
	private ClusterSet segmentation(String method, String kind,
			ClusterSet clusterSet, AudioFeatureSet featureSet,
			Parameter parameter) throws Exception {
		String oldMask = parameter.getParameterSegmentationOutputFile()
				.getMask();
		String oldMethod = parameter.getParameterSegmentation()
				.getMethodAsString();
		int oldNumberOfComponent = parameter.getParameterModel()
				.getNumberOfComponents();
		String oldModelKind = parameter.getParameterModel()
				.getModelKindAsString();

		/* 设定分段参数 */
		parameter.getParameterSegmentation().setMethod(method);
		parameter.getParameterModel().setNumberOfComponents(12);
		parameter.getParameterModel().setModelKind(kind);

		/* 使用MSeg分段 */
		ClusterSet clustersSeg = new ClusterSet();
		MSeg.make(featureSet, clusterSet, clustersSeg, parameter);

//		parameter.getParameterSegmentationOutputFile().setMask(
//				oldMask.replace(".seg", "") + ".s.seg");
//		MainTools.writeClusterSet(parameter, clustersSeg, false);

		parameter.getParameterSegmentation().setMethod(oldMethod);
		parameter.getParameterModel().setNumberOfComponents(
				oldNumberOfComponent);
		parameter.getParameterModel().setModelKind(oldModelKind);
		parameter.getParameterSegmentationOutputFile().setMask(oldMask);

		return clustersSeg;
	}

	/**
	 * called
	 * 使用 GMMS(SNS) 除去 Non-Speech
	 * 
	 * @param clusterSetFrom
	 * @param clusteSetInit
	 * @param clusterSet
	 * @param featureSet
	 * @param parameter
	 * @return
	 * @throws Exception
	 */
	private ClusterSet removeSilence(ClusterSet clusterSetFrom,
			ClusterSet clusteSetInit, ClusterSet clusterSet,
			AudioFeatureSet featureSet, Parameter parameter) throws Exception {
		/* 加载静音模型 */
		InputStream snsInputStream = null;
		GMMArrayList pmsVect = null;
		if(parameter.getParameterSegmentationInputFile().getMask() != null && 
				parameter.getParameterSegmentationInputFile().getMask().contains("sns")){
			System.err.println("use VAD(cutWav) tools");
		}else if(parameter.snsModel == null || !parameter.snsModel.contains("gmm")){
			System.err.println("use default snsModel");
			snsInputStream = getClass().getResourceAsStream("models/sns.gmms");
			pmsVect = MainTools.readGMMContainer(snsInputStream,parameter.getParameterModel());			
		}else{
			System.err.println("use " + parameter.snsModel + " as snsModel");
			File file = new File(parameter.snsModel);
			snsInputStream = new FileInputStream(file);
			pmsVect = MainTools.readGMMContainer(snsInputStream,parameter.getParameterModel());						
		}
		String oldMask = parameter.getParameterSegmentationOutputFile()
				.getMask();
		String oldDecoderPenalty = parameter.getParameterDecoder()
				.getDecoderPenaltyAsString();
		/* 设定解码参数 */
		AudioFeatureSet featureSet2 = loadFeature(featureSet, parameter,
				clusterSet, "featureSetTransformation,1:1:2:0:0:0,13,0:0:0:0");
		parameter.getParameterDecoder().setDecoderPenalty("15:5,25:5");// 15:5,25:5;6:5,10:5
		//N,OS,S
		ClusterSet clusterSetSNS = null;
		if(parameter.getParameterSegmentationInputFile().getMask() != null &&
				parameter.getParameterSegmentationInputFile().getMask().contains("sns")){
			//使用VAD后的结果
			clusterSetSNS = MainTools.readClusterSet(parameter);
//			ArrayList<Segment> segments = clusterSetSNS.getSegmentVectorRepresentation();
//			Collections.sort(segments);
//			if(segments.get(segments.size()-1).getEndInSecond() - 1> wavFileLength){
//				System.err.println("Fatal Error: wave file has something wrong");
//				System.exit(-1);
//			}
		}else{
			/* viterbi解码 SNS */
			clusterSetSNS = MDecode.make(featureSet2, clusteSetInit, pmsVect, parameter);
		}
//		 parameter.getParameterSegmentationOutputFile().setMask(oldMask.replace(".seg", "") + ".sms.seg");
//		 MainTools.writeClusterSet(parameter, clusterSetSNS, false);

		parameter.getParameterSegmentationOutputFile().setMask(oldMask);
		parameter.getParameterDecoder().setDecoderPenalty(oldDecoderPenalty);

//		return filter(clusterSetFrom, clusterSetSNS, parameter);
		return filter(clusterSetFrom, clusterSetSNS);
	}
	private ClusterSet filter(ClusterSet clusterSetFrom, ClusterSet clusterSetSNS) throws DiarizationException{
		ClusterSet clusterSetResult = new ClusterSet();
		ArrayList<Segment> speechSegments = new ArrayList<Segment>();
		for(Segment segment : clusterSetSNS.getSegmentVectorRepresentation()){
			if(segment.getClusterName().equals("N") && segment.getLength() <= 50){
				segment.setCluster(clusterSetSNS.getCluster("S"));
			}
		}
		clusterSetSNS.collapse();
		for(Segment segment : clusterSetSNS.getSegmentVectorRepresentation()){
			if(segment.getClusterName().equals("S")){
				speechSegments.add(segment);
			}
		}
		Collections.sort(speechSegments);
		TreeMap<Integer, Segment> segmentMap = clusterSetFrom.getFeatureMap();
		for(Segment segment : speechSegments){
			int start = segment.getStart();
			int length = segment.getLength();
			for (int i = start; i < (start + length); i++) {
				if (segmentMap.containsKey(i)) {
					String name = segmentMap.get(i).getClusterName();
					Cluster cluster = clusterSetResult.getOrCreateANewCluster(name);
					cluster.addSegment(segmentMap.get(i));
				}
			}
		}
		clusterSetResult.collapse();
		return clusterSetResult;
	}
	/**
	 * called
	 * NS 用静音过滤 过滤原始结果 clusteSetFrom 由segmation得到 clusterSetSNS 由上面的decode得到
	 * S与NS分类
	 * 
	 * @param clusterSetFrom
	 * @param clusterSetBy
	 * @param parameter
	 * @return
	 * @throws Exception
	 */
//	private ClusterSet filter(ClusterSet clusterSetFrom,
//			ClusterSet clusterSetBy, Parameter parameter) throws Exception {
//		String oldMask = parameter.getParameterSegmentationOutputFile()
//				.getMask();
//
//		/* 设定过滤参数 */
//		parameter.getParameterFilter().setSegmentPadding(10);
//		parameter.getParameterFilter().setSilenceMinimumLength(20);
//		parameter.getParameterFilter().setSpeechMinimumLength(50);
//		if(parameter.getParameterSegmentationInputFile().getMask() != null &&
//				parameter.getParameterSegmentationInputFile().getMask().contains("sns")){
//			parameter.getParameterSegmentationFilterFile().setClusterFilterName("N");//silence
//		}else{
//			parameter.getParameterSegmentationFilterFile().setClusterFilterName("NS");//silence
//		}
//
//		/* 使用SFilter过滤结果 */
//		ClusterSet clusterSetResult = SFilter.make(clusterSetFrom,
//				clusterSetBy, parameter);
//		clusterSetResult.collapse();
//		
////		MainTools.writeClusterSet(parameter, clusterSetResult, false);
//		
//
//		parameter.getParameterSegmentationOutputFile().setMask(oldMask);
//		return clusterSetResult;
//	}

	/**
	 * called
	 * 线性cluster但注意其中有个问题 BICLClustering中使用每段做一个cluster
	 * 最后结果是，clusterAndGMM中同一个cluster共有多个，而merger时出错 Clustering linear.
	 * 
	 * @param threshold
	 *            the threshold 2
	 * @param clusterSet
	 *            the cluster set
	 * @param featureSet
	 *            the feature set
	 * @param parameter
	 *            the parameter
	 * @return the cluster set
	 * @throws Exception
	 *             the exception
	 */
	private ClusterSet clusteringLinear(double threshold,
			ClusterSet clusterSet, AudioFeatureSet featureSet,
			Parameter parameter) throws Exception {
		String mask = parameter.getParameterSegmentationOutputFile().getMask();
		String oldMethod = parameter.getParameterClustering()
				.getMethodAsString();
		double oldThreshold = parameter.getParameterClustering().getThreshold();
		String oldModelKind = parameter.getParameterModel()
				.getModelKindAsString();
		int oldNumberOfComponent = parameter.getParameterModel()
				.getNumberOfComponents();

		/* 设定cluster参数 */
		parameter.getParameterModel().setModelKind("DIAG");
		parameter.getParameterModel().setNumberOfComponents(1);
		parameter.getParameterClustering().setMethod("l");
		parameter.getParameterClustering().setThreshold(threshold);

		ClusterSet clustersLClust = MClust.make(featureSet, clusterSet,
				parameter, null);
		if (parameter.getParameterDiarization().isSaveAllStep()) {
			parameter.getParameterSegmentationOutputFile().setMask(
					mask.replace(".seg", "") + ".l.seg");
			MainTools.writeClusterSet(parameter, clustersLClust, false);
		}
		// parameter.getParameterSegmentationOutputFile().setMask(
		// mask.replace(".seg", "") + ".l.seg");
		// MainTools.writeClusterSet(parameter, clustersLClust, false);
		parameter.getParameterSegmentation().setMethod(oldMethod);
		parameter.getParameterModel().setNumberOfComponents(
				oldNumberOfComponent);
		parameter.getParameterModel().setModelKind(oldModelKind);
		parameter.getParameterClustering().setThreshold(oldThreshold);
		parameter.getParameterSegmentationOutputFile().setMask(mask);
		return clustersLClust;
	}

	/**
	 * called
	 * 层级聚类 Clustering.
	 * 
	 * @param threshold
	 *            the threshold
	 * @param clusterSet
	 *            the cluster set
	 * @param featureSet
	 *            the feature set
	 * @param parameter
	 *            the parameter
	 * @return the cluster set
	 * @throws Exception
	 *             the exception
	 */
	private ClusterSet clustering(double threshold, ClusterSet clusterSet,
			AudioFeatureSet featureSet, Parameter parameter) throws Exception {
		String mask = parameter.getParameterSegmentationOutputFile().getMask();
		String oldMethod = parameter.getParameterClustering()
				.getMethodAsString();
		double oldThreshold = parameter.getParameterClustering().getThreshold();
		String oldModelKind = parameter.getParameterModel()
				.getModelKindAsString();
		int oldNumberOfComponent = parameter.getParameterModel()
				.getNumberOfComponents();

		System.out.println("\tclustering threshold is "+threshold);
		/* 加载ubm */
		InputStream ubmInputStream = null;
		GMMArrayList ubmVect = null;
		
		if(parameter.sampleRate.equals("6")){
			System.err.println("use UBM for 6KHz");
			ubmInputStream = getClass().getResourceAsStream("models/ubm_6k_100h.gmm");
			ubmVect = MainTools.readGMMContainer(ubmInputStream,parameter.getParameterModel());			
		}else {
			System.err.println("use UBM for 8KHz");
			ubmInputStream = getClass().getResourceAsStream("models/ubm_8k_100h.gmm");
			ubmVect = MainTools.readGMMContainer(ubmInputStream,parameter.getParameterModel());						
		}
		GMM ubm = ubmVect.get(0);

		/* 设定cluster的参数 */
		parameter.getParameterClustering().setMethod("c");
//		parameter.getParameterClustering().setMethod("ce");
		parameter.getParameterClustering().setThreshold(threshold);
		parameter.getParameterModel().setModelKind("FULL");
		parameter.getParameterModel().setNumberOfComponents(16);// 1

		ClusterSet clustersHClust = MClust.make(featureSet, clusterSet,
				parameter, ubm);
//		 parameter.getParameterSegmentationOutputFile().setMask(parameter.getParameterSegmentationOutputFile()
//					.getMask().replace(".seg", "") + ".hc.seg");
//		 MainTools.writeClusterSet(parameter, clustersHClust, false);
		parameter.getParameterSegmentation().setMethod(oldMethod);
		parameter.getParameterModel().setNumberOfComponents(
				oldNumberOfComponent);
		parameter.getParameterModel().setModelKind(oldModelKind);
		parameter.getParameterClustering().setThreshold(oldThreshold);
		parameter.getParameterSegmentationOutputFile().setMask(mask);

		return clustersHClust;
	}

	/**
	 * called
	 * viterbi解码 Decode.
	 * 
	 * @param nbComp
	 *            the nb comp
	 * @param threshold
	 *            the threshold
	 * @param clusterSet
	 *            the cluster set
	 * @param featureSet
	 *            the feature set
	 * @param parameter
	 *            the parameter
	 * @return the cluster set
	 * @throws Exception
	 *             the exception
	 */
	private ClusterSet decode(int nbComp, double threshold,
			ClusterSet clusterSet, ClusterSet decodeClusterSet, AudioFeatureSet featureSet,
			Parameter parameter) throws Exception {
		String mask = parameter.getParameterSegmentationOutputFile().getMask();
		String oldModelKind = parameter.getParameterModel()
				.getModelKindAsString();
		int oldNumberOfComponent = parameter.getParameterModel()
				.getNumberOfComponents();

		
		ClusterSet twoMainClusters = new ClusterSet();
		if(clusterSet.getClusterVectorRepresentation().size() > 0){
			int max = 0;
			int secondMax = 0;
			int maxSegment = 0;
			for(int j = 0;j < clusterSet.clusterGetSize();j++){
				if(clusterSet.getClusterVectorRepresentation().get(j).getLength() > maxSegment){
					maxSegment = clusterSet.getClusterVectorRepresentation().get(j).getLength();
					max = j;
				}
			}
			Cluster maxCluster = clusterSet.getClusterVectorRepresentation().get(max);
			twoMainClusters.addCluster(maxCluster);
			if(clusterSet.getClusterVectorRepresentation().size() > 1){
				maxSegment = 0;
				for(int j = 0;j < clusterSet.clusterGetSize();j++){
					if(j != max && clusterSet.getClusterVectorRepresentation().get(j).getLength() > maxSegment){
						maxSegment = clusterSet.getClusterVectorRepresentation().get(j).getLength();
						secondMax = j;
					}
				}
				Cluster secondMaxCluster = clusterSet.getClusterVectorRepresentation().get(secondMax);
				twoMainClusters.addCluster(secondMaxCluster);
			}
		}
		
		// ** Train GMM for each cluster.
		// ** GMM is a 16 component gaussian with diagonal covariance matrix
		// ** one GMM = one speaker = one cluster
		// ** initialization of the GMMs :
		// ** - same global covariance for each gaussian,
		// ** - 1/16 for the weight,
		// ** - means are initialized with the mean of 10 successive vectors
		// taken
		parameter.getParameterModel().setModelKind("DIAG");
		parameter.getParameterModel().setNumberOfComponents(nbComp);
//		GMMArrayList gmmInitVect = new GMMArrayList(clusterSet.clusterGetSize());
		GMMArrayList gmmInitVect = new GMMArrayList(2);
		MTrainInit.make(featureSet, twoMainClusters, gmmInitVect, parameter);
		// ** EM training of the initialized GMM
		GMMArrayList gmmVect = new GMMArrayList(2);//两个主要的cluster
		MTrainEM.make(featureSet, twoMainClusters, gmmInitVect, gmmVect, parameter);

		// ** set the penalty to move from the state i to the state j, penalty
		// to move from i to i is equal to 0
		parameter.getParameterDecoder().setDecoderPenalty(
				String.valueOf(threshold));
		// ** make Viterbi decoding using the 8-GMM set
		// ** one state = one GMM = one speaker = one cluster
		ClusterSet clustersDClust = MDecode.make(featureSet, decodeClusterSet,
				gmmVect, parameter);
		
		parameter.getParameterSegmentationOutputFile().setMask(mask);
		parameter.getParameterModel().setNumberOfComponents(
				oldNumberOfComponent);
		parameter.getParameterModel().setModelKind(oldModelKind);
		return clustersDClust;
	}

	/**
	 * called
	 * @param clustersDClust
	 * @return
	 */
	public ClusterSet changeSpeakerName(ClusterSet clustersDClust){
		//将speaker的name改为0,1
		//先说话的speaker为0，后说的为1
		if(clustersDClust.clusterGetSize() == 1){
			clustersDClust.getClusterVectorRepresentation().get(0).setName("0");
			return clustersDClust;
		}
		if(clustersDClust.getClusterVectorRepresentation().get(0).firstSegment().getStart() < 
				clustersDClust.getClusterVectorRepresentation().get(1).firstSegment().getStart()){
			clustersDClust.getClusterVectorRepresentation().get(0).setName("0");
			clustersDClust.getClusterVectorRepresentation().get(1).setName("1");
		}else{
			clustersDClust.getClusterVectorRepresentation().get(1).setName("0");
			clustersDClust.getClusterVectorRepresentation().get(0).setName("1");
		}
		return clustersDClust;
	}
	
	/**
	 * called
	 * @param clustersDClust
	 * @return
	 */
	private boolean thresholdIsLarge(ClusterSet clustersDClust) {
		int firstSpeakerTime = clustersDClust.getClusterVectorRepresentation().get(0).getLength();
		int secondSpeakerTime = clustersDClust.getClusterVectorRepresentation().get(1).getLength();
		int allLength = clustersDClust.getLength();
		int maxContinuent = 0;
		int count = 0;
		String speakerName = "";
		boolean someoneContinue = false;
		ArrayList<Segment> segments = new ArrayList<Segment>();
		
		clustersDClust.collapse(100);
		
		for(Segment segment : clustersDClust.getSegmentVectorRepresentation()){
			segments.add(segment);
		}
		Collections.sort(segments);
		int segmentsLength = segments.size();
		for(Segment segment : segments){
			if(!segment.getClusterName().equals(speakerName)){
				if(count > maxContinuent){
					maxContinuent = count;
				}
				count = 1;
				speakerName = segment.getClusterName();
			}else{
				count++;
			}
		}
		if(count > maxContinuent){
			maxContinuent = count;
		}
		if(segmentsLength <= 12){
			if(maxContinuent >= 5){
				someoneContinue = true;
			}
		}else{
			if(maxContinuent >= 7){
				someoneContinue = true;
			}
		}
		if(!someoneContinue){
			if(clustersDClust.getLength() > 100*30){
				//0.9;0.8
				if((firstSpeakerTime > 0.85 * allLength || secondSpeakerTime > 0.85 * allLength)){
					return true;
				}
			}else{ 
				//0.75;0.7
				if((firstSpeakerTime > 0.75 * allLength || secondSpeakerTime > 0.75 * allLength)){
					return true;
				}
			}
		}else{
			return true;
		}
		return false;
	}
//	/**
//	 * 在segment边界加入静音，为ASR用
//	 * @param clusterSet
//	 * @return
//	 */
//	private ClusterSet addSilence(Parameter parameter, ClusterSet clusterSet){
//		ClusterSet resultClusterSet = clusterSet;
//		int addSilenceLength = (int)(Double.parseDouble(parameter.addSilence)*100);
//		if(addSilenceLength == 0){
//			return resultClusterSet;
//		}
//		ArrayList<Segment> segments = resultClusterSet.getSegmentVectorRepresentation();
//		Collections.sort(segments);
//		for(int i = 0;i < segments.size()-1;i++){
//			if(i == 0){
//				if(segments.get(i).getStartInSecond() != 0){
//					if(segments.get(i).getStartInSecond() <= addSilenceLength/2){
//						segments.get(i).setLength(segments.get(i).getLength()+segments.get(i).getStart());
//						segments.get(i).setStart(0);
//					}else{
//						segments.get(i).setLength(segments.get(i).getLength() + addSilenceLength/2);
//						segments.get(i).setStart(segments.get(i).getStart() - addSilenceLength/2);
//					}
//				}
//			}else if(i == segments.size()-1){
//				if(segments.get(i).getEndInSecond() != wavFileLength){
//					if(wavFileLength - segments.get(i).getEndInSecond() <= (100*addSilenceLength/2)){
//						segments.get(i).setLength((int)wavFileLength*100 - segments.get(i).getStart());
//					}else{
//						segments.get(i).setLength(segments.get(i).getLength() + addSilenceLength/2);
//					}
//				}
//			}else{
//				Segment previous = segments.get(i-1);
//				Segment current = segments.get(i);
//				if(current.getStart() - (previous.getStart()+previous.getLength()) == 0){
//					continue;
//				}
//				if(current.getStart() - (previous.getStart()+previous.getLength()) < addSilenceLength){
//					int gap = current.getStart() - (previous.getStart()+previous.getLength());
//					current.setStart(current.getStart()-gap/2);
//					current.setLength(current.getLength() + gap/2);
//					previous.setLength(previous.getLength() + gap/2);
//				}else{
//					current.setStart(current.getStart()-addSilenceLength/2);
//					current.setLength(current.getLength() + addSilenceLength/2);
//					previous.setLength(previous.getLength() + addSilenceLength/2);
//				}
//				segments.set(i-1, previous);
//				segments.set(i, current);
//			}
//		}
//		return resultClusterSet;
//	}
	/**
	 * called
	 * 程序主体函数 Diarization.
	 * 
	 * @param parameter
	 *            the parameter
	 * @param clusterSet
	 *            the cluster set
	 * @throws DiarizationException
	 *             the diarization exception
	 * @throws Exception
	 *             the exception
	 */
	public void diarization(Parameter parameter, ClusterSet clusterSet)
			throws DiarizationException, Exception {
		AudioFeatureSet featureSet = null;
		ClusterSet clustersSegInit = null;
		String featureDesc = parameter.getParameterInputFeature()
				.getFeaturesDescriptorAsString();
		if (parameter.getParameterDiarization().isLoadInputSegmentation() == false) {
			featureSet = loadFeature(parameter, clusterSet, featureDesc);
			featureSet.setCurrentShow(parameter.show);
			int nbFeatures = featureSet.getNumberOfFeatures();
			clusterSet.getFirstCluster().firstSegment().setLength(nbFeatures);
			clustersSegInit = sanityCheck(clusterSet, featureSet, parameter);
		} else {
			featureSet = loadFeature(parameter, clusterSet, featureDesc);
			featureSet.setCurrentShow(parameter.show);
			clustersSegInit = sanityCheck(clusterSet, featureSet, parameter);
			featureSet = loadFeature(parameter, clustersSegInit, featureDesc);
			featureSet.setCurrentShow(parameter.show);
		}
		Date date = new Date();
		System.err.println(date + "\tStarting Segmentation...\t10% completed");
		
		/** -----------------segmentation------------------------ **/
		int segmentationMethod = parameter.getParameterSegmentation()
				.getMethod().ordinal();
		String segmentationMethodString = ParameterSegmentation.SegmentationMethodString[segmentationMethod];

		ClusterSet clustersSegSave = segmentation(segmentationMethodString,
				"FULL", clustersSegInit, featureSet, parameter);
		date = new Date();
		System.err.println(date+"\tFinishing Segementation\t20% completed\n\tStarting removing silence...");
		
		/** ----------------除掉silence部分----------------------- **/
		ClusterSet clusterNoSilence = removeSilence(clustersSegSave,
				clustersSegInit, clusterSet, featureSet, parameter);
		
		date = new Date();
		if(clusterNoSilence.getClusterVectorRepresentation().size() == 0){
			System.err.println(date+"\t this is a non-speech audio, system will exit");
			Cluster cluster = new Cluster("-1");
			Segment segment  = new Segment(" ", 0, 0, cluster, 100);
			cluster.addSegment(segment);
			ClusterSet clusterSet2 = new ClusterSet();
			clusterSet2.addCluster(cluster);
			MainTools.writeClusterSet(parameter, clusterSet2, true);
			return;
		}else
			System.err.println(date+"\tFinishing removing silence\t30% completed\n\tStarting linear clustering...");
		
		int speechlength = 180;
		if(clusterNoSilence.getLength() > speechlength*100){
			needCutWav = true;
		}
		ClusterSet extract3MinClusterSet = new ClusterSet();
		
		if(needCutWav){
			int currentLength = 0;
			ArrayList<Cluster> sortByTimeList = clusterNoSilence.getClusterVectorRepresentation();
			Collections.sort(sortByTimeList);
			ClusterSet tempClusterSet = new ClusterSet();
			for(Cluster cluster : sortByTimeList){
				tempClusterSet.addCluster(cluster);
				currentLength += cluster.getLength();
				if(currentLength >= speechlength * 100){
					break;
				}
			}
			extract3MinClusterSet = tempClusterSet;
		}else{
			extract3MinClusterSet = clusterNoSilence;
		}

		/** -----------对初始 segmentation cluster---------------- **/
		ClusterSet clustersLClust = clusteringLinear(0.0, extract3MinClusterSet,
				featureSet, parameter);
		date = new Date();
		System.err.println(date+"\tFinishing linear clustering\t40 %completed\n\tStarting hierarchical clustering...");
		//1.5.3 -2.8
		//1.5.4 -1.4
		//1.5.5 -2
		//1.6 -1.35 -2.8 -3.6 -4
		clusterThrehold = -1.35;
		ClusterSet clustersHClust = clustering(clusterThrehold, clustersLClust, featureSet,
				parameter);
		date = new Date();
		System.err.println(date+"\tFinishing hierarchical clustering\t90% completed\n\tStarting decoding...");
		/** -----------viterbi 解码 resegmentation ---------------- **/
		ClusterSet clustersDClust = decode(16, 45, clustersHClust, clusterNoSilence,featureSet,
				parameter);
		date = new Date();
		System.err.println(date+"\tFinishing decoding\t100% completed\n\tOutput result file...");

//		if(clustersDClust.getClusterVectorRepresentation().size() == 1 ||
//				thresholdIsLarge(clustersDClust)){
//			clusterThrehold = -2.8;
//			clustersHClust = clustering(clusterThrehold, clustersLClust, featureSet,
//					parameter);
//			/** -----------viterbi 解码 resegmentation ---------------- **/
//			clustersDClust = decode(16, 45, clustersHClust, clusterNoSilence,featureSet,
//					parameter);
//			
//			if(clustersDClust.getClusterVectorRepresentation().size() == 1 ||
//					thresholdIsLarge(clustersDClust)){
//				clusterThrehold = -3.6;
//				clustersHClust = clustering(clusterThrehold, clustersLClust, featureSet,
//						parameter);
//				/** -----------viterbi 解码 resegmentation ---------------- **/
//				clustersDClust = decode(16, 45, clustersHClust, clusterNoSilence,featureSet,
//						parameter);
//				if(clustersDClust.getClusterVectorRepresentation().size() == 1 ||
//						thresholdIsLarge(clustersDClust)){
//					clusterThrehold = -4;
//					clustersHClust = clustering(clusterThrehold, clustersLClust, featureSet,
//							parameter);
//					/** -----------viterbi 解码 resegmentation ---------------- **/
//					clustersDClust = decode(16, 45, clustersHClust, clusterNoSilence,featureSet,
//							parameter);
//				}
//			}
//		}
		clustersDClust = changeSpeakerName(clustersDClust);
		clustersDClust = removeLittleSegment(clustersDClust);
		clustersDClust.collapse(100);
		MainTools.writeClusterSet(parameter, clustersDClust, true);
	}
	

	private ClusterSet removeLittleSegment(ClusterSet clustersDClust) {
		ArrayList<Segment> segments = clustersDClust.getSegmentVectorRepresentation();
		Iterator<Segment> iterator = segments.iterator();
		while(iterator.hasNext()){
			Segment segment = iterator.next();
			if(segment.getLength() < 30){
				Cluster cluster = segment.getCluster();
				cluster.removeSegment(segment);
				iterator.remove();
			}
		}
		return clustersDClust;
	}

	/**
	 * called
	 * @param src
	 * @param target
	 * @throws Exception
	 */
	private static void convertAudioFiles(String src, String target) throws Exception {
//		FileInputStream fis = new FileInputStream(src);
//		FileOutputStream fos = new FileOutputStream(target);
//
//		// 计算长度
//		byte[] buf = new byte[1024 * 4];
//		int size = fis.read(buf);
//		int PCMSize = 0;
//		while (size != -1) {
//			PCMSize += size;
//			size = fis.read(buf);
//		}
//		fis.close();
//
//		// 填入参数，比特率等等。这里用的是16位单声道 8000 hz
//		WaveHeader header = new WaveHeader();
//		// 长度字段 = 内容的大小（PCMSize) + 头部字段的大小(不包括前面4字节的标识符RIFF以及fileLength本身的4字节)
//		header.fileLength = PCMSize + (44 - 8);
//		header.FmtHdrLeth = 16;
//		header.BitsPerSample = 16;
//		header.Channels = 1;
//		header.FormatTag = 0x0001;
//		header.SamplesPerSec = 8000;
//		header.BlockAlign = (short) (header.Channels * header.BitsPerSample / 8);
//		header.AvgBytesPerSec = header.BlockAlign * header.SamplesPerSec;
//		header.DataHdrLeth = PCMSize;
//
//		byte[] h = header.getHeader();
//
//		assert h.length == 44; // WAV标准，头部应该是44字节
//		// write header
//		fos.write(h, 0, h.length);
//		// write data stream
//		fis = new FileInputStream(src);
//		size = fis.read(buf);
//		while (size != -1) {
//			fos.write(buf, 0, size);
//			size = fis.read(buf);
//		}
//		fis.close();
//		fos.close();
		WaveFileReader wavFile = new WaveFileReader(src, false);
		WaveFileWriter newWaveFile = new WaveFileWriter();
		Random random = new Random(1);
		float[] samples = new float[wavFile.getDataLen()];
		for(int i = 0;i < samples.length;i++){
			samples[i] = (float)wavFile.getData()[0][i] / 32767.0f;
			float randomNum =  random.nextFloat();//返回[0-1]之间的float数值。
        	samples[i] += (Math.abs(randomNum) * 2.0f - 1.0f) * 0.001f;
		}
		File outWav = new File(target);
		newWaveFile.write(samples, outWav);
	}
	/**
	 * 加入扰动
	 * @param featureData
	 * @return
	 * @throws IOException 
	 */
	public static void addDither(String src, String target) throws IOException{
		WaveFileReader wavFile = new WaveFileReader(src, true);
		WaveFileWriter newWaveFile = new WaveFileWriter();
		Random random = new Random(1);
		float[] samples = new float[wavFile.getDataLen()];
		for(int i = 0;i < samples.length;i++){
			samples[i] = (float)wavFile.getData()[0][i] / 32767.0f;
			float randomNum =  random.nextFloat();//返回[0-1]之间的float数值。
        	samples[i] += (Math.abs(randomNum) * 2.0f - 1.0f) * 0.001f;
		}
		File outWav = new File(target);
		newWaveFile.write(samples, outWav);
	}
	
	/**
	 * called
	 * Gets the parameter.
	 * 
	 * @param args
	 *            the args
	 * @return the parameter
	 */
	public static Parameter getParameter(String[] args) {
		Parameter parameter = new Parameter();
		parameter.readParameters(args);
		if(parameter.sampleRate.equals("6")){
			parameter.getParameterInputFeature().setFeaturesDescription(
					"audio6kHz2sphinx,1:1:0:0:0:0,13,0:0:0:0");
		}else{
			parameter.getParameterInputFeature().setFeaturesDescription(
					"audio8kHz2sphinx,1:1:0:0:0:0,13,0:0:0:0");
		}
		return parameter;
	}

	/**
	 * 
	 * @param inputFile
	 * @return
	 * @throws IOException
	 */
	private static boolean isWaveFormat(File inputFile) throws IOException{
		FileInputStream fis = new FileInputStream(inputFile);
		BufferedInputStream bis = new BufferedInputStream(fis);
		byte[] buf = new byte[4];
		bis.read(buf);
		String chunkdescriptor = new String(buf);
		if(chunkdescriptor.contains("RIFF")){
			return false;
		}
		return true;
	}
	/**
	 * 供外部调用
	 * @param args
	 * @return
	 */
	public static int make(String[] args){
		try {
			SpkDiarizationLogger.setup();
//			arguments = args;
			Diarization app = new Diarization();
			Parameter parameter = getParameter(args);

			if(parameter.outlogFile != null && !parameter.outlogFile.trim().equals("")){
				System.setErr(new PrintStream(new FileOutputStream(parameter.outlogFile, true)));
				System.setOut(new PrintStream(new FileOutputStream(parameter.outlogFile, true)));
			}
			
			parameter.logCmdLine(args);
			File inputFile = new File(parameter.getParameterInputFeature().getFeatureMask());
			String inputFileName = inputFile.getName();
			if(inputFileName.contains("wav")){
				inputFileName = inputFileName.substring(0,
						inputFileName.lastIndexOf("."));
			}
			FileInputStream fis = new FileInputStream(inputFile);
			File convertWav = new File(inputFile.getParent()+"/"+inputFileName+"convertWav");
			if(isWaveFormat(inputFile)){
				System.out.println("pcm");
				convertAudioFiles(inputFile.getAbsolutePath(),convertWav.getAbsolutePath());
			}else{
				System.out.println("wav");
				addDither(inputFile.getAbsolutePath(),convertWav.getAbsolutePath());
			}
			parameter.getParameterInputFeature().setFeatureMask(convertWav.getAbsolutePath());
			int fileSize = fis.available();
			app.wavFileLength = fileSize / 16000;
			parameter.show = inputFileName; // 将show改为文件名
			long startTime = System.currentTimeMillis(); // 获取开始时间

			if (parameter.show.isEmpty() == false) {
				if (parameter.getParameterDiarization().getSystem() == ParameterBNDiarization.SystemString[1]) {
					parameter.getParameterSegmentationSplit().
								setSegmentMaximumLength((10 * parameter.getParameterSegmentationInputFile().getRate()));
				}
				Date date = new Date();
				System.err.println(date+"\tDiarization tuning\t 0% completed");
				
				ClusterSet fullClusterSet = app.initialize(parameter);
				parameter.show = fullClusterSet.getShowNames().first();
				
				app.diarization(parameter, fullClusterSet);
			}
			long endTime = System.currentTimeMillis(); // 获取结束时间
			DecimalFormat df = new DecimalFormat("#.##");
			double rt = (double) (endTime - startTime) / (1000 * app.wavFileLength);
			String segOutFilename = IOFile.getFilename(parameter.getParameterSegmentationOutputFile().getMask(), 
					parameter.show);
			File segFile = new File(segOutFilename);
			String content = ";; " + df.format(rt) + "\n";
			InputStream input = new FileInputStream(segFile.getAbsoluteFile());
			InputStreamReader reader = new InputStreamReader(input, "UTF-8");
			BufferedReader br = new BufferedReader(reader);
			String temp;
			while((temp = br.readLine()) != null){
				content += temp + "\n";
			}
			FileWriter fileWriter = new FileWriter(segFile);
			fileWriter.write(content);
			fileWriter.flush();
			if(convertWav != null){
				boolean flag = convertWav.delete();
			}
			return 1;
		} catch (DiarizationException e) {
			logger.log(Level.SEVERE, "Diarization error", e);
			e.printStackTrace();
			return -2;
		} catch (IOException e) {
			logger.log(Level.SEVERE, "IOExecption error", e);
			e.printStackTrace();
			return -3;
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Execption error", e);
			e.printStackTrace();
			return -4;
		}
	}
	/**
	 * called
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {
		try {
			SpkDiarizationLogger.setup();
			Diarization app = new Diarization();
			app.arguments = args;
			Parameter parameter = getParameter(args);
			if (args.length <= 1) {
				parameter.help = true;
			}
			
			if(parameter.outlogFile != null && !parameter.outlogFile.equals("")){
				System.setErr(new PrintStream(new FileOutputStream(parameter.outlogFile, true)));
				System.setOut(new PrintStream(new FileOutputStream(parameter.outlogFile, true)));
			}
			
			parameter.logCmdLine(args);
			info(parameter, "Diarization");
			File inputFile = new File(parameter.getParameterInputFeature().getFeatureMask());
			String inputFileName = inputFile.getName();
			if(inputFileName.contains("wav")){
				inputFileName = inputFileName.substring(0,
						inputFileName.lastIndexOf("."));
			}
			FileInputStream fis = new FileInputStream(inputFile);
			File convertWav = new File(inputFile.getParent()+"/"+inputFileName+"convertWav");
			if(isWaveFormat(inputFile)){
				System.out.println("pcm");
				convertAudioFiles(inputFile.getAbsolutePath(),convertWav.getAbsolutePath());
			}else{
				System.out.println("wav");
				addDither(inputFile.getAbsolutePath(),convertWav.getAbsolutePath());
			}
			parameter.getParameterInputFeature().setFeatureMask(convertWav.getAbsolutePath());
			int fileSize = fis.available();
			app.wavFileLength = fileSize / 16000;
			parameter.show = inputFileName; // 将show改为文件名
			long startTime = System.currentTimeMillis(); // 获取开始时间

			
			if (parameter.show.isEmpty() == false) {
				if (parameter.getParameterDiarization().getSystem() == ParameterBNDiarization.SystemString[1]) {
					parameter.getParameterSegmentationSplit().
								setSegmentMaximumLength((10 * parameter.getParameterSegmentationInputFile().getRate()));
				}
//				logger.info("Diarization tuning");
				Date date = new Date();
				System.err.println(date+"\tDiarization tuning\t 0% completed");
				ClusterSet fullClusterSet = app.initialize(parameter);
				parameter.show = fullClusterSet.getShowNames().first();
				
				app.diarization(parameter, fullClusterSet);
			}
			long endTime = System.currentTimeMillis(); // 获取结束时间
			DecimalFormat df = new DecimalFormat("#.##");
			double rt = (double) (endTime - startTime) / (1000 * app.wavFileLength);
			String segOutFilename = IOFile.getFilename(parameter.getParameterSegmentationOutputFile().getMask(), 
					parameter.show);
			if(!segOutFilename.contains("xml")){
				File segFile = new File(segOutFilename);
				String content = ";; " + df.format(rt) + "\n";
				InputStream input = new FileInputStream(segFile.getAbsoluteFile());
				InputStreamReader reader = new InputStreamReader(input, "UTF-8");
				BufferedReader br = new BufferedReader(reader);
				String temp;
				while((temp = br.readLine()) != null){
					content += temp + "\n";
				}
				FileWriter fileWriter = new FileWriter(segFile);
				fileWriter.write(content);
				fileWriter.flush();
				if(convertWav != null){
					boolean flag = convertWav.delete();
				}
			}
		} catch (DiarizationException e) {
			logger.log(Level.SEVERE, "Diarization error", e);
			e.printStackTrace();
			
		} catch (IOException e) {
			logger.log(Level.SEVERE, "IOExecption error", e);
			e.printStackTrace();
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Execption error", e);
			e.printStackTrace();
		} 

	}

	/**
	 * 运行信息 Info.
	 * 
	 * @param parameter
	 *            the parameter
	 * @param programName
	 *            the program name
	 * @throws IllegalArgumentException
	 *             the illegal argument exception
	 * @throws IllegalAccessException
	 *             the illegal access exception
	 * @throws InvocationTargetException
	 *             the invocation target exception
	 */
	public static void info(Parameter parameter, String programName)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		if (parameter.help) {
			logger.config(parameter.getSeparator2());
			logger.config("name = " + programName);
			logger.config(parameter.getSeparator());
			parameter.logShow();

			parameter.getParameterInputFeature().logAll(); // fInMask
			logger.config(parameter.getSeparator());
			parameter.getParameterSegmentationInputFile().logAll(); // sInMask
			parameter.getParameterSegmentationInputFile2().logAll(); // sInMask
			parameter.getParameterSegmentationOutputFile().logAll(); // sOutMask
			logger.config(parameter.getSeparator());
			parameter.getParameterDiarization().logAll();
			logger.config(parameter.getSeparator());
		}
	}
}
