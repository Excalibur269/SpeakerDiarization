mkdir package

ant -f  ./SpeakerDiarization/build.xml 
cp ./SpeakerDiarization/src/lium_lib.jar ./package
cp ./SpeakerDiarization/build/SpeakerSeperation.jar ./package/
cp ./script/shell_SBS_TEST/* ./package/

tar -cf package.tar package/
rm -rf package
