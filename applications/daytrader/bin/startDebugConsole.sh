#!/bin/bash

export GERONIMO_HOME=~/geronimo/geronimo/modules/assembly/target/geronimo-1.2-SNAPSHOT

java -jar ${GERONIMO_HOME}/bin/deployer.jar --user system --password manager start  org/apache/geronimo/DebugConsole

