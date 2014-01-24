#!/bin/bash

#path=/media/files/zbh/testdata_speaker/
path=$1

for dir in `ls $path`;
do
	echo "./ehmm_result/"$dir 
	mkdir -p "./ehmm_result/"$dir

	#for file in `ls $path$dir | grep '[(_64)(_32)].ehmm$'` 
	for file in `ls $path$dir | grep 'jar.ehmm$'` 
	do
		cp -r $path$dir"/"$file "./ehmm_result/"$dir"/"$file
	done
done
 
