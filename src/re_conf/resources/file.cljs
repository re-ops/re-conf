(ns re-conf.resources.file
  "File resources"
  (:require-macros
   [clojure.core.strint :refer (<<)])
  (:require
   [re-conf.resources.shell :refer (exec)]
   [clojure.string :refer (split-lines join split)]
   [re-conf.resources.log :refer (info debug error channel?)]
   [re-conf.resources.common :refer (run obj->clj error?)]
   [re-conf.spec.file :refer (check-link check-dir check-file contains)]
   [cljs.core.async :as async :refer [<! go put! chan]]
   [cljs-node-io.core :as io]
   [cljs-node-io.fs :as fs]
   [cljstache.core :refer [render]]))

(def fs (js/require "fs"))

; utility functions

(defn- translate
  "convert io.fs nil to :ok and err? into {:error e}"
  [c m]
  (go
    (let [[f _ :as r] (<! c)]
      (cond
        (nil? f) {:ok m}
        :else {:error (if (string? f) f (map obj->clj r))}))))

; inner resource implemetation

(defn- run-template
  "Create a file from a template with args"
  [args tmpl dest]
  (go
    (let [[esl s] (<! (io/aslurp tmpl))]
      (if esl
        {:error {:message "reading template source failed" :error esl :source tmpl}}
        (let [[esp] (<! (io/aspit dest (render s args)))]
          (if esp
            {:error {:message "reading template source failed" :error esp :source tmpl}}
            {:ok {:message "writing template source success" :template tmpl :dest dest}}))))))

(defn- append-options [args opts]
  (cond-> args
    (opts :recursive) (into ["-R"])))

(defn- run-copy
  [src dest]
  (let [c (chan)]
    (.copyFile fs src dest
               (fn [e]
                 (if e
                   (put! c {:error e})
                   (put! c {:ok (<< "copied ~{src} to ~{dest}")}))))

    c))

(defn- rmdir [d]
  (translate
   (go
     (if-not (:exists (<! (check-dir d)))
       [nil (<< "folder ~{d} missing, skipping rmdir")]
       (let [[err v]  (<! (fs/areaddir d))]
         (if err
           [err]
           (if (empty? v)
             (<! (fs/armdir d))
             (<! (fs/arm-r d)))))))
   (<< "folder ~{d} removed")))

(defn- mkdir [d]
  (translate
   (go
     (if-not (:exists (<! (check-dir d)))
       (<! (fs/amkdir d))
       [nil (<< "folder ~{d} exists, skipping mkdir")]))
   (<< "folder ~{d} created")))

(defn- touch [dest]
  (translate
   (go
     (if-not (:exists (<! (check-file dest)))
       (<! (fs/atouch dest))
       [nil (<< "file ~{dest} exists, skipping touch")]))
   (<< "file ~{dest} touched")))

(defn- rmfile [dest]
  (translate
   (go
     (if (:exists (<! (check-file dest)))
       (<! (fs/arm dest))
       [nil (<< "file ~{dest} does not exists, skipping file rm")]))
   (<< "file ~{dest} removed")))

(defn- mklink
  [src target]
  (translate
   (go
     (let [{:keys [error ok exists]} (<! (check-link src target))]
       (if-not exists
         (<! (fs/asymlink src target))
         (if ok
           [nil ok]
           [error]))))
   (<< "link from ~{src} to ~{target} created")))

(defn- rmlink
  [path]
  (translate
   (go
     (let [{:keys [error ok exists]} (<! (check-link path))]
       (if exists
         (<! (fs/aunlink path))
         (if ok
           [nil ok]
           [error]))))
   (<< "link ~{path} removed")))

(defn- add-line "Append a line to a file"
  [dest line]
  (go
    (let [{:keys [present error]} (<! (contains dest line))]
      (if (and error (nil? present))
        {:error error}
        (if present
          {:ok (<< " ~{dest} contains ~{line} skipping") :skip true}
          (<! (translate (fs/awriteFile dest line {:append true}) (<< "added ~{line} to ~{dest}"))))))))

(defn- set-key [k v sep]
  (fn [line]
    (let [[f & _] (split line (re-pattern sep))]
      (if (= f k)
        (str k sep v)
        line))))

(defn- set-line [dest k v sep]
  (go
    (if-let [e (error? (<! (check-file dest)))]
      {:error e}
      (let [[err lines] (<! (fs/areadFile dest "utf-8"))]
        (if err
          {:error err}
          (let [edited (map (set-key k v sep)  (split-lines lines))]
            (<!
             (translate
              (fs/awriteFile dest (join "\n" edited) {:override true})
              (<< "set ~{k}~{sep}~{v}")))))))))

(defn line-eq
  "line equal predicate"
  [line]
  (fn [curr] (not (= curr line))))

(defn into-spec [m args]
  (if (empty? args)
    m
    (let [a (first args)]
      (cond
        (or (fn? a) (string? a)) (into-spec (clojure.core/update m :args (fn [v] (conj v a))) (rest args))
        (channel? a) (into-spec (assoc m :ch a) (rest args))
        (keyword? a) (into-spec (assoc m :state a) (rest args))))))

(defn- rm-line [dest f]
  (go
    (let [[err lines] (<! (fs/areadFile dest "utf-8"))]
      (if err
        {:error err}
        (let [filtered (filter f (split-lines lines))]
          (<!
           (translate
            (fs/awriteFile dest (join "\n" filtered) {:override true})
            (<< "removed ~{filtered} from ~{dest}"))))))))
;resources

(defn template
  "Create a file from a mustache template resource:

    (template \"/home/re-ops/.ssh/autorized-keys\" \"authorized-keys.mustache\" {:keys ...})
   "
  ([tmpl dest args]
   (template nil tmpl dest args))
  ([c tmpl dest args]
   (run c run-template [args tmpl dest])))

(defn copy
  "Copy a file resource:

    (copy src dest)
  "
  ([src dest]
   (run-copy src dest))
  ([c src dest]
   (run c copy [src dest])))

(defn chown
  "Change file/directory owner using uid & gid resource:

    (chown \"/home\"/re-ops/.ssh\" \"foo\" \"bar\"); using user/group
    (chown \"/home\"/re-ops/.ssh\" \"foo\" \"bar\" {:recursive true}); chown -R
   "
  ([a u g]
   (if (channel? a)
     (go {:error "missing group"})
     (chown a u g {})))
  ([a b c d]
   (if (channel? a)
     (chown a b c d {})
     (apply exec (append-options ["/bin/chown" (<< "~{b}:~{c}") a] d))))
  ([c dest u g options]
   (run c chown [dest u g options])))

(defn rename
  "Rename a file/directory resource:

    (rename \"/tmp/foo\"  \"/tmp/bar\")
  "
  ([src dest]
   (translate (fs/arename src dest) (<< "~{src} moved to ~{dest}")))
  ([c src dest]
   (run c rename [src dest])))

(defn chmod
  "Change file/directory mode resource:

    (chmod \"/home\"/re-ops/.ssh\" \"0777\")
    (chmod \"/home\"/re-ops/.ssh\" \"0777\" {:recursive true})
  "
  ([dest mode]
   (chmod dest mode {}))
  ([a b c]
   (if (channel? a)
     (chmod a b c {})
     (apply exec (append-options ["/bin/chmod" b a] c))))
  ([c dest mode options]
   (run c chmod [dest mode options])))

(def directory-states {:present mkdir :absent rmdir})

(defn directory
  "Directory resource:
    (directory \"/tmp/bla\") ; create directory
    (directory \"/tmp/bla\" :present) ; explicit create
    (directory \"/tmp/bla\" :absent) ; remove directory
  "
  ([dest]
   (directory nil dest :present))
  ([dest state]
   (directory nil dest state))
  ([c dest state]
   (run c (directory-states state) [dest])))

(def file-states {:present touch :absent rmfile})

(defn file
  "File resource:
    (file \"/tmp/bla\") ; touch a file
    (file \"/tmp/bla\" :present) ; explicit present
    (file \"/tmp/bla\" :absent) ; remove a file
  "
  ([dest]
   (file nil dest :present))
  ([dest state]
   (file nil dest state))
  ([c dest state]
   (run c (file-states state) [dest])))

(defn symlink
  "Symlink resource:

    (symlink \"/home/re-ops/.vim/.vimrc\"  \"/home/re-ops/.vimrc\") ; create symlink
  "
  ([& as]
   (let [{:keys [ch args state] :or {state :present}} (update (into-spec {} as) :args reverse)
         fns {:present mklink :absent rmlink}]
     (if ch
       (run ch (fns state) args)
       (run nil (fns state) args)))))

(defn line
  "File line resource either append or remove lines:

    (line \"/tmp/foo\" \"bar\"); append line to the file
    (line \"/tmp/foo\" \"bar\" :present); append explicit
    (line \"/tmp/foo\" (line-eq \"bar\") :absent); remove lines equal to bar from the file
    (line \"/tmp/foo\" (fn [curr] (> 5 (.length curr))) :absent); remove lines using a function
    (line \"/tmp/foo\" \"key\" \"value\" \"=\" :set); set key value using seperator
  "
  ([& as]
   (let [{:keys [ch args state] :or {state :present}} (update (into-spec {} as) :args reverse)
         fns {:present add-line :set set-line :absent rm-line}]
     (if ch
       (run ch (fns state) args)
       (run nil (fns state) args)))))

(comment
  (info (go (<! (symlink "/home/ronen/foo\\0" :absent))) ::test))
