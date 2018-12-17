(ns re-conf.profile
  (:require
   [clj-time.core :as c]
   [clj-time.format :as f]
   [oz.core :as oz]
   [cheshire.core :as json]))

(defn start-server []
  (oz/start-plot-server!))

(defn line-plot [vs]
  {:data {:values vs}
   :encoding {:x {:field "x"}
              :y {:field "y"}
              :color {:field "col" :type "nominal"}}
   :mark "line"})

(defn cleanup
  "keep only analysis friendly keys"
  [rs]
  (map
   (fn [{:keys [message timestamp]}]
     (merge (select-keys message #{:function :profile}) {:timestamp timestamp})) rs))

(defn by-function
  "group by function type"
  [rs]
  (group-by :function rs))

(defn logs [f]
  (map #(json/parse-string % true) (clojure.string/split (slurp f) #"\n")))

(defn resources
  "grab profiled resources "
  [lines]
  (filter (fn [m] (get-in m [:message :function])) lines))

(def winston-time (f/formatter "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))

(def vega-fmt (f/formatter "HH:mm:ss"))

(defn into-group [rs]
  (map-indexed
   (fn [i m]
     (let [{:keys [function profile timestamp]} m [n c] profile]
       {:x (f/unparse vega-fmt (f/parse winston-time timestamp))
        :y (clojure.edn/read-string (str n "." c))
        :col function}))
   rs))

(defn load- []
  (into-group (cleanup (resources (logs "example.json")))))

(def line-plot
  {:data {:values (load-)
          :format {:parse {:x "%HH:%mm:%ss"}}}
   :width 800
   :encoding {:x {:field "x" :timeunit "seconds" :type "temporal"}
              :y {:field "y"}
              :color {:field "col" :type "nominal"}}
   :mark "line"})

(comment
  (start-server)
  (oz/v! line-plot))
