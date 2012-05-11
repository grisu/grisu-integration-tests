sleep_seconds=$1

on_normal_term_message="NORMAL_TERMINATION"
on_normal_term_rc=0
on_killed_message="GOT_KILLED"
on_killed_rc=42

trap "echo ${on_killed_message}; exit ${on_killed_rc}" INT TERM

sleep ${sleep_seconds}
echo ${on_normal_term_message}
exit ${on_normal_term_rc}