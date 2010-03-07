#!/bin/bash -e
export CLASSPATH=.:mysql-connector-java.jar:pircbot.jar
java KattilaBot &
echo $! >kattilabot.pid

