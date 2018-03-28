(ns re-conf.cljs.download
  "nodejs native file download"
  (:require
   [taoensso.timbre :as timbre :refer-macros  [trace debug info error]]
   [cljs.core.async :as async :refer [<! >! chan go-loop go take!]]
   [cljs.nodejs :as nodejs]))

(def fs (js/require "fs"))
(def request (js/require "request"))
(def crypto (js/require "crypto"))

(defn download
  "download a file"
  [url dest c]
  (let [file (.createWriteStream fs dest)
        getter (.get request url)]
    (.on getter "error" (fn [e] (error e) (go (>! c {:error e}))))
    (.on getter "response" (fn [resp] (go (>! c {:ok (.-statusCode resp)}))))
    (.pipe getter file)))

(defn checkum
  [f k c]
  (let [shasum (.createHash crypto (name k))
        stream (.ReadStream fs f)]
    (.on stream "data" (fn [data] (.update shasum data)))
    (.on stream "error" (fn [e] (go (>! c {:error e}))))
    (.on stream "end" (fn [] (go (>! c {:ok (.digest shasum "hex")}))))))

(comment
  (let [c (chan)]
    (download "https://releases.hashicorp.com/packer/1.2.2/packer_1.2.2_linux_386.zip" "/tmp/packer-2.zip" c)
    (take! c (fn [r] (println r))))

  (let [c (chan)]
    (checkum "/tmp/acker-2.zip" :sha512 c)
    (take! c (fn [r] (println r)))))
