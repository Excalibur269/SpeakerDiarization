#!/bin/bash

wavPath=$1

mkdir seg_textGird

for dir in `find $wavPath -type d`;
do
	mkdir -p "./seg_textGird/"${dir#$wavPath}
done

for dir in `find $wavPath -name "*" | grep '.seg$'`;
do
	cp -f $dir "./seg_textGird/"${dir#$wavPath}
done
