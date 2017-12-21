(ns watchlist.core
  (:require [goog.dom]
            [hipo.core :as hipo]
            [common.dom :as dom]
            [watchlist.nodes :as nodes]))

(def state (atom {:matches []
                  :clicks {:term nil
                           :index -1}}))

(defn mod-clicks-over-nodes
  [{:keys [term index] :as clicks} term-clicked nodes]
  (if (= term-clicked term)
    (mod (inc index) (count nodes))
    0))

(defn term-clicked [term-clicked _]
  (let [term-nodes (filter #(= term-clicked (:term %)) (:matches @state))
        index (mod-clicks-over-nodes (:clicks @state) term-clicked term-nodes)]
    (nodes/scroll-to-node! (:node (nth term-nodes index)))
    (swap! state assoc :clicks {:term term-clicked
                                :index index})))

(defn display-match [group-term hits started]
  [:span {:class (apply str "watchlist-status-bar-item "
                        (-> hits first :groups))
          :on-mouseover (nodes/evt dom/add-class "watchlist-item-hover")
          :on-mouseout  (nodes/evt dom/del-class "watchlist-item-hover")
          :on-mousedown (nodes/evt dom/add-class "watchlist-item-click")
          :on-mouseup   (nodes/evt dom/del-class "watchlist-item-click")
          :on-click     (partial term-clicked group-term)
          :title        (str (- (.getTime (js/Date.)) started) " ms elapsed")}
   group-term
   [:span {:class "watchlist-status-bar-count"} (count hits)]])

(defn viewport-height-ratio []
  ;; buh
  ;; https://github.com/jquery/jquery/blob/d71f6a53927ad02d728503385d15539b73d21ac8/src/dimensions.js#L23-L29
  (let [coords {:doc-client-height (.. js/window -document -documentElement -clientHeight)
                :doc-offset-height (.. js/window -document -documentElement -offsetHeight)
                :doc-scroll-height (.. js/window -document -documentElement -scrollHeight)
                :bod-client-height (.-clientHeight (.querySelector js/document "body"))
                :bod-offset-height (.-offsetHeight (.querySelector js/document "body"))
                :bod-scroll-height (.-scrollHeight (.querySelector js/document "body"))}
        ;; Some coordinates can be zero?
        dims (remove #(zero? (val %)) coords)
        sorted (sort-by val dims)
        [dmin-k dmin-v] (first sorted)
        [dmax-k dmax-v] (last sorted)]
    (/ dmin-v dmax-v)))

;; The only site that seems to need this...
;; https://www.justinobeirne.com/google-maps-moat
;; is somehow blocking all canvas rendering anyway.
;; Consider nixing.
(defn doc-height []
  (let [ch (.-clientHeight (.querySelector js/document "body"))
        dh (.. js/window -document -documentElement -clientHeight)]
    (cond
      (pos? ch) ch
      (pos? dh) dh
      :else (js/alert "blip-bar unable to find non-zero document-height or client-document-height"))))

(defn draw-blip-bar [matches]
  (when-let [old-blip-bar (.querySelector js/document "#watchlist-blip-bar")]
    (.removeChild (.-parentElement old-blip-bar) old-blip-bar))
  (let [blip-bar (.createDom goog.dom "canvas" #js {"id" "watchlist-blip-bar"})]
    (set! (.-width blip-bar) 16)
    (set! (.-height blip-bar) (doc-height))
    (.appendChild (.querySelector js/document "body") blip-bar)
    (let [ratio (viewport-height-ratio)
          scroll-y (.-scrollY js/window)
          width (- (.-clientWidth blip-bar) 2)
          ctx (.getContext blip-bar "2d")]
      (doseq [{node :node} matches]
        (let [node-top (.-top (.getBoundingClientRect node))
              node-bgcolor (.getPropertyValue (.getComputedStyle js/window node) "background-color")
              start-y (.round js/Math (* (+ node-top scroll-y) ratio))]
          (set! (.-fillStyle ctx) "#999")
          (.fillRect ctx 1 start-y width 4)
          (set! (.-fillStyle ctx) node-bgcolor)
          (.fillRect ctx 1.5 (+ start-y 0.5) (- width 1) 3))))))

(defn statusbar [started styles]
  (when-let [matches (seq (:matches @state))]
    (draw-blip-bar matches)
    (let [els (hipo/create
               [:div {:id "watchlist-status-bar" :class "watchlist-emphasized"}
                (when styles
                  [:style {:type "text/css"} styles])
                [:span {:class "watchlist-status-bar-item" :id "watchlist-title"} "Watchlist"]
                (map (fn [[group-term hits]]
                       (display-match group-term hits started))
                     (sort-by key (group-by :term matches)))])]
      (.appendChild (.querySelector js/document "#watchlist-status-root") els))))

(declare observer)

(defn go [{:keys [styles] :as options}]
  ;; Accommodate fact that we may be re-drawing elements if the page has changed.
  (.disconnect @observer)
  (let [started (.getTime (js/Date.))]
    (when-let [old-wrapper (.querySelector js/document "#watchlist-wrapper")]
      (.removeChild (.-parentElement old-wrapper) old-wrapper))
    (let [matches (nodes/highlight-matches! (:parsed options))]
      (swap! state assoc :matches matches))
    (let [style    (.createDom goog.dom "style" #js {:type "text/css"} styles)
          app-root (.createDom goog.dom "div" #js {:id "watchlist-status-root"})
          wrapper  (.createDom goog.dom
                               "div"
                               #js {:id "watchlist-wrapper"}
                               style app-root)]
      (.appendChild (.querySelector js/document "body") wrapper)
      (statusbar started styles)
      (.observe @observer
                (.querySelector js/document "body")
                #js {:childList true
                     :attributes true
                     :characterData true
                     :subtree true}))))

(def ratio (atom nil))
(def observer (atom nil))

(defn url-allowed? [blacklist]
  (not (and (not (empty? blacklist))
            (re-find (re-pattern blacklist)
                     (.-URL js/document)))))

(defn ^:export init! [{:keys [terms blacklist styles] :as options}]
  (when (and terms (url-allowed? blacklist))
    ;; Observe mutations so we can re-draw if the page changes.
    (reset! ratio (viewport-height-ratio))
    (reset! observer (js/window.MutationObserver.
                      (fn [mutation-list observer]
                        (let [ratio' (viewport-height-ratio)]
                          (if (not= ratio' @ratio)
                            (do (reset! ratio ratio')
                                (go options)))))))
    (go options)))
