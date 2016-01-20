#!/bin/bash

md_install_dir=`(cd ../..; pwd)`

ant \
	-Ddebug.mode=true \
	-Dbuild.jar.mode=jar \
	-Ddeveloper.mode=true \
	-Dmd.install.dir=${md_install_dir} \
	-f runTests.ant
	