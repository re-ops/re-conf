(ns re-conf.resources.common
  "Common resource functions"
  (:require
   [re-conf.resources.log :refer (info debug error)]
   [cljs.core.async :as async :refer [<! go]]))

(defn ok?  [m]
  (contains? m :ok))

(defn error? [m]
  (when-not (ok? m)
    (:error m)))

(def process (js/require "process"))

(defn function-name
  [f]
  (let [parts (clojure.string/split (str (js/Object f)) #"\s")
        raw (first (clojure.string/split (second parts) #"\("))]
    (-> raw
        (clojure.string/replace #"\$" ".")
        (clojure.string/replace #"\_" "-"))))

(defn profile [f args]
  (go
    (let [start (.hrtime process)]
      (debug {:profile start :function (function-name f) :pre true} ::profile)
      (let [r (<! (apply f args))
            end (.hrtime process start)]
        (debug (assoc r :profile end :function (function-name f) :post true) ::profile)
        r))))

(defn run [c f args]
  (go
    (let [r (if c (<! c) {:ok true})]
      (if (:ok r)
        (<! (profile f args))
        r))))

(defn obj->clj
  "js->clj does not work for objects with custom ctors"
  [obj]
  (-> (fn [result k]
        (let [v (aget obj k)]
          (if (= "function" (goog/typeOf v))
            result
            (assoc result (keyword k) v))))
      (reduce {} (.getKeys goog/object obj))))

