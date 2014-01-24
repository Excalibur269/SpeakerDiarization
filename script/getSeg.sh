#!/bin/bash

wavPath=$1
saveResult=$2	#$2="d" donot save seg


if (( $saveResult=="d" || $saveResult=="-d" ));
then
	exit
else
	mkdir seg_result
fi

for dir in `find $wavPath -type d`;
do
	mkdir -p "./seg_result/"${dir#$wavPath}
done

for dir in `find $wavPath -name "*" | grep '.seg$'`;
do
	cp -f $dir "./seg_result/"${dir#$wavPath}
done
