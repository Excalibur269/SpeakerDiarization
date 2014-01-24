#!/bin/bash

testTool=$1
wavPath=$2

tmp_file="/tmp/$.fifo"
mkfifo $tmp_file
exec 6<>$tmp_file
rm $tmp_file

thread=15
for ((i=0;i<$thread;i++));
do
	echo "\r"
done >& 6

logName=./log/testEhmm/$testTool/
mkdir -p $logName

for dir in `ls -d $wavPath*` ;
do
	for file in `ls $dir/*.wav` ;
	do
			fileBase=`basename $file`
			fileBase=${fileBase%.wav}

			java -Xmx4G -jar $testTool --fInputMask=$file  --fInputDesc="audio8kHz2sphinx" --sOutputMask=$dir"/"$fileBase"_"$testTool  --sOutputFormat=seg $fileBase 2>&1 | tee  -a "$logName"$fileBase"_"$testTool".log"
								
	done 
done

