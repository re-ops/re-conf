(ns re-conf.cljs.download
  "nodejs native file download"
  (:require
   [re-conf.cljs.log :refer (info error)]
   [cljs.core.async :as async :refer [<! >! chan go-loop go take! put!]]
   [cljs.nodejs :as nodejs]))

(def fs (js/require "fs"))
(def request (js/require "request"))
(def crypto (js/require "crypto"))

(defn download
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

(defn checkum
  [f expected k]
  (let [shasum (.createHash crypto (name k))
        stream (.ReadStream fs f)
        c (chan)]
    (.on stream "data" (fn [data] (.update shasum data)))
    (.on stream "error" (fn [e] (put! c {:error e})))
    (.on stream "end" (fn [] (put! c (verify (.digest shasum "hex") expected))))
    c))

(comment
  (info (checkum "/tmp/packer_1.2.2_linux_amd64.zip" "" :sha256) ::debug))
