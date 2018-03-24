#! /bin/bash
echo "$@"
ssh re-ops@192.168.122.161 /usr/bin/node "$@"
