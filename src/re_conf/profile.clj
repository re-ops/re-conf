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

(defn profile-data
  "grab profiled resources "
  [lines]
  (filter (fn [m] (get-in m [:message :function])) lines))

(def winston-time (f/formatter "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))

(def vega-fmt (f/formatter "HH:mm:ss"))

(defn normalize [rs]
  (map
   (fn [m]
     (let [{:keys [function profile timestamp]} m [n c] profile
           f (last (clojure.string/split function #"\."))]
       {:x (f/unparse vega-fmt (f/parse winston-time timestamp))
        :y (clojure.edn/read-string (str n "." c))
        :col f}))
   rs))

(defn grouped
  "group resources by type"
  [rs]
  (group-by :col rs))

(defn resources [f]
  (normalize (cleanup (profile-data (logs f)))))

(defn aggregates [f]
  (map
   (fn [[k rs]]
     {:resource k :profile (reduce (fn [s {:keys [y]}] (+ s y)) 0 rs)})
   (grouped (resources f))))

(defn timeline [f]
  {:mark "point"
   :width 800
   :data
   {:values (resources f)
    :format {:parse {:x "utc: '%H:%M:%s'"}}}
   :encoding {:x {:field "x" :timeUnit "minutesseconds" :type "temporal"}
              :y {:field "y" :type "quantitative"}
              :color {:field "col" :type "nominal"}}})

(defn total
  ([f]
   (total f #{}))
  ([f excluded]
   {:mark "bar"
    :width 800
    :data {:values (filter (fn [{:keys [resource]}] (not (excluded resource))) (aggregates f))}
    :encoding {:x {:field "resource" :type "ordinal"}
               :y {:field "profile" :type "quantitative"}
               :color {:field "resource" :type "nominal"}}}))

(defn viz [f]
  [:div
   [:h1 "re-conf profiling"]
   [:p "timeline and resources compared"]
   [:div
    [:vega-lite (timeline f)]
    [:vega-lite (total f)]]])

(comment
  (start-server)
  (oz/view! (viz "logs/nas.json")))
