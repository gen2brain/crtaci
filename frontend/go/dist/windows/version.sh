#!/bin/bash
VERSION=`cat ../../../../backend/crtaci/crtaci.go | grep Version | awk -F' = ' '{print $2}' | tr -d '"'`
sed "s/{VERSION}/$VERSION/g" crtaci.iss.in > crtaci.iss
