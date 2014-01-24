#!/bin/bash

cutTool=$1 		#cutwav path
path=$2			#wavfile path

logName=./log/train
mkdir -p $logName
savePath="./gmmSet/gmmSNSSet/"
mkdir -p $savePath

tmp_file="/tmp/$.fifo"
mkfifo $tmp_file
exec 6<>$tmp_file
rm $tmp_file


thread=10
for ((i=0;i<$thread;i++));
do
	echo "\r"
done >& 6


rm *txt


for dir in `ls $path`;
do
	if [ ! -d $path/$dir ]
	then
		continue
	fi

	for file in `ls $path/$dir | grep '.wav$'`;
	do
		read -u6
		{
			fileName=$path/$dir"/"$file

			./$cutTool $fileName
			
			echo >& 6
		}&	
		
	done
	
	wait
	
	find . -name "*txt" | xargs -i awk '{if($8=="S") print $0}' {} > all_ubm.seg
	find . -name "*txt" | xargs -i rm {}

done


./trainUbm.sh LIUM_SpkDiarization-8.4.1.jar all_ubm.seg



