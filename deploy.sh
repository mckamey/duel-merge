#!/bin/sh

clear;clear

cd merge-builder
mvn clean deploy -U -DperformRelease=true -Dgpg.keyname=EE82F9AB
cd ..

cd merge-maven-plugin
mvn clean deploy -U -DperformRelease=true -Dgpg.keyname=EE82F9AB
