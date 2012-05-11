sleep_seconds=$1

killed_jobmanagers_success_message="KILLED_JOBMANAGERS_SUCCESS"
killed_jobmanagers_failure_message="KILLED_JOBMANAGERS_FAILURE"
on_normal_term_message="NORMAL_TERMINATION"
on_normal_term_rc=0
on_killed_message="GOT_KILLED"
on_killed_rc=42

get_job_manager_pids() {
  uid=$(whoami)
  echo $(ps -ef | grep globus-job-manager | grep ${uid} | grep -v grep | sed 's/ \+/ /g' | cut -d\  -f2)
}

trap "echo ${on_killed_message}; exit ${on_killed_rc}" INT TERM

pids=$(get_job_manager_pids)

# wait for job managers to have fully started
# without waiting, a job manager might be restarted
sleep 5

for pid in ${pids}; do
  kill -9 ${pid}
done

# a good kill may need time
sleep 5

pids2=$(get_job_manager_pids)

if [ "X" == "X${pids2}" ] && [ "X" != "X${pids}" ]; then
  echo ${killed_jobmanagers_success_message}
else
  echo ${killed_jobmanagers_failure_message} 
fi

# wait for the configured number of seconds
sleep ${sleep_seconds}
echo ${on_normal_term_message}
exit ${on_normal_term_rc}