#!/bin/bash

LIC_FILE=licence_top.txt
LIC_FILE_TMP=$LIC_FILE.tmp

for file in $(find src/com/ -name '*.java')
	do
		echo "Modifying file : $file"
		cp $LIC_FILE $LIC_FILE_TMP
		cat $file >> $LIC_FILE_TMP
		mv $LIC_FILE_TMP $file 		
done
