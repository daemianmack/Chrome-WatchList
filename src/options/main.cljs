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

(defn save-options! [tab-handle]
  (.get js/chrome.storage.sync "watchlist"
        (fn [data]
          (let [data (js->clj data)
                el   (id->el (str tab-handle "-input"))]
           (.set js/chrome.storage.sync
                 (clj->js
                  (assoc-in data ["watchlist" tab-handle]
                            (to-regex (.-value el))))))
          (.setTimeout js/window
                       #(set-html! (str tab-handle "-status") "")
                       3000)
          (set-html! (str tab-handle "-status")
                     (str (upper-case (first tab-handle))
                          (subs tab-handle 1)
                          " saved.")))))

(defn retrieve-options! []
  (.get js/chrome.storage.sync "watchlist"
        (fn [data]
          (let [{{:strs [terms blacklist]} "watchlist"} (js->clj data)]
            (when terms     (set-val! "terms-input"     (to-text terms)))
            (when blacklist (set-val! "blacklist-input" (to-text blacklist)))))))

(def tab-handles  #{"terms" "blacklist"})
(def tab-suffixes #{"-tab" "-form"})

(defn classes+ [node class] (str (.-className node) " " class))
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
  (doseq [tab-handle #{"terms" "blacklist"}]
    (set! (.-onclick (id->el (str tab-handle "-save")))
          (partial save-options! tab-handle))))

(defn main []
  (retrieve-options!)
  (assign-click-handlers!)
  (save-handlers!))

(main)
