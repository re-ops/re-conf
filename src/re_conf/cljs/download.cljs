(ns re-conf.cljs.download
  "nodejs native file download"
  (:require
   [re-conf.cljs.log :refer (info)]
   [cljs.core.async :as async :refer [<! >! chan go-loop go take!]]
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
    (.on getter "error" (fn [e] (error e) (go (>! c {:error e}))))
    (.on getter "response" (fn [resp] (go (>! c {:ok (.-statusCode resp)}))))
    (.pipe getter file)
    c))

(defn checkum
  [f k]
  (let [shasum (.createHash crypto (name k))
        stream (.ReadStream fs f)
        c (chan)]
    (.on stream "data" (fn [data] (.update shasum data)))
    (.on stream "error" (fn [e] (go (>! c {:error e}))))
    (.on stream "end" (fn [] (go (>! c {:ok (.digest shasum "hex")}))))
    c))

(comment
  (take! (checkum "/tmp/lein-standalone383452244645207519.jar" :sha512) (fn [r] (info r ::log))))
