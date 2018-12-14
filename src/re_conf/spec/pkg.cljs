(ns re-conf.spec.pkg
  "Specification for packages"
  (:require-macros
   [clojure.core.strint :refer (<<)]
   [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :refer [<!]]
   [re-conf.resources.shell :refer (sh)]
   [re-conf.resources.common :refer (run)]))

(defn- check-finger
  "Verify a apt-key gpg key signature with id"
  [id]
  (go
    (let []
      (let [{:keys [ok]} (<! (sh "/usr/bin/apt-key" "fingerprint" id))
            {:keys [out]} ok]
        (if-not (empty? out)
          {:ok (<< "key ~{id} verfied") :key-info out}
          {:error (<< "failed to verify key ~{id}") :key-failure out})))))

(defn fingerprint
  "Check apt key gpg key fingerprint specification"
  ([id]
   (check-finger id))
  ([c id]
   (run c fingerprint [id])))

