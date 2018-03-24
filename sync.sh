while inotifywait -r .; do
   rsync -avz . re-ops@192.168.122.161:/home/re-ops/code/shim/
done
