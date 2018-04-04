(ns re-conf.cljs.common)

(defn- channel?
  "check is x is a channel"
  [x]
  (= (type x) cljs.core.async.impl.channels/ManyToManyChannel))
