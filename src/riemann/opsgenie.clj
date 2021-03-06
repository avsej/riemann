(ns riemann.opsgenie
  "Forwards events to OpsGenie"
  (:require [clj-http.client :as client])
  (:require [cheshire.core :as json]))

(def ^:private alerts-url
  "https://api.opsgenie.com/v1/json/alert")

(defn- post
 "Post to OpsGenie"
 [url body]
  (client/post url
                {:body body
                 :socket-timeout 5000
                 :conn-timeout 5000
                 :content-type :json
                 :accept :json
                 :throw-entire-message? true}))

(defn- message
  "Generate message based on event"
  [event]
  (str (:host event) " "
       (:service event) " is "
       (:state event) " ("
       (:metric event) ")"))

(defn- api-alias
  "Generate OpsGenie alias based on event"
  [event]
  (hash (str (:host event) " "
       (:service event))))

(defn- create-alert
  "Create alert in OpsGenie"
  [api-key event recipients]
  (post alerts-url (json/generate-string
                    {:message (message event)
                     :apiKey api-key
                     :alias (api-alias event)
                     :recipients recipients})))
(defn- close-alert
  "Close alert in OpsGenie"
  [api-key event]
  (post (str alerts-url "/close")
        (json/generate-string
          {:apiKey api-key
           :alias (api-alias event)})))

(defn opsgenie
  "Creates an OpsGenie adapter. Takes your OG service key, and returns a map of
  functions which trigger and resolve events. clojure/hash from event host and service
  will be used as the alias.

  (let [og (opsgenie \"my-service-key\" \"recipient@example.com\")]
    (changed-state
      (where (state \"ok\") (:resolve og))
      (where (state \"critical\") (:trigger og))))"
  [service-key recipients]
  {:trigger     #(create-alert service-key % recipients)
   :resolve     #(close-alert service-key %)})
