(ns re-conf.rcp.backup
  "Backup utilities recipes"
  (:require
   [re-conf.cljs.download :refer (download checksum)]
   [re-conf.cljs.archive :refer (bzip2)]
   [re-conf.cljs.shell :refer (exec)]
   [re-conf.cljs.output :refer (summary)]))

(defn restic
  "Setting up restic"
  []
  (let [dest "/tmp/restic_0.8.3_linux_amd64.bz2"
        sha "1e9aca80c4f4e263c72a83d4333a9dac0e24b24e1fe11a8dc1d9b38d77883705"
        url "https://github.com/restic/restic/releases/download/v0.8.3/restic_0.8.3_linux_amd64.bz2"]
    (->
     (download url dest)
     (checksum dest sha :sha256)
     (bzip2 dest)
     (summary "installing restic done"))))
