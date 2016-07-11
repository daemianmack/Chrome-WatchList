(ns options.ui
  (:require [clojure.string :refer [split join replace upper-case]]))

(enable-console-print!)

(defn to-regex [s]
  (->> (split s #"\n")
       (filter not-empty)
       (join "|")))

(defn to-text [s]
  (->> (split s #"\|")
       (join "\n")))

(defn id->el [id] (.getElementById js/document id))

(defn set-val!  [id val] (set! (.-value     (id->el id)) val))
(defn set-html! [id val] (set! (.-innerHTML (id->el id)) val))

(defn upcase [s]
  (str (upper-case (first s)) (subs s 1)))

(defn update-status [tab-handle]
  (.setTimeout js/window
               #(set-html! (str tab-handle "-status") "")
               3000)
  (set-html! (str tab-handle "-status")
             (str (upcase tab-handle) " saved.")))

(defn save-options* [tab-handle val-fn]
  (.get js/chrome.storage.sync "watchlist"
        (fn [data]
          (let [data (js->clj data)
                el   (id->el (str tab-handle "-input"))]
            (.set js/chrome.storage.sync
                  (clj->js (assoc-in data ["watchlist" tab-handle]
                                     (val-fn (.-value el))))
                  #(update-status tab-handle))))))

(defmulti save-options! (fn [tab-handle] tab-handle))

(defmethod save-options! :default [tab-handle]
  (save-options* tab-handle to-regex))

(defmethod save-options! "styles" [tab-handle]
  (save-options* tab-handle identity))

(defn retrieve-options! []
  (.get js/chrome.storage.sync "watchlist"
        (fn [data]
          (let [{options "watchlist"} (js->clj data)
                {:strs [terms blacklist styles]} options]
            (when terms     (set-val! "terms-input"     (to-text terms)))
            (when blacklist (set-val! "blacklist-input" (to-text blacklist)))
            (when styles    (set-val! "styles-input"     styles))))))

(def tab-handles  #{"terms" "blacklist" "styles"})
(def tab-suffixes #{"-tab" "-form"})

(defn classes+ [node class] (str     (.-className node) " " class))
(defn classes- [node class] (replace (.-className node) class ""))

(defn add-class! [node class] (set! (.-className node) (classes+ node class)))
(defn del-class! [node class] (set! (.-className node) (classes- node class)))

(defn assign-click-handlers! []
  (doseq [handle tab-handles]
    (set! (.-onclick (id->el (str handle "-tab")))
          (fn [e]
            (doseq [handle tab-handles
                    suffix tab-suffixes]
              (del-class! (id->el (str handle suffix)) "active"))
            (doseq [suffix tab-suffixes]
             (add-class! (id->el (str handle suffix))  "active"))))))

(defn save-handlers! []
  (doseq [tab-handle #{"terms" "blacklist" "styles"}]
    (set! (.-onclick (id->el (str tab-handle "-save")))
          (partial save-options! tab-handle))))

(defn main []
  (retrieve-options!)
  (assign-click-handlers!)
  (save-handlers!))

(main)
