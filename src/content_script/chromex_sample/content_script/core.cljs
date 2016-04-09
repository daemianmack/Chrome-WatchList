(ns chromex-sample.content-script.core
  (:require-macros [reagent.ratom :refer [reaction]]
                   [content-script.chromex-sample.content-script.macros :refer [inspect]])
  (:require [reagent.core :as reagent]
            [goog.dom]
            [clojure.string :as s]
            [re-frame.core :refer [register-handler
                                   path
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]))

(extend-type js/NodeList
  ISeqable
  (-seq [array] (array-seq array 0)))


(defn node-seq
  []
  (let [tree (.createTreeWalker js/document
                                (.-body js/document)
                                (.-SHOW_TEXT js/NodeFilter))]
    (take-while some? (repeatedly #(.nextNode tree)))))

(defn classify
  [s]
  (str "<mark class=\"watchlist-highlight\">" s "</mark>"))

(register-handler                 
 :initialize                     
 (fn
   [db _]
   (let [terms #"dynamically|created|who"]
     (let [nodes   (filterv #(re-find terms (.-textContent %)) (node-seq))
           matches (for [node nodes
                         :let [parent (.-parentNode node)
                               html   (.-innerHTML parent)
                               markup (s/replace html terms classify)
                               _      (set! (.-innerHTML parent) markup)]
                         match (re-seq terms html)]
                     {:term match :node parent})]
       (merge db {:started (.getTime (js/Date.))
                  :matches matches})))))

(register-sub
 :initialize
 (fn [db _]
   (reaction @db)))

(defn display-match
  [group-term hits]
  [:span {:class "watchlist-status-bar-item"}
   group-term ":" (count hits)])

(defn statusbar
  []
  (let [matches (subscribe [:initialize])]
    (fn []
      [:div {:id "watchlist-status-bar"
             :class :loading
             :title (str (- (.getTime (js/Date.)) (:started @matches))
                         " ms elapsed")}
       [:span {:class "watchlist-status-bar-item"}
        [:a {:href "/BARF"}]]
       [:span {:class "watchlist-status-bar-separator"}]
       (for [[group-term hits] (group-by :term (:matches @matches))]
         ^{:key group-term} [display-match group-term hits])])))

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
                      (reagent/render [statusbar] app-root)))
                (inspect :blacklist-denied blacklist))
              (inspect :no-watchlist))))))


