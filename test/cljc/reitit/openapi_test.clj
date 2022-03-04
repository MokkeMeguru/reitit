(ns reitit.openapi-test
  (:require [clojure.test :refer [deftest is testing]]
            [muuntaja.core :as m]
            [reitit.coercion.malli :as malli]
            [reitit.coercion.schema :as schema]
            [reitit.coercion.spec :as spec]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as rrc]
            [reitit.openapi :as openapi]
            [reitit.swagger-ui :as swagger-ui]
            [schema.core :as s]
            [spec-tools.data-spec :as ds]
            [clojure.data.json]
            [ring.util.response :as response]))

(def app
  (ring/ring-handler
   (ring/router
    ["/api"
     {:openapi {:id ::math}}

     ["/openapi.json"
      {:get {:no-doc true
             :openapi {:info {:title "my-api"}}
             :handler (openapi/create-openapi-handler)}}]

     ["/spec" {:coercion spec/coercion}
      ["/plus/:z"
       {:patch {:summary "patch"
                :handler (constantly {:status 200})}
        :options {:summary "options"
                  :middleware [{:data {:openapi {:responses {200 {:description "200"}}}}}]
                  :handler (constantly {:status 200})}
        :get {:summary "plus"
              :parameters {:query {:x int?, :y int?}
                           :path {:z int?}}
              :responses {200 {:content {"application/json" {:total int?}}}
                          500 {:description "fail"}}
              :handler (fn [{{{:keys [x y]} :query
                              {:keys [z]} :path} :parameters}]
                         ;; NOTE need to contain content-type
                         (response/content-type
                          {:status 200, :body {:total (+ x y z)}}
                          "application/json"))}
        :post {:summary "plus with body"
               :parameters {:body (ds/maybe [int?])
                            :path {:z int?}}
               :responses {200 {:content {"application/json" {:total int?}}}
                           500 {:description "fail"}}
               :handler (fn [{{{:keys [z]} :path
                               xs :body} :parameters}]
                          (response/content-type
                           {:status 200, :body {:total (+ (reduce + xs) z)}}
                           "application/json"))}}]]
     ;; TODO  malli support
     ;; ["/malli" {:coercion malli/coercion}
     ;;  ["/plus/*z"
     ;;   {:get {:summary "plus"
     ;;          :parameters {:query [:map [:x int?] [:y int?]]
     ;;                       :path [:map [:z int?]]}
     ;;          :openapi {:responses {400 {:schema {:type "string"}
     ;;                                     :description "kosh"}}}
     ;;          :responses {200 {:body [:map [:total int?]]}
     ;;                      500 {:description "fail"}}
     ;;          :handler (fn [{{{:keys [x y]} :query
     ;;                          {:keys [z]} :path} :parameters}]
     ;;                     {:status 200, :body {:total (+ x y z)}})}
     ;;    :post {:summary "plus with body"
     ;;           :parameters {:body [:maybe [:vector int?]]
     ;;                        :path [:map [:z int?]]}
     ;;           :openapi {:responses {400 {:schema {:type "string"}
     ;;                                      :description "kosh"}}}
     ;;           :responses {200 {:body [:map [:total int?]]}
     ;;                       500 {:description "fail"}}
     ;;           :handler (fn [{{{:keys [z]} :path
     ;;                           xs :body} :parameters}]
     ;;                      {:status 200, :body {:total (+ (reduce + xs) z)}})}}]]

     ;; TODO schema supoort
     ;; ["/schema" {:coercion schema/coercion}
     ;;  ["/plus/*z"
     ;;   {:get {:summary "plus"
     ;;          :parameters {:query {:x s/Int, :y s/Int}
     ;;                       :path {:z s/Int}}
     ;;          :openapi {:responses {400 {:schema {:type "string"}
     ;;                                     :description "kosh"}}}
     ;;          :responses {200 {:body {:total s/Int}}
     ;;                      500 {:description "fail"}}
     ;;          :handler (fn [{{{:keys [x y]} :query
     ;;                          {:keys [z]} :path} :parameters}]
     ;;                     {:status 200, :body {:total (+ x y z)}})}
     ;;    :post {:summary "plus with body"
     ;;           :parameters {:body (s/maybe [s/Int])
     ;;                        :path {:z s/Int}}
     ;;           :openapi {:responses {400 {:schema {:type "string"}
     ;;                                      :description "kosh"}}}
     ;;           :responses {200 {:body {:total s/Int}}
     ;;                       500 {:description "fail"}}
     ;;           :handler (fn [{{{:keys [z]} :path
     ;;                           xs :body} :parameters}]
     ;;                      {:status 200, :body {:total (+ (reduce + xs) z)}})}}]]
     ]

    {:data {:middleware [openapi/openapi-feature
                         rrc/coerce-exceptions-middleware
                         rrc/coerce-request-middleware
                         rrc/coerce-response-middleware]}})))

(require '[fipp.edn])
(deftest openapi-test
  (testing "endpoints work"
    (testing "spec"
      (is (= {:body {:total 6}, :status 200, :headers {"Content-Type" "application/json"}}
             (app {:request-method :get
                   :uri "/api/spec/plus/3"
                   :query-params {:x "2", :y "1"}})))
      ;; (is (= {:body {:total 7}, :status 200, :headers {"Content-Type" "application/json"}}
      ;;        (app {:request-method :post
      ;;              :uri "/api/spec/plus/3"
      ;;              :body-params [1 3]})))
      )
    ;; (testing "schema"
    ;;   (is (= {:body {:total 6}, :status 200}
    ;;          (app {:request-method :get
    ;;                :uri "/api/schema/plus/3"
    ;;                :query-params {:x "2", :y "1"}}))))
    )
  (testing "openapi-spec"
    (let [spec (:body (app {:request-method :get
                            :uri "/api/openapi.json"}))
          expected {:x-id #{::math}
                    :openapi "3.1.0"
                    :info {:title "my-api"}
                    :paths {"/api/spec/plus/{z}" {:patch {:parameters []
                                                          :summary "patch"
                                                          :responses {:default {:description ""}}}
                                                  :options {:parameters []
                                                            :summary "options"
                                                            :responses {200 {:description "200"}}}
                                                  :get {:parameters [{:in "query"
                                                                      :name "x"
                                                                      :description ""
                                                                      :required true
                                                                      :schema
                                                                      {:type "integer"
                                                                       :format "int64"}}
                                                                     {:in "query"
                                                                      :name "y"
                                                                      :description ""
                                                                      :required true
                                                                      :schema
                                                                      {:type "integer"
                                                                       :format "int64"}}
                                                                     {:in "path"
                                                                      :name "z"
                                                                      :description ""
                                                                      :required true
                                                                      :schema
                                                                      {:type "integer"
                                                                       :format "int64"}}]
                                                        :responses {200 {:content
                                                                         {"application/json"
                                                                          {:schema {:type "object"
                                                                                    :properties {"total" {:format "int64"
                                                                                                          :type "integer"}}
                                                                                    :required ["total"]}}}}
                                                                    500 {:description "fail"}}
                                                        :summary "plus"}
                                                  :post {:parameters [{:name "array"
                                                                       :in :body
                                                                       :description ""
                                                                       :required false
                                                                       :schema
                                                                       {:type "array", :items {:type "integer", :format "int64"}}}
                                                                      {:name "z"
                                                                       :in "path"
                                                                       :description ""
                                                                       :required true
                                                                       :schema {:type "integer", :format "int64"}}]
                                                         :responses {200 {:content
                                                                          {"application/json"
                                                                           {:schema {:properties {"total" {:format "int64"
                                                                                                           :type "integer"}}
                                                                                     :required ["total"]
                                                                                     :type "object"}}}}
                                                                     500 {:description "fail"}}
                                                         :summary "plus with body"}}
                            ;; "/api/malli/plus/{z}" {:get {:parameters [{:in "query"
                            ;;                                            :name :x
                            ;;                                            :description ""
                            ;;                                            :required true
                            ;;                                            :type "integer"
                            ;;                                            :format "int64"}
                            ;;                                           {:in "query"
                            ;;                                            :name :y
                            ;;                                            :description ""
                            ;;                                            :required true
                            ;;                                            :type "integer"
                            ;;                                            :format "int64"}
                            ;;                                           {:in "path"
                            ;;                                            :name :z
                            ;;                                            :description ""
                            ;;                                            :required true
                            ;;                                            :type "integer"
                            ;;                                            :format "int64"}]
                            ;;                              :responses {200 {:schema {:type "object"
                            ;;                                                        :properties {:total {:format "int64"
                            ;;                                                                             :type "integer"}}
                            ;;                                                        :required [:total]}
                            ;;                                               :description ""}
                            ;;                                          400 {:schema {:type "string"}
                            ;;                                               :description "kosh"}
                            ;;                                          500 {:description "fail"}}
                            ;;                              :summary "plus"}
                            ;;                        :post {:parameters [{:in "body"
                            ;;                                             :name "body"
                            ;;                                             :description ""
                            ;;                                             :required false
                            ;;                                             :schema {:type "array"
                            ;;                                                      :items {:type "integer"
                            ;;                                                              :format "int64"}
                            ;;                                                      :x-nullable true}}
                            ;;                                            {:in "path"
                            ;;                                             :name :z
                            ;;                                             :description ""
                            ;;                                             :type "integer"
                            ;;                                             :required true
                            ;;                                             :format "int64"}]
                            ;;                               :responses {200 {:description ""
                            ;;                                                :schema {:properties {:total {:format "int64"
                            ;;                                                                              :type "integer"}}
                            ;;                                                         :required [:total]
                            ;;                                                         :type "object"}}
                            ;;                                           400 {:schema {:type "string"}
                            ;;                                                :description "kosh"}
                            ;;                                           500 {:description "fail"}}
                            ;;                               :summary "plus with body"}}
                            ;; "/api/schema/plus/{z}" {:get {:parameters [{:description ""
                            ;;                                             :format "int32"
                            ;;                                             :in "query"
                            ;;                                             :name "x"
                            ;;                                             :required true
                            ;;                                             :type "integer"}
                            ;;                                            {:description ""
                            ;;                                             :format "int32"
                            ;;                                             :in "query"
                            ;;                                             :name "y"
                            ;;                                             :required true
                            ;;                                             :type "integer"}
                            ;;                                            {:in "path"
                            ;;                                             :name "z"
                            ;;                                             :description ""
                            ;;                                             :type "integer"
                            ;;                                             :required true
                            ;;                                             :format "int32"}]
                            ;;                               :responses {200 {:description ""
                            ;;                                                :schema {:additionalProperties false
                            ;;                                                         :properties {"total" {:format "int32"
                            ;;                                                                               :type "integer"}}
                            ;;                                                         :required ["total"]
                            ;;                                                         :type "object"}}
                            ;;                                           400 {:schema {:type "string"}
                            ;;                                                :description "kosh"}
                            ;;                                           500 {:description "fail"}}
                            ;;                               :summary "plus"}
                            ;;                         :post {:parameters [{:in "body"
                            ;;                                              :name "body"
                            ;;                                              :description ""
                            ;;                                              :required false
                            ;;                                              :schema {:type "array"
                            ;;                                                       :items {:type "integer"
                            ;;                                                               :format "int32"}
                            ;;                                                       :x-nullable true}}
                            ;;                                             {:in "path"
                            ;;                                              :name "z"
                            ;;                                              :description ""
                            ;;                                              :type "integer"
                            ;;                                              :required true
                            ;;                                              :format "int32"}]
                            ;;                                :responses {200 {:description ""
                            ;;                                                 :schema {:properties {"total" {:format "int32"
                            ;;                                                                                :type "integer"}}
                            ;;                                                          :additionalProperties false
                            ;;                                                          :required ["total"]
                            ;;                                                          :type "object"}}
                            ;;                                            400 {:schema {:type "string"}
                            ;;                                                 :description "kosh"}
                            ;;                                            500 {:description "fail"}}
                            ;;                                :summary "plus with body"}}
                            }}]
      (is (= expected spec))

      (testing "ring-async openapi-spec"
        (let [response* (atom nil)
              respond (partial reset! response*)]
          (app {:request-method :get
                :uri "/api/openapi.json"} respond (fn [_] (is false)))
          (is (= expected (:body @response*))))))))

(defn spec-paths [app uri]
  (-> {:request-method :get, :uri uri} app :body :paths keys))

(deftest multiple-openapi-apis-test
  (let [ping-route ["/ping" {:get (constantly "ping")}]
        spec-route ["/openapi.json"
                    {:get {:no-doc true
                           :handler (openapi/create-openapi-handler)}}]
        app (ring/ring-handler
             (ring/router
              [["/common" {:openapi {:id #{::one ::two}}}
                ping-route]

               ["/one" {:openapi {:id ::one}}
                ping-route
                spec-route]

               ["/two" {:openapi {:id ::two}}
                ping-route
                spec-route
                ["/deep" {:openapi {:id ::one}}
                 ping-route]]
               ["/one-two" {:openapi {:id #{::one ::two}}}
                spec-route]]))]
    (is (= ["/common/ping" "/one/ping" "/two/deep/ping"]
           (spec-paths app "/one/openapi.json")))
    (is (= ["/common/ping" "/two/ping"]
           (spec-paths app "/two/openapi.json")))
    (is (= ["/common/ping" "/one/ping" "/two/ping" "/two/deep/ping"]
           (spec-paths app "/one-two/openapi.json")))))

(deftest openapi-ui-config-test
  (let [app (swagger-ui/create-swagger-ui-handler
             {:path "/"
              :config {:jsonEditor true}})]
    (is (= 302 (:status (app {:request-method :get, :uri "/"}))))
    (is (= 200 (:status (app {:request-method :get, :uri "/index.html"}))))
    (is (= {:jsonEditor true, :url "/openapi.json"}
           (->> {:request-method :get, :uri "/config.json"}
                (app) :body (m/decode m/instance "application/json"))))))

(deftest without-openapi-id-test
  (let [app (ring/ring-handler
             (ring/router
              [["/ping"
                {:get (constantly "ping")}]
               ["/openapi.json"
                {:get {:no-doc true
                       :handler (openapi/create-openapi-handler)}}]]))]
    (is (= ["/ping"] (spec-paths app "/openapi.json")))
    (is (= #{::openapi/default}
           (-> {:request-method :get :uri "/openapi.json"}
               (app) :body :x-id)))))

(deftest with-options-endpoint-test
  (let [app (ring/ring-handler
             (ring/router
              [["/ping"
                {:options (constantly "options")}]
               ["/pong"
                (constantly "options")]
               ["/openapi.json"
                {:get {:no-doc true
                       :handler (openapi/create-openapi-handler)}}]]))]
    (is (= ["/ping" "/pong"] (spec-paths app "/openapi.json")))
    (is (= #{::openapi/default}
           (-> {:request-method :get :uri "/openapi.json"}
               (app) :body :x-id)))))

(deftest all-parameter-types-test
  (let [app (ring/ring-handler
             (ring/router
              [["/parameters"
                {:post {:coercion spec/coercion
                        :parameters {:query {:q string?}
                                     :body {:b string?}
                                     :form {:f string?}
                                     :header {:h string?}
                                     :path {:p string?}}
                        :handler identity}}]
               ["/openapi.json"
                {:get {:no-doc true
                       :handler (openapi/create-openapi-handler)}}]]))
        spec (:body (app {:request-method :get, :uri "/openapi.json"}))]
    (is (= ["query" "body" "formData" "header" "path"]
           (map :in (get-in spec [:paths "/parameters" :post :parameters]))))))

;; (def app
;;   (ring/ring-handler
;;    (ring/router
;;     ["/api"
;;      {:openapi {:id ::math}}
;;      ["/openapi.json"
;;       {:get {:no-doc true
;;              :openapi {:info {:title "my-api"}}
;;              :handler (openapi/create-openapi-handler)}}]
;;      ["/spec" {:coercion spec/coercion}
;;       ["/plus/:z"
;;        {:patch {:summary "patch"
;;                 :handler (constantly {:status 200})}
;;         :get {:summary "plus"
;;               :parameters {:query {:x int?, :y int?}
;;                            :path {:z int?}}
;;               :responses {200 {:content {"application/json" {:total int?}}}
;;                           500 {:description "fail"}}
;;               :handler (fn [{{{:keys [x y]} :query
;;                               {:keys [z]} :path} :parameters}]
;;                          (response/content-type
;;                           {:status 200, :body {:total (+ x y z)}}
;;                           "application/json"))}}]]]
;;     {:data {:middleware [openapi/openapi-feature
;;                          rrc/coerce-exceptions-middleware
;;                          rrc/coerce-request-middleware
;;                          rrc/coerce-response-middleware]}})))

;; (require '[fipp.edn])
;; (deftest openapi-test
;;   (testing "endpoints work"
;;     (testing "spec"
;;       (is (= {:body {:total 6} :status 200 :headers {"Content-Type" "application/json"}}
;;              (app {:request-method :get
;;                    :uri "/api/spec/plus/3"
;;                    :query-params {:x "2" :y "1"}}))))
;;     ;; (testing "openapi-spec"
;;     ;;   (let [spec (:body (app {:request-method :get
;;     ;;                           :uri "/api/openapi.json"}))]))
;;     ))

;; (app {:request-method :get
;;       :uri "/api/spec/plus/3"
;;       :query-params {:x "2" :y "1"}})

(clojure.pprint/pprint
 (app {:request-method :get
       :uri "/api/openapi.json"}))
