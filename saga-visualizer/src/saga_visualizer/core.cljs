(ns saga-visualizer.core
  (:require [clojure.browser.repl :as repl]
            [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(defonce request-json (reagent/atom {}))
(defonce current-request (reagent/atom (reagent/atom nil)))
(defonce dragging-request (reagent/atom nil))
(defonce lines (reagent/atom {}))
(defonce node-positions (reagent/atom {}))

(defn json-ui []
  [:div
   [:h3 "Request JSON"]
   [:textarea
    {:value     (.stringify js/JSON (clj->js (map #(dissoc % :uuid) (vals @request-json))))
     :read-only true
     :style
                {:width  "50%"
                 :height "100px"}}]])

(defonce request-count (reagent/atom 0))

(defn request-icon []
  [:div {
         :on-click #(swap! request-count inc)
         :style    {:width            30
                    :height           20
                    :top              100
                    :left             50
                    :border-style     "solid"
                    :border-color     "black"
                    :background-color "white"}}])

(defn position [elem]
  (let [parent-pos (@node-positions (.-id (.-parentElement elem)))
        parent-x (:x parent-pos)
        parent-y (:y parent-pos)
        parent-width (.-offsetWidth (.-parentElement elem))
        parent-height (.-offsetHeight (.-parentElement elem))
        x (.-offsetLeft elem)
        y (.-offsetTop elem)
        width (.-offsetWidth elem)
        height (.-offsetHeight elem)]
    {:x (cond
          (< x 0) parent-x
          (> (+ x width) parent-width) (+ parent-x parent-width)
          :else (+ parent-x (/ parent-width 2)))
     :y (cond
          (< y 0) parent-y
          (> (+ y height) parent-height) (+ parent-y parent-height)
          :else (+ parent-y (/ parent-height 2)))})
  )


(defn square [x]
  (* x x))

(defn degrees [radians]
  (/ (* radians 180) Math/PI))

(defn line [start end]
  (let [start-pos (position (.getElementById js/document start))
        end-pos (position (.getElementById js/document end))
        length (Math/sqrt
                 (+ (square (- (:y end-pos) (:y start-pos)))
                    (square (- (:x end-pos) (:x start-pos)))))
        rad (Math/atan
              (/ (- (:x start-pos) (:x end-pos))
                 (- (:y end-pos) (:y start-pos))))
        deg (degrees rad)]
    [:div {:style (into {:width            1
                         :height           length
                         :left             (/ (+ (:x start-pos) (:x end-pos)) 2)
                         :top              (/ (- (+ (:y start-pos) (:y end-pos)) length) 2)
                         :position         "absolute"
                         :background-color "black"}
                        [{:transform (str "rotate(" deg "deg)")}
                         {:-webkit-transform (str "rotate(" deg "deg)")}
                         {:-ms-transform (str "rotate(" deg "deg)")}]
                        )}]))

(defn request-field [field]
  [:input {:type      "text"
           :size      "50"
           :on-change (fn [evt]
                        (let [content (.-value (.-target evt))]
                          (swap! @current-request assoc field content)
                          (swap! request-json assoc (:uuid (deref @current-request)) (deref @current-request))))
           :value     (field (deref @current-request))}])

(defn request-detail []
  [:div
   [:h3 "Request Details"]
   [:div
    [:div
     [:span "Request ID"]]
    [:div
     [request-field :id]]
    [:div
     [:span "Service Name"]]
    [:div
     [request-field :serviceName]]
    [:div
     [:span "Path"]]
    [:div
     [request-field :path]]]])


(defn connector [request pos visible?]
  (reagent/with-let [border-color (reagent/atom "black")
                     id (.toString (random-uuid))]
                    [:div {:id             id
                           :on-mouse-over  #(reset! border-color "green")
                           :on-mouse-leave #(reset! border-color "black")
                           :on-mouse-down  (fn [evt]
                                             (reset! dragging-request {:connector-id id
                                                                       :request      @request})
                                             )
                           :on-mouse-up    (fn [evt]
                                             (when (not (nil? dragging-request))
                                               (swap! request assoc :parents (set (filter #(not= % (:id @request))
                                                                                          (conj (:parents @request) (:id (:request @dragging-request))))))
                                               (swap! request-json assoc (:uuid @request) @request)
                                               (swap! lines assoc (str (:id (:request @dragging-request)) "-" (:id @request))
                                                      {:start-pos (:connector-id @dragging-request)
                                                       :end-pos   id})
                                               (reset! dragging-request nil)))
                           :style          {:left             (:x pos)
                                            :top              (:y pos)
                                            :width            10
                                            :height           10
                                            :border-style     "solid"
                                            :border-color     @border-color
                                            :position         "absolute"
                                            :visibility       (if visible?
                                                                "visible"
                                                                "hidden")
                                            :background-color "white"}}]))

(defn request-node []
  (reagent/with-let [uuid (.toString (random-uuid))
                     request (reagent/atom {:uuid uuid :serviceName "some service" :parents (set '())})
                     selected? (reagent/atom false)
                     mouse-over? (reagent/atom false)
                     border-color (reagent/atom "black")
                     z-index (reagent/atom '())
                     start-pos (reagent/atom {:x 100 :y 150})
                     current-pos (reagent/atom {:x 100 :y 150})
                     ignored (swap! node-positions assoc uuid @current-pos)]
                    [:div {:id             uuid
                           :on-mouse-over  #(reset! mouse-over? true)
                           :on-mouse-leave #(reset! mouse-over? false)
                           :style          (into {:width            100
                                                  :height           64
                                                  :left             (:x @current-pos)
                                                  :top              (:y @current-pos)
                                                  :text-align       "center"
                                                  :vertical-align   "middle"
                                                  :background-color "white"
                                                  :position         "absolute"}
                                                 (when (not (empty? @z-index))
                                                   [{:z-index (first @z-index)}]))}
                     [:div {:on-mouse-over  #(reset! border-color "red")
                            :on-mouse-up    (fn [_]
                                              (reset! selected? false)
                                              (reset! current-request request)
                                              (swap! z-index #(drop 1 %)))
                            :on-mouse-leave (fn [_]
                                              (reset! selected? false)
                                              (swap! z-index #(drop 1 %))
                                              (reset! border-color "black"))
                            :on-mouse-down  (fn [evt]
                                              (reset! selected? true)
                                              (when (empty? @z-index)
                                                (swap! z-index conj (.-zIndex (.-target evt))))
                                              (swap! z-index conj 2147483647)
                                              (swap! start-pos assoc :x (.-clientX evt) :y (.-clientY evt)))
                            :on-mouse-move  (fn [evt]
                                              (when @selected?
                                                (swap! current-pos assoc
                                                       :x (- (+ (:x @current-pos) (.-clientX evt)) (:x @start-pos))
                                                       :y (- (+ (:y @current-pos) (.-clientY evt)) (:y @start-pos)))
                                                (swap! start-pos assoc :x (.-clientX evt) :y (.-clientY evt))
                                                (swap! node-positions assoc uuid {:x (.-offsetLeft (.-parentElement (.-target evt)))
                                                                                  :y (.-offsetTop (.-parentElement (.-target evt)))})))
                            :style          {:width            100
                                             :height           64
                                             :text-align       "center"
                                             :vertical-align   "middle"
                                             :border-style     "solid"
                                             :border-color     @border-color
                                             :background-color "white"}}
                      (:serviceName @request)]
                     [connector request {:x 43 :y -7} @mouse-over?]
                     [connector request {:x 43 :y 59} @mouse-over?]
                     [connector request {:x -10 :y 26} @mouse-over?]
                     [connector request {:x 95 :y 26} @mouse-over?]]
                    ))

(defn draw-lines []
  (if (not (empty? @lines))
    (for [l (vals @lines)]
      [line (:start-pos l) (:end-pos l)])
    [:div]))

(defn graph-ui []
  (into (into
          [:div {:id "request_graph"}
           [:h2 "Graph"]
           [:div {:style {:width            640
                          :height           200
                          :background-color "gray"}}
            [request-icon]]
           [request-detail]]
          (repeat @request-count [request-node]))
        (draw-lines)))


(defn interface []
  [:div
   [graph-ui]
   [json-ui]])

(reagent/render-component [interface]
                          (.getElementById js/document "dynamic"))
