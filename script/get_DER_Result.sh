#!/bin/sh

grep 'ERROR =' $1 | awk -v FS=[=\)\ ] '{print $8,$18}' | sort -n > ${1/%".txt"/".result"}

