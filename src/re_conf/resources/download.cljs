(ns re-conf.resources.download
  "nodejs native file download"
  (:require
   [re-conf.resources.common :refer (run)]
   [re-conf.resources.log :refer (info error)]
   [cljs.core.async :as async :refer [<! >! chan go-loop go take! put!]]
   [cljs.nodejs :as nodejs]))

(def fs (js/require "fs"))
(def request (js/require "request"))
(def crypto (js/require "crypto"))

(defn run-download
  "download a file"
  [url dest]
  (let [file (.createWriteStream fs dest)
        getter (.get request url)
        c (chan)]
    (.on getter "error"
         (fn [e]
           (error e ::download)
           (.unlink fs dest)
           (put! c {:error e})))
    (.on getter "response"
         (fn [resp]
           (when-not (= 200 (.-statusCode resp))
             (error resp ::download)
             (put! c {:error resp}))))
    (.on file "finish"
         (fn []
           (.close file
                   (fn [_] (put! c {:ok "file downloaded"})))))
    (.pipe getter file)
    c))

(defn verify
  "Verify checksum matching"
  [sum expected]
  (if (= sum expected)
    {:ok {:message "checksum matches" :sum sum :expected expected}}
    {:error {:message "checksum does not match!" :sum sum :expected expected}}))

(defn run-checkum
  [f expected k]
  (let [shasum (.createHash crypto (name k))
        stream (.ReadStream fs f)
        c (chan)]
    (.on stream "data" (fn [data] (.update shasum data)))
    (.on stream "error" (fn [e] (put! c {:error e})))
    (.on stream "end" (fn [] (put! c (verify (.digest shasum "hex") expected))))
    c))

; resources

(defn checksum
  "Checksum a file and validate expected value"
  ([file e k]
   (checksum nil file e k))
  ([c file e k]
   (run c #(run-checkum file e k))))

(defn download
  "Download file resource, if checksum is provided download will be lazy:
    (download url dest expected :sha256); download if file missing or checksum mismatch
    (download url dest); always download
   "
  ([url dest]
   (download nil url dest))
  ([c url dest]
   (run c #(run-download url dest)))
  ([url dest sum k]
   (download nil url dest sum k))
  ([c url dest sum k]
   (go
     (if (and (.existsSync fs dest) (:ok (<! (checksum dest sum k))))
       {:ok "Existing downloaded file match checksum, skipping" :skip true}
       (<!
        (-> c
            (download url dest)
            (checksum dest sum k)))))))

(comment
  (info (checksum "/tmp/restic_0.8.3_linux_amd64.bz2" "123" :sha256) ::debug))
