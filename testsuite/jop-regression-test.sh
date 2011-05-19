#!/bin/sh
#
# git pull and run JOP regression test
#
LOG_DIR=`pwd`/logs

# Change to jop directory
cd jop

# Pull
git pull

# Make sure working tree is clean
# git status | grep "working directory clean" >/dev/null
# if [ $? -ne 0 ] ; then
#  echo "Working directory not clean - aborting nightly build" ; exit 1
# fi

# Setup the environment
. testsuite/env.sh

# Run testsuite
testsuite/rotate.sh ${LOG_DIR}
mkdir ${LOG_DIR}/current
testsuite/run.sh ${LOG_DIR}/current

# Clean the working tree
# git -d clean

# Mail results
LOG_FILE=${LOG_DIR}/current/report.txt
RECIPIENTS=`cat testsuite/recipients.txt`
dos2unix ${LOG_FILE}
mail -s "[JOP Nightly] Build report `date`" ${RECIPIENTS} < ${LOG_FILE}
