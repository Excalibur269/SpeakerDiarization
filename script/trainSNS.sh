#!/bin/bash 

LOCALCLASSPATH=$1
showWav=$2"%s.wav"
showSeg=${3%%".seg"}

seg=$path"%s.seg"


fDesc="audio8kHz2sphinx,1:1:1:1:0:0,26,0:0:0"

gmmInit=$path"%s.init.gmms" 
gmm=$path"%s.gmms"

java -Xmx4G -cp $LOCALCLASSPATH fr.lium.spkDiarization.programs.MTrainInit \
--sInputMask=$seg --fInputMask=$path$showWav --fInputDesc=$fDesc --kind=DIAG \
--nbComp=64 --emInitMethod=split_all --emCtrl=1,5,0.05 --tOutputMask=$gmmInit --nbThread=8 $showSeg

java -Xmx4G -cp $LOCALCLASSPATH fr.lium.spkDiarization.programs.MTrainEM \
--sInputMask=$seg --fInputMask=$path$showWav  --emCtrl=1,20,0.01 --fInputDesc=$fDesc \
--tInputMask=$gmmInit --tOutputMask=$gmm  --nbThread=8 $showSeg

rm $gmmInit
