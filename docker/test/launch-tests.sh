#!/bin/bash 

CLASSPATH=.:./jars/*

echo ==== Running test ScalarTest
java ScalarTest ADKDPM G:DPMTEST1 12.345

if [ $? -ne 0 ]; then
   echo ==== Failed ScalarTest
else 
   echo ==== Success!
fi

echo ==== Running test DAQ simple on G:DPMTEST1
java Example 

if [ $? -ne 0 ]; then
   echo ==== Failed DAQ simple
else 
   echo ==== Success!
fi
