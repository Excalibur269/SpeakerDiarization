testTool=$1			#first arg is the test tool
wavPath=$2   		#second arg is the wav path
outPath=$3			#third arg is dir to store result
saveResult=$4		#forth arg is "s" or "-s"   save seg


#run all the wav
./runSBS.sh $testTool $wavPath

#deal the result   get the seg and textGrid
mkdir seg_textGrid
for dir in `find $wavPath -name "*" | grep '.[(seg)(TextGrid)]$'`;
do
	cp -f $dir "./seg_textGrid/"`basename $dir`
done

java -jar Hypothesis.jar ./seg_textGrid/
java -jar Reference.jar ./seg_textGrid/

rm -rf seg_textGrid

#get DER into $outPath"/out.txt"
perl md-eval-v21.pl -afc -c 1 -r Reference.rttm -s ./Hypothesis.rttm &> $outPath"/out.txt"
#get the statistics of DER.txt into $outPath"/out.result"
grep 'ERROR =' $outPath"/out.txt" | awk -v FS=[=\)\ ] '{print $8,$18}' | sort -n > $outPath"/out.result"


#save seg into seg_result
if [[ $saveResult = "s" || $saveResult = "-s" ]]
then
	mkdir $outPath"/seg_result"
	
	for dir in `find $wavPath -type d`;
	do
		mkdir -p "./seg_result/"${dir#$wavPath}
	done

	for dir in `find $wavPath -name "*" | grep '.seg$'`;
	do
		cp -f $dir $outPath"/seg_result/"${dir#$wavPath}
		rm $dir			#delete intermediate result
	done
fi
