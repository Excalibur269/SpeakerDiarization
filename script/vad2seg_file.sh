#!/bin/bash

cutTool=$1		#cutwav path
file=$2			#wavfile path

./$cutTool $file


fileName=${2%%".wav"}

logFile=`basename $fileName`_log.txt

echo ";; cluster S" > S
echo ";; cluster N" > N

awk -v FS='[\]\[ ]' -v MM=$fileName' 1 %s %s U U U %s\n' \
'{
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
