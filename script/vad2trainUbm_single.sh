#!/bin/bash

cutTool=$1 		#cutwav path
path=$2			#wavfile path


for dir in `ls $path`;
do
echo $path/$dir
	if [ ! -d $path/$dir ]
	then
		continue
	fi

	for file in `ls $path/$dir | grep '.wav$'`;
	do
		fileName=$path/$dir"/"$file
		
		./$cutTool $fileName
		
		baseFile=`basename $fileName`
		logFile=${baseFile%%.wav}"_log.txt"
		
		awk '{if($8=="S") print $0}' $logFile > all_ubm.seg

		rm $logFile
done


./trainUbm.sh LIUM_SpkDiarization-8.4.1.jar all_ubm.seg



