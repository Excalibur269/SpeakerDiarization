#!/bin/bash

chanPath=$1
wavPath=../testdata_speaker/
trainTool=./LIUM_SpkDiarization-8.4.1.jar


compCountSet="64"
fDescSet="1:1:1:1:0:0"

logName=./log/train
mkdir -p $logName
savePath="./gmmSet/gmmChaSet/"
mkdir -p $savePath

tmp_file="/tmp/$.fifo"
mkfifo $tmp_file
exec 6<>$tmp_file
rm $tmp_file

thread=15
for ((i=0;i<$thread;i++));
do
	echo "\r"
done >& 6


for fVec in $fDescSet
do
	for compCount in $compCountSet
	do
		for dir in `ls -d $chanPath*`
		do
			read -u6
			{
				dirBase=`basename $dir`
				dirBase=${dirBase%.chan.seg}
				gmmName="$savePath"$dirBase"_"$fVec"_"$compCount".chan.gmms"

echo $gmmName"  "$dirBase				

				java -Xmx4G -cp $trainTool fr.lium.spkDiarization.programs.MTrainInit --sInputMask=$dir --fInputMask=$wavPath"%s.wav" --fInputDesc="audio8kHz2sphinx,"$fVec",13,0:0:0" --kind=DIAG  --nbComp=64 --emInitMethod=split_all --emCtrl=1,5,0.05 --tOutputMask=$gmmName".init" --nbThread=8 $dirBase 2>&1 | tee -a $logName$dirBase"_"$fVec"_"$compCount".log"

				java -Xmx4G -cp $trainTool fr.lium.spkDiarization.programs.MTrainEM  --sInputMask=$dir --fInputMask=$wavPath"%s.wav"  --emCtrl=1,20,0.01 --fInputDesc="audio8kHz2sphinx,"$fVec",13,0:0:0" --tInputMask=$gmmName".init" --tOutputMask=$gmmName --nbThread=8 $dirBase 2>&1 | tee -a $logName$dirBase"_"$fVec"_"$compCount".log"

				rm $gmmName".init" 
	
				echo >& 6		
			}&
		done
	done
done

exec 6>&-
