while inotifywait -r /tmp/cljs-repl-share; do
   rsync -avz . re-ops@192.168.122.161:/tmp/cljs-repl-share/
done
