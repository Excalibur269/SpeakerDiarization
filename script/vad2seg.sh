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
		
		./$cutTool -o 1 -s 10 $fileName
		
		fileName=${fileName%%".wav"}
		logFile=`basename $fileName`_log.txt
		
		echo ";; cluster S" > S
		echo ";; cluster N" > N

		awk -v FS='[\]\[ ]' -v MM=$fileName' 1 %s %s U U U %s\n' \
		'{
			$2=$2*100
			$4=$4*100			

			if($6=="S"){
				printf  MM,$2,$4,$6 >> "S"
			}else{
				printf  MM,$2,$4,$6 >> "N"
			}
		}' $logFile

		cat S N > $fileName.sns.seg

		rm $logFile
		rm ${logFile/%"_log.txt"/"_clip.txt"}
		rm S
		rm N

	done
done



