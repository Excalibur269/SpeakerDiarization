/**
 * 
 
 * <p>
 * Diarization
 * </p>
 * 
 * @author <a href="mailto:sylvain.meignier@lium.univ-lemans.fr">Sylvain Meignier</a>
 * @version v2.0
 * 
 *          Copyright (c) 2007-2009 Universite du Maine. All Rights Reserved. Use is subject to license terms.
 * 
 *          THIS SOFTWARE IS PROVIDED BY THE "UNIVERSITE DU MAINE" AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *          DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *          USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 *          ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * 
 * 
 * 
 */

package system;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;
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
import libClusteringMethod.CLRHClustering;
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
import tools.SAdjSeg;
import tools.SFilter;

/**
 * The Class Diarization.
 */
public class Diarization_cluster extends Thread {

	/** The Constant logger. */
	private final static Logger logger = Logger
			.getLogger(Diarization_cluster.class.getName());

	/** The arguments. */
	static String[] arguments;

	/** The diarization list. */
	static ArrayList<Diarization_cluster> diarizationList;
	/** The nb treated job. */
	static int nbTreatedJob = 0;
	/** The list of cluster set. */
	static ArrayList<ClusterSet> listOfClusterSet;

	/** 判断是否是长音频 */
	static boolean needCutWav = false;
	static double clusterThrehold = 0;

	/**
	 * ------------------------------功能框架--------------------------------------
	 * ---
	 **/
	/**
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

		// parameter.getParameterSegmentationOutputFile().setMask(
		// oldMask.replace(".seg", "") + ".s.seg");
		// MainTools.writeClusterSet(parameter, clustersSeg, false);
		parameter.getParameterSegmentation().setMethod(oldMethod);
		parameter.getParameterModel().setNumberOfComponents(
				oldNumberOfComponent);
		parameter.getParameterModel().setModelKind(oldModelKind);
		parameter.getParameterSegmentationOutputFile().setMask(oldMask);

		return clustersSeg;
	}

	/**
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
	private ClusterSet getSpeechGMMs(ClusterSet clusterSetFrom,
			ClusterSet clusteSetInit, ClusterSet clusterSet,
			AudioFeatureSet featureSet, Parameter parameter) throws Exception {
		/* 加载静音模型 */
		InputStream pmsInputStream = getClass().getResourceAsStream(
				"models/allData_sns_111_64.gmms");
		// InputStream pmsInputStream = getClass().getResourceAsStream(
		// "models/taikang_1111_64.sns.gmms");

		GMMArrayList pmsVect = MainTools.readGMMContainer(pmsInputStream,
				parameter.getParameterModel());

		String oldMask = parameter.getParameterSegmentationOutputFile()
				.getMask();
		String oldDecoderPenalty = parameter.getParameterDecoder()
				.getDecoderPenaltyAsString();
		/* 设定解码参数 */
		AudioFeatureSet featureSet2 = loadFeature(featureSet, parameter,
				clusterSet, "featureSetTransformation,1:1:2:0:0:0,13,0:0:0:0");
		parameter.getParameterDecoder().setDecoderPenalty("6:5,10:5");// 10,10;6:5,10:5

		/* viterbi解码 SNS */
		ClusterSet clusterSetSNS = MDecode.make(featureSet2, clusteSetInit,
				pmsVect, parameter);

		if (parameter.getParameterDiarization().isSaveAllStep()) {
			parameter.getParameterSegmentationOutputFile().setMask(
					oldMask.replace(".seg", "") + ".sms.seg");
			MainTools.writeClusterSet(parameter, clusterSetSNS, false);
		}
		// parameter.getParameterSegmentationOutputFile().setMask(
		// oldMask.replace(".seg", "") + ".sms.seg");
		// MainTools.writeClusterSet(parameter, clusterSetSNS, false);

		parameter.getParameterSegmentationOutputFile().setMask(oldMask);
		parameter.getParameterDecoder().setDecoderPenalty(oldDecoderPenalty);

		// ???? clusterSetSNS.removeCluster("NS");
		return filter(clusterSetFrom, clusterSetSNS, parameter);
	}

	/**
	 * NS 用静音过滤 过滤原始结果 clusteSetFrom 由segmation得到 clusterSetSNS 由上面的decode得到
	 * S与NS分类
	 * 
	 * @param clusterSetFrom
	 * @param clusterSetBy
	 * @param parameter
	 * @return
	 * @throws Exception
	 */
	private ClusterSet filter(ClusterSet clusterSetFrom,
			ClusterSet clusterSetBy, Parameter parameter) throws Exception {
		int oldSegmentPadding = parameter.getParameterFilter()
				.getSegmentPadding();
		int oldSilenceMinimumLength = parameter.getParameterFilter()
				.getSilenceMinimumLength();
		int oldSpeechMinimumLength = parameter.getParameterFilter()
				.getSpeechMinimumLength();
		String oldSegmentationFilterFile = parameter
				.getParameterSegmentationFilterFile().getClusterFilterName();
		String oldMask = parameter.getParameterSegmentationOutputFile()
				.getMask();

		/* 设定过滤参数 */
		parameter.getParameterFilter().setSegmentPadding(10);
		parameter.getParameterFilter().setSilenceMinimumLength(20);
		parameter.getParameterFilter().setSpeechMinimumLength(50);

		parameter.getParameterSegmentationFilterFile().setClusterFilterName(
				"NS");

		/* 使用SFilter过滤结果 */
		ClusterSet clusterSetResult = SFilter.make(clusterSetFrom,
				clusterSetBy, parameter);
		clusterSetResult.collapse();
		if (parameter.getParameterDiarization().isSaveAllStep()) {
			parameter.getParameterSegmentationOutputFile().setMask(
					oldMask.replace(".seg", "") + ".flt.seg");
			MainTools.writeClusterSet(parameter, clusterSetResult, false);
		}

		parameter.getParameterSegmentationOutputFile().setMask(oldMask);
		parameter.getParameterFilter().setSegmentPadding(oldSegmentPadding);
		parameter.getParameterFilter().setSilenceMinimumLength(
				oldSilenceMinimumLength);
		parameter.getParameterFilter().setSpeechMinimumLength(
				oldSpeechMinimumLength);
		parameter.getParameterSegmentationFilterFile().setClusterFilterName(
				oldSegmentationFilterFile);

		return clusterSetResult;
	}

	/**
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

		/* 加载ubm */
		// InputStream ubmInputStream = getClass().getResourceAsStream(
		// "models/ubm.gmm");
		// GMMArrayList ubmVect = MainTools.readGMMContainer(ubmInputStream,
		// parameter.getParameterModel());
		// GMM ubm = ubmVect.get(0);

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

		/* 加载ubm */
		// InputStream ubmInputStream = getClass().getResourceAsStream(
		// "models/ubm.gmm");
		InputStream ubmInputStream = getClass().getResourceAsStream(
				"models/ubm_all.gmm");
		GMMArrayList ubmVect = MainTools.readGMMContainer(ubmInputStream,
				parameter.getParameterModel());
		GMM ubm = ubmVect.get(0);

		/* 设定cluster的参数 */
		parameter.getParameterClustering().setMethod("c");
		parameter.getParameterClustering().setThreshold(threshold);
		parameter.getParameterModel().setModelKind("FULL");
		parameter.getParameterModel().setNumberOfComponents(16);// 1
		// logger.finer("method:" +
		// parameter.getParameterClustering().getMethod()
		// + " thr:" + parameter.getParameterClustering().getThreshold());
		ClusterSet clustersHClust = MClust.make(featureSet, clusterSet,
				parameter, ubm);

		// parameter.getParameterSegmentationOutputFile().setMask(
		// mask.replace(".seg", "") + ".h.seg");
		// MainTools.writeClusterSet(parameter, clustersHClust, false);

		parameter.getParameterSegmentation().setMethod(oldMethod);
		parameter.getParameterModel().setNumberOfComponents(
				oldNumberOfComponent);
		parameter.getParameterModel().setModelKind(oldModelKind);
		parameter.getParameterClustering().setThreshold(oldThreshold);
		parameter.getParameterSegmentationOutputFile().setMask(mask);

		return clustersHClust;
	}

	/**
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
		int max = 0;
		int secondMax = 0;
		int maxSegment = 0;
		for(int j = 0;j < clusterSet.clusterGetSize();j++){
			if(clusterSet.getClusterVectorRepresentation().get(j).getLength() > maxSegment){
				maxSegment = clusterSet.getClusterVectorRepresentation().get(j).getLength();
				max = j;
			}
		}
		maxSegment = 0;
		for(int j = 0;j < clusterSet.clusterGetSize();j++){
			if(j != max && clusterSet.getClusterVectorRepresentation().get(j).getLength() > maxSegment){
				maxSegment = clusterSet.getClusterVectorRepresentation().get(j).getLength();
				secondMax = j;
			}
		}
		Cluster maxCluster = clusterSet.getClusterVectorRepresentation().get(max);
		Cluster secondMaxCluster = clusterSet.getClusterVectorRepresentation().get(secondMax);
		twoMainClusters.addCluster(maxCluster);
		twoMainClusters.addCluster(secondMaxCluster);
		
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
		if (parameter.getParameterDiarization().isSaveAllStep()) {
			parameter.getParameterSegmentationOutputFile().setMask(
					mask.replace(".seg", "") + ".d.seg");
			MainTools.writeClusterSet(parameter, clustersDClust, false);
		}

		// ** move the boundaries of the segment in low energy part of the
		// signal
		ClusterSet clustersAdjClust = SAdjSeg.make(featureSet, clustersDClust,
				parameter);
		if (parameter.getParameterDiarization().isSaveAllStep()) {
			parameter.getParameterSegmentationOutputFile().setMask(
					mask.replace(".seg", "") + ".adj.seg");
			MainTools.writeClusterSet(parameter, clustersAdjClust, false);
		}

		parameter.getParameterSegmentationOutputFile().setMask(mask);
		parameter.getParameterModel().setNumberOfComponents(
				oldNumberOfComponent);
		parameter.getParameterModel().setModelKind(oldModelKind);
		return clustersAdjClust;
	}

	
	public ClusterSet changeSpeakerName(ClusterSet clustersDClust){
		//将speaker的name改为0,1
		//先说话的speaker为0，后说的为1
		if(clustersDClust.clusterGetSize() == 1){
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
	public void Diarization(Parameter parameter, ClusterSet clusterSet)
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

		System.err.println("\tStarting Segmentation...\t10% completed");
		/** -----------------segmentation------------------------ **/
		int segmentationMethod = parameter.getParameterSegmentation()
				.getMethod().ordinal();
		String segmentationMethodString = ParameterSegmentation.SegmentationMethodString[segmentationMethod];

		ClusterSet clustersSegSave = segmentation(segmentationMethodString,
				"FULL", clustersSegInit, featureSet, parameter);
		System.err.println("\tFinishing Segementation\t20% completed\n\tStarting removing silence...");
		/** ----------------除掉silence部分----------------------- **/
		ClusterSet clusterNoSilence = getSpeechGMMs(clustersSegSave,
				clustersSegInit, clusterSet, featureSet, parameter);
		System.err.println("\tFinishing removing silence\t30% completed\n\tStarting linear clustering...");
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
		System.err.println("\tFinishing linear clustering\t40 %completed\n\tStarting hierarchical clustering...");
		clusterThrehold = -1.35;
		ClusterSet clustersHClust = clustering(clusterThrehold, clustersLClust, featureSet,
				parameter);
		System.err.println("\tFinishing hierarchical clustering\t90% completed\n\tStarting decoding...");
		/** -----------viterbi 解码 resegmentation ---------------- **/
		ClusterSet clustersDClust = decode(16, 100, clustersHClust, clusterNoSilence,featureSet,
				parameter);
		System.err.println("\tFinishing decoding\t100% completed\n\tOutput result file...");
		
		
		MainTools.writeClusterSet(parameter, changeSpeakerName(clustersDClust), true);
	}

	/** --------------------多线程框架------------------------- **/
	/**
	 * telephone diarization corpus.
	 * 
	 * @param parameter
	 *            the parameter
	 * @throws DiarizationException
	 *             the diarization exception
	 * @throws Exception
	 *             the exception
	 */
	public void DiarizationCorpus(Parameter parameter)
			throws DiarizationException, Exception {
		// Parameter parameter = getParameter(arguments);

		ClusterSet fullClusterSet = initialize(parameter);
		listOfClusterSet = MainTools.splitHypotesis(fullClusterSet);

		int nbThread = parameter.getParameterDiarization().getThread();

		diarizationList = new ArrayList<Diarization_cluster>(nbThread);

		for (int i = 0; i < nbThread; i++) {
			diarizationList.add(new Diarization_cluster());
			diarizationList.get(i).start();
		}

		while (isThreadAlive() == true) {
			Thread.sleep(10000);
		}

	}

	/*
	 * Gets the next cluster set.
	 * 
	 * @return the next cluster set
	 */
	public synchronized ClusterSet getNextClusterSet() {

		int index = nbTreatedJob;
		nbTreatedJob++;
		if (index < listOfClusterSet.size()) {
			return listOfClusterSet.get(index);
		}
		return null;
	}

	@Override
	public void run() {
		ClusterSet clusterSet = getNextClusterSet();
		while (clusterSet != null) {
			Parameter parameter = getParameter(arguments);
			parameter.show = clusterSet.getShowNames().first();
			// logger.finer("-------------------------------------------");
			// logger.finer("--- " + parameter.show + " ---");
			// logger.finer("-------------------------------------------");
			try {
				Diarization(parameter, clusterSet);
				System.gc();
			} catch (DiarizationException e) {
				logger.log(Level.SEVERE, "Diarization error", e);
				e.printStackTrace();
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Exception error", e);
				e.printStackTrace();
			}
			clusterSet = getNextClusterSet();
		}
	}

	/**
	 * Checks if is thread alive.
	 * 
	 * @return true, if is thread alive
	 */
	public boolean isThreadAlive() {
		for (Diarization_cluster diarization : diarizationList) {
			if (diarization.isAlive() == true) {
				return true;
			}
		}
		return false;
	}

	/** ---------------------程序框架-------------------------- **/
	/**
	 * Gets the parameter.
	 * 
	 * @param args
	 *            the args
	 * @return the parameter
	 */
	public static Parameter getParameter(String[] args) {
		Parameter parameter = new Parameter();
		parameter.getParameterInputFeature().setFeaturesDescription(
				"audio8kHz2sphinx,1:1:0:0:0:0,13,0:0:0:0");
		parameter.readParameters(args);
		return parameter;
	}

	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {
		try {
			SpkDiarizationLogger.setup();
			arguments = args;
			Parameter parameter = getParameter(args);
			if (args.length <= 1) {
				parameter.help = true;
			}
			parameter.logCmdLine(args);
			info(parameter, "Diarization");
			File inputFile = new File(parameter.getParameterInputFeature().getFeatureMask());
			String inputFileName = inputFile.getName();
			inputFileName = inputFileName.substring(0,
					inputFileName.lastIndexOf("."));
			FileInputStream fis = new FileInputStream(inputFile);
			int fileSize = fis.available();
			long wavFileLength = fileSize / 16000;
			parameter.show = inputFileName; // 将show改为文件名
			long startTime = System.currentTimeMillis(); // 获取开始时间

			if (parameter.show.isEmpty() == false) {
				Diarization_cluster diarization = new Diarization_cluster();
				if (parameter.getParameterDiarization().getSystem() == ParameterBNDiarization.SystemString[1]) {
					parameter.getParameterSegmentationSplit().
								setSegmentMaximumLength((10 * parameter.getParameterSegmentationInputFile().getRate()));
				}
				// logger.info("Diarization tuning");
				System.err.println("\tDiarization tuning\t 0% completed");
				diarization.DiarizationCorpus(parameter);

			}
			long endTime = System.currentTimeMillis(); // 获取结束时间
			DecimalFormat df = new DecimalFormat("#.##");
			double rt = (double) (endTime - startTime) / (1000 * wavFileLength);
			String segOutFilename = IOFile.getFilename(parameter.getParameterSegmentationOutputFile().getMask(), 
					inputFileName);
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
