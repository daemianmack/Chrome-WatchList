(ns options.ui
  (:require [clojure.string :refer [split join]]))


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

(defn save-options! []
  (.set js/chrome.storage.sync
        (clj->js
         {:watchlist {:terms     (to-regex (.-value (id->el "watchlist_term_input")))
                      :blacklist (to-regex (.-value (id->el "watchlist_blacklist_input")))}}))
  (.setTimeout js/window
               #(set-html! "status" "")
               1000)
  (set-html! "status" "Options saved."))

(defn retrieve-options! []
  (.get js/chrome.storage.sync "watchlist"
        (fn [data]
          (let [{{:strs [terms blacklist]} "watchlist"} (js->clj data)]
            (when terms     (set-val! "watchlist_term_input"      (to-text terms)))
            (when blacklist (set-val! "watchlist_blacklist_input" (to-text blacklist)))))))

(defn main []
  (set! (.-onclick (.getElementById js/document "save")) save-options!)
  (retrieve-options!))

(main)
