(ns chromex-sample.content-script.core
  (:require-macros [reagent.ratom :refer [reaction]]
                   [content-script.chromex-sample.content-script.macros :refer [inspect]])
  (:require [reagent.core :as reagent]
            [goog.dom]
            [clojure.string :as s]
            [re-frame.core :refer [register-handler
                                   register-sub
                                   dispatch
                                   subscribe]]))

(defn node-seq []
  (let [tree (.createTreeWalker js/document
                                (.-body js/document)
                                (.-SHOW_TEXT js/NodeFilter))]
    (take-while some? (repeatedly #(.nextNode tree)))))

(defn classify [s]
  (str "<mark class=\"watchlist-highlight\">" s "</mark>"))

(register-sub
 :db
 (fn [db _]
   (reaction @db)))

(register-handler :initialize
 (fn [db [_ option-data]]
   (let [terms (re-pattern (:terms option-data))]
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

(defn display-match [group-term hits started]
  [:span {:class "watchlist-status-bar-item"
          :title (str (- (.getTime (js/Date.))
                         started)
                      " ms elapsed")}
   group-term ":" (count hits)])

(defn statusbar []
  (let [db (subscribe [:db])]
    (fn []
      [:div {:id "watchlist-status-bar"
             :class :loading
             :title (str (- (.getTime (js/Date.)) (:started @db))
                         " ms elapsed")}
       [:span {:class "watchlist-status-bar-item"}
        [:a {:href "/BARF"}]]
       [:span {:class "watchlist-status-bar-separator"}]
       (doall
        (for [[group-term hits] (group-by :term (:matches @db))]
          ^{:key group-term} [display-match group-term hits (:started @db)]))])))

(defn url-allowed? [blacklist]
  (not (and (some? blacklist)
            (re-find (re-pattern blacklist)
                     (.-URL js/document)))))

(defn ^:export init! []
  (.get js/chrome.storage.sync "watchlist"
        #(when-let [options-data (:watchlist (js->clj % :keywordize-keys true))]
           (dispatch [:initialize options-data])
           (let [{:keys [terms blacklist]} options-data]
             (when (and terms (url-allowed? blacklist))
               (do (let [attrs (clj->js {"className" "watchlist-wrapper"})
                         app-root (.createDom goog.dom "div" attrs)]
                     (.appendChild (.querySelector js/document "body") app-root)
                     (reagent/render [statusbar] app-root))))))))
