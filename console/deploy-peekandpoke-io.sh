#!/bin/bash

# Get caller directory
initial_wd=`pwd`

echo "Switching to dir $initial_wd"
pwd

shopt -s dotglob

echo "Uploading JS files"
scp -r ./build/dist/js/productionExecutable/* finzo:/www/htdocs/w0057ac0/finzo/klang.finzo.de
#scp -r ./build/kotlin-webpack/js/developmentExecutable/* finzo:/www/htdocs/w0057ac0/finzo/klang.finzo.de

echo "Uploading resources"
scp -r ./src/jsMain/resources/* finzo:/www/htdocs/w0057ac0/finzo/klang.finzo.de/
