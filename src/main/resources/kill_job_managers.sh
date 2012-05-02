#!/usr/bin/env bash

# kill all globus job managers of the user who submitted this job
# and then sleep for the configured number of seconds

sleep_time=$1
success_string=$2
error_string=$3
uid=$(whoami)
pids=$(ps -ef | grep globus-job-manager | grep ${uid} | grep -v grep | sed 's/ \+/ /g' | cut -d\  -f2)

sleep 5
for pid in ${pids}; do
  kill -9 ${pid}
done

sleep 5
pids2=$(ps -ef | grep globus-job-manager | grep ${uid} | grep -v grep | sed 's/ \+/ /g' | cut -d\  -f2)
if [ "X" == "X${pids2}" ] && [ "X" != "X${pids}" ]; then
  echo ${success_string} > stdout.txt
else
  echo ${error_string} > stdout.txt
fi

sleep ${sleep_time}