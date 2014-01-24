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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import lib.DiarizationException;
import lib.MainTools;
import libClusteringData.Cluster;
import libClusteringData.ClusterSet;
import libClusteringData.Segment;
import libFeature.AudioFeatureSet;
import libModel.gaussian.GMMArrayList;
import parameter.Parameter;
import programs.MDecode;
import programs.MEHMM;
import programs.MSegInit;
import tools.SFilter;
//import tools.SAdjSeg;
//import tools.SSplitSeg;

/**
 * The Class Telephone.
 */
public class Diarization_ehmm {

	private final static Logger logger = Logger
			.getLogger(Diarization_ehmm.class.getName());

	/**
	 * Load feature.
	 * 
	 * @param param
	 *            the param
	 * @param clusters
	 *            the clusters
	 * @param desc
	 *            the desc
	 * @return the audio feature set
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws DiarizationException
	 *             the diarization exception
	 */
	public static AudioFeatureSet loadFeature(Parameter param,
			ClusterSet clusters, String desc) throws IOException,
			DiarizationException {
		param.getParameterInputFeature().setFeaturesDescription(desc);
		return MainTools.readFeatureSet(param, clusters);
	}

	/**
	 * Load feature.
	 * 
	 * @param features
	 *            the features
	 * @param param
	 *            the param
	 * @param clusters
	 *            the clusters
	 * @param desc
	 *            the desc
	 * @return the audio feature set
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws DiarizationException
	 *             the diarization exception
	 */
	private AudioFeatureSet loadFeature(AudioFeatureSet features,
			Parameter param, ClusterSet clusters, String desc)
			throws IOException, DiarizationException {
		param.getParameterInputFeature().setFeaturesDescription(desc);
		return MainTools.readFeatureSet(param, clusters, features);
	}

	/** 使用 GMMS(SNS) 除去 Non-Speech */
	private ClusterSet getSpeechGMMs(String threshold,
			ClusterSet clusteSetInit, ClusterSet clusterSet,
			AudioFeatureSet featureSet, Parameter parameter) throws Exception {
		/* 加载静音模型 */
		InputStream pmsInputStream = getClass().getResourceAsStream(
				"models/allData_sns_111_64.gmms");
		GMMArrayList pmsVect = MainTools.readGMMContainer(pmsInputStream,
				parameter.getParameterModel());

		String oldMask = parameter.getParameterSegmentationOutputFile()
				.getMask();
		String oldDecoderPenalty = parameter.getParameterDecoder()
				.getDecoderPenaltyAsString();
		/* 设定解码参数 */
		AudioFeatureSet featureSet2 = loadFeature(featureSet, parameter,
				clusterSet, "featureSetTransformation,1:1:2:0:0,13,0:0:0:0");
		parameter.getParameterDecoder().setDecoderPenalty(threshold);

		/* viterbi解码 SNS */
		ClusterSet clusterSetSNS = MDecode.make(featureSet2, clusteSetInit,
				pmsVect, parameter);

		if (parameter.getParameterDiarization().isSaveAllStep()) {
			parameter.getParameterSegmentationOutputFile().setMask(
					oldMask.replace(".seg", "") + ".sms.seg");
			MainTools.writeClusterSet(parameter, clusterSetSNS, false);
		}

		parameter.getParameterSegmentationOutputFile().setMask(oldMask);
		parameter.getParameterDecoder().setDecoderPenalty(oldDecoderPenalty);

		clusterSetSNS.removeCluster("NS");
		return filter(clusteSetInit, clusterSetSNS, parameter);
	}

	/** NS 用静音过滤 */
	private ClusterSet filter(ClusterSet clusterSetFrom,
			ClusterSet clusterSetBy, Parameter parameter) throws Exception {
		/**
		 * 过滤原始结果 clusteSetFrom 由segmation得到 clusterSetSNS 由上面的decode得到 S与NS分类
		 */
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
		parameter.getParameterFilter().setSegmentPadding(0);
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
	 * Make media.
	 * 
	 * @param parameter
	 *            the parameter
	 * @throws DiarizationException
	 *             the diarization exception
	 * @throws Exception
	 *             the exception
	 */
	public void makeMedia(Parameter parameter) throws DiarizationException,
			Exception {
		String mask = parameter.getParameterSegmentationOutputFile().getMask();
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

		AudioFeatureSet featureSet = loadFeature(parameter, clusterSet,
				parameter.getParameterInputFeature()
						.getFeaturesDescriptorAsString());

		featureSet.setCurrentShow(parameter.show);
		int nbFeatures = featureSet.getNumberOfFeatures();

		if (parameter.getParameterDiarization().isLoadInputSegmentation() == false) {
			clusterSet.getFirstCluster().firstSegment().setLength(nbFeatures);
		}

		// ** check the quality of the MFCC, remove similar consecutive MFCC of
		// the featureSet
		ClusterSet clusteSetInit = new ClusterSet();
		MSegInit.make(featureSet, clusterSet, clusteSetInit, parameter);
		clusteSetInit.collapse();

		parameter.getParameterSegmentationOutputFile().setMask(mask + ".i.seg");
		if (parameter.getParameterDiarization().isSaveAllStep()) {
			MainTools.writeClusterSet(parameter, clusteSetInit, false);
		}

		/* 除去静音 NS,S (exitNS,exitS,loopNS,loopS) (1,2)更容易进入NS的状态，(2,1)更容易保持在NS状态 */
		ClusterSet clusteSetNoSilence = getSpeechGMMs("5,15", clusteSetInit,
				clusterSet, featureSet, parameter);
		// ClusterSet clusteSetNoSilence =
		// getSpeechGMMs("10,10",clusteSetInit,clusterSet, featureSet,
		// parameter);

		/* S1,S2(50,50) 两个人状态都有一定的保持 */
		GMMArrayList speakersList = get2Spk("50,50", clusteSetInit,
				clusteSetNoSilence, clusterSet, featureSet, parameter);

		/* 重新切分静音 */
		clusteSetNoSilence = getSpeechGMMs("20,3", clusteSetInit, clusterSet,
				featureSet, parameter);
		/* 重新解码 */
		ClusterSet clusterSet2Spk = MDecode.make(featureSet,
				clusteSetNoSilence, speakersList, parameter);// 此处没处理好decode的ubm问题

		parameter.getParameterSegmentationOutputFile().setMask(mask + ".ehmm");
		MainTools.writeClusterSet(parameter, clusterSet2Spk, true);
	}

	private GMMArrayList get2Spk(String threshold, ClusterSet clusteSetInit,
			ClusterSet clusteSetSNS, ClusterSet clusterSet,
			AudioFeatureSet featureSet, Parameter parameter) throws Exception {
		AudioFeatureSet featureSet2 = loadFeature(featureSet, parameter,
				clusterSet, "featureSetTransformation,1:1:1:0:0,13,0:0:0:0");
		InputStream ubmInputStream = getClass().getResourceAsStream(
				"models/ubm_all_111_6.gmm");
		GMMArrayList ubmVect = MainTools.readGMMContainer(ubmInputStream,
				parameter.getParameterModel());

		parameter.getParameterDecoder().setDecoderPenalty(threshold);

		GMMArrayList spkVec = new GMMArrayList();
		ClusterSet clusterSet2Spk = MEHMM.make2Spk(featureSet2, clusteSetSNS,
				ubmVect.get(0), spkVec, parameter);

		String oldMask = parameter.getParameterSegmentationOutputFile()
				.getMask();
		if (parameter.getParameterDiarization().isSaveAllStep()) {
			parameter.getParameterSegmentationOutputFile().setMask(
					oldMask.replace(".seg", "") + ".2spk.seg");
			MainTools.writeClusterSet(parameter, clusterSet2Spk, false);
		}
		parameter.getParameterSegmentationOutputFile().setMask(oldMask);

		return spkVec;
	}

	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {
		try {
			long startTime = System.currentTimeMillis(); // 获取开始时间
			Parameter parameter = new Parameter();
			parameter.getParameterInputFeature().setFeaturesDescription(
					"audio2sphinx,1:1:1:0:0:0,13,0:0:0:0");

			parameter.readParameters(args);
			File inputFile = new File(parameter.getParameterInputFeature()
					.getFeatureMask());
			String inputFileName = inputFile.getName();
			inputFileName = inputFileName.substring(0,
					inputFileName.lastIndexOf("."));
			// parameter.show = inputFileName;
			FileInputStream fis = new FileInputStream(inputFile);
			long fileSize = fis.available();
			long wavFileLength = fileSize / 16000;
			if (args.length <= 1) {
				parameter.help = true;
			}
			parameter.logCmdLine(args);
			info(parameter, "Diarization");

			if (parameter.show.isEmpty() == false) {
				Diarization_ehmm telephone = new Diarization_ehmm();
				telephone.makeMedia(parameter);
			}
			long endTime = System.currentTimeMillis(); // 获取结束时间
			System.out.println("run time： " + (endTime - startTime) / 1000
					+ "s");
			DecimalFormat df = new DecimalFormat("#.##");
			double rt = (double) (endTime - startTime) / (1000 * wavFileLength);
			System.out.println("RT = " + df.format(rt));
		} catch (DiarizationException e) {
			logger.log(Level.SEVERE, "Diarization error", e);
			e.printStackTrace();
		} catch (IOException e) {
			logger.log(Level.SEVERE, "IO error", e);
			e.printStackTrace();
		} catch (Exception e) {
			logger.log(Level.SEVERE, "error", e);
			e.printStackTrace();
		}

	}

	/**
	 * Info.
	 * 
	 * @param parameter
	 *            the parameter
	 * @param program
	 *            the program
	 * @throws IllegalArgumentException
	 *             the illegal argument exception
	 * @throws IllegalAccessException
	 *             the illegal access exception
	 * @throws InvocationTargetException
	 *             the invocation target exception
	 */
	public static void info(Parameter parameter, String program)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		if (parameter.help) {
			logger.config(parameter.getSeparator2());
			logger.config("Program name = " + program);
			logger.config(parameter.getSeparator());
			parameter.logShow();

			parameter.getParameterInputFeature().logAll(); // fInMask
			logger.config(parameter.getSeparator());
			parameter.getParameterSegmentationInputFile().logAll(); // sInMask
			// param.getParameterSegmentationInputFile().printEncodingFormat();
			parameter.getParameterSegmentationOutputFile().logAll(); // sOutMask
			logger.config(parameter.getSeparator());
			parameter.getParameterDiarization().logAll();
			logger.config(parameter.getSeparator());
		}
	}
}
