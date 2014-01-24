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
		
		./$cutTool -o 1 -s 5 -S 20 $fileName
		
		fileName=${fileName%%".wav"}
		baseFile=`basename $fileName`_
		logFile=$baseFile"log.txt"
		
		echo ";; cluster S" > $baseFile"S"
		echo ";; cluster N" > $baseFile"N"

		awk -v FS='[\]\[ ]' -v file=$baseFile -v FOMAT=$fileName' 1 %s %s U U U %s\n' \
		'{
			$2=$2*100
			$4=$4*100			

			if($6=="S"){
				printf  FOMAT,$2,$4,$6 >> file"S"
			}else{
				printf  FOMAT,$2,$4,$6 >> file"N"
			}
		}' $logFile

		cat $baseFile"S" $baseFile"N" > $fileName.sns.seg

		rm $logFile
		rm ${logFile/%"_log.txt"/"_clip.txt"}
		rm $baseFile"S"
		rm $baseFile"N"

	done
done

find ../trainData/ -name "*.sns.seg" | xargs -i cat {} >> ../script/all_sns.seg
awk '{if($8=="S") print $1,$2,$3*100,$4*100-$3*100,$5,$6,$7,$8}' all_sns.seg > all_ubm.seg

./trainUbm.sh LIUM_SpkDiarization-8.4.1.jar all_ubm.seg



