(ns chromex-sample.content-script.core
  (:require-macros [reagent.ratom :refer [reaction]]
                   [content-script.chromex-sample.content-script.macros :refer [inspect]])
  (:require [reagent.core :as reagent]
            [goog.dom]
            [chromex-sample.content-script.nodes :as nodes]
            [re-frame.core :refer [register-handler
                                   register-sub
                                   dispatch
                                   subscribe]]))

(register-sub :matches (fn [db _] (reaction (:matches @db))))
(register-sub :started (fn [db _] (reaction (:started @db))))

(register-handler
 :started
 (fn [db _]
   (assoc db :started (.getTime (js/Date.)))))

(register-handler
 :initialize
 (fn [db [_ option-data]]
   (let [matches (nodes/highlight-matches! (:terms option-data))]
     (assoc db :matches matches))))

(register-handler
 :clicked
 (fn [{:keys [clicks matches] :as db} [_ term-clicked]]
   (let [streak (if (= term-clicked (:term clicks))
                  (inc (:streak clicks))
                  0)
         matches (filter #(= term-clicked (:term %)) matches)
         node (:node (nth matches (mod streak (count matches))))]
     (nodes/scroll-to-node! node)
     (merge db {:clicks {:term term-clicked
                         :streak streak}}))))

(defn display-match [group-term hits started]
  (let [this (reagent/current-component)]
    [:span {:class "watchlist-status-bar-item"
            :on-mouse-over (nodes/evt nodes/add-class this "watchlist-item-hover")
            :on-mouse-out  (nodes/evt nodes/del-class this "watchlist-item-hover")
            :on-mouse-down (nodes/evt nodes/add-class this "watchlist-item-click")
            :on-mouse-up   (nodes/evt nodes/del-class this "watchlist-item-click")
            :on-click      #(dispatch [:clicked group-term])
            :title         (str (- (.getTime (js/Date.)) started) " ms elapsed")}
     group-term
     [:span {:class "watchlist-status-bar-count"} (count hits)]]))

(defn statusbar []
  (let [matches (subscribe [:matches])
        started (subscribe [:started])]
    (fn []
      (when (seq @matches)
        [:div {:id "watchlist-status-bar" :class "watchlist-emphasized"}
         [:span {:class "watchlist-status-bar-item"} "Watchlist"]
         [:span {:class "watchlist-status-bar-separator"}]
         (doall
          (for [[group-term hits] (sort-by key (group-by :term @matches))]
            ^{:key group-term} [display-match group-term hits @started]))]))))

(defn url-allowed? [blacklist]
  (not (and (some? blacklist)
            (re-find (re-pattern blacklist)
                     (.-URL js/document)))))

(defn ^:export init! []
  (dispatch [:started])
  (.get js/chrome.storage.sync "watchlist"
        #(when-let [options-data (:watchlist (js->clj % :keywordize-keys true))]
           (dispatch [:initialize options-data])
           (let [{:keys [terms blacklist]} options-data]
             (when (and terms (url-allowed? blacklist))
               (let [attrs (clj->js {"className" "watchlist-wrapper"})
                     app-root (.createDom goog.dom "div" attrs)]
                 (.appendChild (.querySelector js/document "body") app-root)
                 (reagent/render [statusbar] app-root)))))))
