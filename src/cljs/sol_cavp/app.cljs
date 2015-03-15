(ns sol-cavp.app
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent-forms.core :refer [bind-fields]]))

(enable-console-print!) ;; enable print at web inspector console

(def google-spreadsheet-url "https://docs.google.com/spreadsheets/d/1SIN6GlkVozeZCe-MxjUXDV2fpnlWvKmKWaOdZmNbWGk/pubhtml")

(def master-form (atom {}))

(def lock (new js/Auth0Lock "6I7t0JdYGGTFoxeB8gmW0LWn7LciMm7G" "sol-cavp.auth0.com")) ;; auth0 lock 

(def user (atom {:logged-in false :profile {}})) ;; profile

(defn init-table-top []
  (let [enabled?  (fn [x] (when (= (:enabled x) "TRUE") x))
        with-data (fn [data] (filter enabled? (:elements (:2015 (js->clj data :keywordize-keys true)))))
        callback  (fn [data]            
                    (reset! master-form (with-data data)))]
    (.init js/Tabletop (js-obj "key" google-spreadsheet-url "callback" callback "simpleSheet" false))))

(defn row [label & body]
  [:div.row
   [:div {:class "col-md-4 col-lg-4"} [:label label]]
   [:div {:class "col-md-8 col-lg-8"} body]])

(defn date-picker [setdate]
  (let [today (.format (js/moment (new js/Date)) "MM/DD/YYYY")]
    (fn [] [:input#set-date {:type "text" :placeholder  "mm/dd/yyyy" :size 11 :value today}])))

(defn date-picker-did-mount []
  (.ready (js/$ js/document) 
          (fn [] (.datepicker (js/$ "#set-date") (clj->js {:format "mm/dd/yyyy"})))))

(defn date-picker-component []
  (reagent/create-class {:render date-picker
                         :component-did-mount date-picker-did-mount}))

(defn return-component [type]
  (let [input-component (fn [x] [:input.form-control {        
                                                      :type x 
                                                      :on-change #(print (-> % .-target .-value))}])]
    (case type
      "datepicker" [date-picker-component]
      [input-component type])))

(defn text-area []
  [:div {:class "col-md-6 col-lg-6"} 
   [:p.text-center "notes"]
   [:textarea.form-control {:rows 24}]])

(defn login-btn []

  (let [state (atom {:auth false})
        logged-in? #(if (false? (:auth @state)) "login" "logout")
        in-out (fn [] (if (false? (:auth @state))
                              (.show lock (clj->js {:popup true}) 
                                     (fn [err profile token]
                                       (if (not (nil? err))
                                         (print err)
                                         (do 
                                           (.setItem js/localStorage "userToken" token) ;; save the JWT token.
                                           ;; (swap! assoc :profile (js->clj profile :keywordize-keys true))
                                           (swap! state assoc :auth true)
                                           (print profile)
                                           (print token)))))
                              (do 
                               (.removeItem js/localStorage "userToken")
                               ;; (aset js/document "location" "href" "/")
                               (swap! state assoc :auth false))))] 

    (fn []      
      (print (str "user logged in :: " @state))
      [:label {:for "btn-login"} "Are you a volunteer? "
       [:button {:class "btn btn-default" :id "btn-login" :type "submit" :on-click in-out}
        (logged-in?)]])))

(defn part-1 []
  (let []
    (fn []
      (println (count @master-form))     
      [:div 
       [:div.header]
       [:h5.text-right
        [login-btn]
        [:br]
        "part " [:span.badge "1"]]          
       [:br]
       [:div.row
        [:div {:class "col-md-6 col-lg-6"} 
         (for [c @master-form]
           ^{:key c} [row (:field-name c) [:div {:key (str "part-1-" (:rowNumber c))} 
                                           [return-component (:type c)]]])]
        [text-area]]]))) 

(defn init []
  (init-table-top)
  (reagent/render-component [part-1] (.getElementById js/document "mount")))
