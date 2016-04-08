(ns chromex-sample.content-script.core
  (:require-macros [reagent.ratom :refer [reaction]]
                   [content-script.chromex-sample.content-script.macros :refer [inspect]])
  (:require [reagent.core :as reagent]
            [goog.dom :as gdom]
            [goog.net.XhrIo]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]))

(extend-type js/NodeList
  ISeqable
  (-seq [array] (array-seq array 0)))



(register-handler                 
 :initialize                     
 (fn
   [db _]
   (let [terms #"dynamically|created|who"]
     (let [elems (.querySelectorAll js/document
                                    "*:not(iframe):not(noscript):not(script):not(textarea)")
           elems (.call js/Array.prototype.slice elems)
           matches (.filter elems (fn [x] (re-find terms (.-innerHTML x))))
           matches (mapcat (fn [el] 
                             (let [html (.-innerHTML el)]
                               (set! (.-innerHTML el) (str "<em>" html "</em>")) 
                               (for [match (re-seq terms html)]
                                 {:term match :node el}))) 
                           matches)]
       (merge db {:started (.getTime (js/Date.))
                  :matches (vec matches)})))))

(register-sub
 :initialize
 (fn [db _]
   (reaction @db)))

(defn simple-example
  []
  (let [matches (subscribe [:initialize])]
    (fn []
      [:div {:id "watchlist-status-bar" :class :loading}
       [:span {:class "watchlist-status-bar-item"}
        [:a {:href "/BARF"}]]
       [:span {:class "watchlist-status-bar-separator"}]
       (for [[group-term hits] (group-by :term (:matches @matches))]
         [:span {:id :watchlist-results
                 :class "watchlist-status-bar-item"
                 :title (str (- (.getTime (js/Date.)) (:started @matches))
                             " ms elapsed")}
          group-term ":" (count hits)])])))

(defn ^:export init!
  []
  (.get js/chrome.storage.sync "watchlist"
        (fn [option-data]
          (let [{{:strs [terms blacklist]} "watchlist"} (js->clj option-data)]
            (if terms
              (if (url-allowed? blacklist)
                (do (dispatch-sync [:initialize])
                    (let [attrs (clj->js {"className" "watchlist-wrapper"})
                          app-root (.createDom goog.dom "div" attrs)]
                      (.appendChild (.querySelector js/document "body") app-root)
                      (reagent/render [simple-example] app-root)))
                (inspect :blacklist-denied blacklist))
              (inspect :no-watchlist))))))


