(ns re-conf.profile
  (:require
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

(defn load- []
  (filter identity
          (map-indexed
           (fn [i m] (let [[n c] (get-in m [:message :profile])]
                       (when n {:x i :y (clojure.edn/read-string (str n "." c))})))
           (map #(json/parse-string % true) (clojure.string/split (slurp "example.json") #"\n")))))

(comment
  (start-server)
  (oz/v! (line-plot (load-))))
