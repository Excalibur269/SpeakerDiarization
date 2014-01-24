#!/bin/bash 

LOCALCLASSPATH=$1
showWav="%s.wav"
showSeg=${2%%".seg"}


seg="%s.seg"

fDesc="audio8kHz2sphinx,1:1:1:1:0:0,26,0:0:0"

gmmInit="%s.init.gmm" 

gmm="%s.gmm"

java -Xmx4G -cp $LOCALCLASSPATH fr.lium.spkDiarization.programs.MTrainInit \
--sInputMask=$seg --fInputMask=$showWav --fInputDesc=$fDesc --kind=DIAG \
--nbComp=16 --emInitMethod=split_all --emCtrl=1,5,0.05 --tOutputMask=$gmmInit --nbThread=8 $showSeg

java -Xmx4G -cp $LOCALCLASSPATH fr.lium.spkDiarization.programs.MTrainEM \
--sInputMask=$seg --fInputMask=$showWav  --emCtrl=1,20,0.01 --fInputDesc=$fDesc \
--tInputMask=$gmmInit --tOutputMask=$gmm  --nbThread=8 $showSeg


rm $gmmInit
