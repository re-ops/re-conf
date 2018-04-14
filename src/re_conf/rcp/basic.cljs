(ns re-conf.rcp.basic
  (:require
   [cljs.core.async :refer [go]]
   [re-conf.cljs.log :refer (info error)]
   [re-conf.cljs.pkg :refer (install)]
   [re-conf.cljs.download :refer (download checksum)]
   [re-conf.cljs.archive :refer (unzip)]
   [re-conf.cljs.shell :refer (exec)]
   [re-conf.cljs.core :refer (template summary)]))

(defn packer
  "Setup up packer"
  []
  (let [dest "/tmp/packer_1.2.2_linux_amd64.zip"
        sha "6575f8357a03ecad7997151234b1b9f09c7a5cf91c194b23a461ee279d68c6a8"
        url "https://releases.hashicorp.com/packer/1.2.2/packer_1.2.2_linux_amd64.zip"]
    (->
     (download url dest)
     (checksum dest sha :sha256)
     (unzip dest "/tmp/packger")
     (summary "installing packer done"))))

(defn restic
  "Setting up restic"
  []
  (let [dest "/tmp/restic_0.8.3_linux_amd64.bz2"
        sha "1e9aca80c4f4e263c72a83d4333a9dac0e24b24e1fe11a8dc1d9b38d77883705"
        url "https://github.com/restic/restic/releases/download/v0.8.3/restic_0.8.3_linux_amd64.bz2"]
    (->
     (download url dest)
     (checksum dest sha :sha256)
     (exec "bzip2" "-f" "-d" dest)
     (summary "installing restic done"))))

(comment
  (do
    (go (restic))
    (go (packer))))
