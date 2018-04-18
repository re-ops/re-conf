(ns re-conf.rcp.build
  "build tools recipes"
  (:require
   [re-conf.cljs.download :refer (download checksum)]
   [re-conf.cljs.archive :refer (unzip)]
   [re-conf.cljs.shell :refer (exec)]
   [re-conf.cljs.output :refer (summary)]))

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
