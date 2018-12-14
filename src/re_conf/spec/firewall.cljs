(ns re-conf.spec.firewall
  "Firewall specifications (UFW)"
  (:require-macros
   [cljs.core.match :refer [match]]
   [clojure.core.strint :refer (<<)]
   [cljs.core.async.macros :refer [go]])
  (:require
   [clojure.string :refer (split-lines trim)]
   [re-conf.resources.log :refer (info debug error)]
   [cljs.core.async :refer [<!]]
   [re-conf.resources.shell :refer (sh)]
   [re-conf.resources.firewall :refer (ufw-bin)]))

(defn- ufw-rule
  "ufw rule status syntax from map"
  [m]
  (match [m]
    [{:from f :port p}] [(str p) "ALLOW" f]
    [{:from f :to t :port p}] [(str p) "on" t "ALLOW" f]
    [{:from f :to t}] [t "ALLOW" f]
    [{:port p}] [(str p) "ALLOW" "Anywhere"]))

(defn status-lines [{:keys [out]}]
  (map
   (fn [line]
     (filter (comp not empty?) (map trim (.split line "  ")))) (split-lines out)))

(defn check-rule
  "Check if rule is in place"
  [rule]
  (go
    (let [{:keys [ok error]} (<! (sh ufw-bin "status")) expected (ufw-rule rule)]
      (if-not ok
        {:error error}
        (if-let [match (first (filter #(= % expected) (status-lines ok)))]
          {:ok (<< "matched rule ~{match}")}
          {:error (<< "rule ~{expected} not found")})))))

(comment
  (info (check-rule {:port 22}) ::check)
  (info (check-rule {:to "10.0.1.1" :from "10.0.1.10"}) ::check))
