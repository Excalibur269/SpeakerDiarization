#!/bin/bash

testTool=$1
wavPath=$2/

tmp_file="/tmp/$.fifo"
mkfifo $tmp_file
exec 6<>$tmp_file
rm $tmp_file

thread=10
for ((i=0;i<$thread;i++));
do
	echo "\r"
done >& 6


for file in `find $wavPath -name "*" | grep '.wav$'`;
do
	read -u6
	{
		fileBase=`basename $file`
		fileBase=${fileBase%.wav}

		java -Xmx4G -jar $testTool --fInputMask=$file   --sOutputMask=${file%.wav}".seg"  --sOutputFormat=seg $fileBase 
								
		echo >& 6
	}&		
done

exec 6>&-

wait

exit

