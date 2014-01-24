#!/bin/bash

testTool=$1
wavPath=$2/

tmp_file="/tmp/$.fifo"
mkfifo $tmp_file
exec 6<>$tmp_file
rm $tmp_file

thread=20
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
		read -u6
		{
			fileBase=`basename $file`
			fileBase=${fileBase%.wav}

			java -Xmx4G -jar $testTool --fInputMask=$file  --fInputDesc="audio8kHz2sphinx" --sOutputMask=$dir"/"$fileBase".seg"  --sOutputFormat=seg $fileBase 
								
			echo >& 6
		}&		
	done 
done

exec 6>&-
