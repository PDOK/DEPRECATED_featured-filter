(ns featured-filter.core
  (:require [cheshire [factory :as jfac] [parse :as jparse]]
            [clj-time [format :as tf] [coerce :as tc]]
            [cognitect.transit :as transit]
            [clojure.walk :as walk])
  (:gen-class)
  (:import (com.fasterxml.jackson.core JsonParser JsonToken)
           (org.joda.time DateTimeZone DateTime)
           (java.io File)
           (clojure.lang MapEntry)))

;; 2015-02-26T15:48:26.578Z
(def ^{:private true} date-time-formatter (tf/formatters :date-time-parser))

(defn parse-time
  "Parses an ISO8601 date timestring to local date time"
  [datetimestring]
  (when-not (clojure.string/blank? datetimestring)
    (tf/parse date-time-formatter datetimestring)))

(defn map-to-feature [obj]
  (let [validity (parse-time (obj "_validity"))
        current-validity (parse-time (obj "_current_validity"))
        feature (cond-> obj
                        true (assoc "_validity" validity)
                        current-validity (assoc "_current_validity" current-validity))]
    feature))

(defn- parse-object [^JsonParser jp]
  (jparse/parse* jp identity nil nil))

(defn- read-meta-data [^JsonParser jp]
  (.nextToken jp)
  (loop [state {}
         currentName (.getCurrentName jp)]
    (if (= "features" currentName)
      state
      (let [obj (do (.nextToken jp) (parse-object jp))
            name (-> (doto jp .nextToken) .getCurrentName)]
        (recur (merge state {currentName obj}) name)))))

(defn- read-features [^JsonParser jp]
  (if (and (.nextToken jp) (not= (.getCurrentToken jp) JsonToken/END_ARRAY))
    (lazy-seq (cons (parse-object jp) (read-features jp)))
    []))

(defn- features-from-stream* [^JsonParser jp & overrides]
  (.nextToken jp)
  (when (= JsonToken/START_OBJECT (.getCurrentToken jp))
    (let [meta (read-meta-data jp)]
      ;; features should be array
      (when (= JsonToken/START_ARRAY (.nextToken jp))
        [meta (map map-to-feature (read-features jp))]))))

(defn features-from-stream [input-stream]
  "Parses until 'features' for state. Then returns vector [meta <lazy sequence of features>]."
  (let [reader (clojure.java.io/reader input-stream)
        factory jfac/json-factory
        parser (.createParser factory reader)
        meta-and-features (features-from-stream* parser)]
    meta-and-features))

(defn write-date-time
  "Represent DateTime in RFC3339 format string."
  [d]
  (.toString (.withZone ^DateTime d (DateTimeZone/forID "UTC"))))

(defn read-date-time
  "Read RFC3339 string to DateTime."
  [s]
  (DateTime/parse s))

(def writers
  {DateTime  (transit/write-handler (constantly "DateTime") write-date-time)})

(def readers
  {"DateTime" (transit/read-handler read-date-time)})


(def statefile ".state/validity.json")

(defn write-state [state]
  (clojure.java.io/make-parents statefile)
  (with-open [out (clojure.java.io/output-stream statefile)]
    (let [writer (transit/writer out :json {:handlers writers})]
      (transit/write writer state))))

(defn read-state []
  (if (.exists (File. statefile))
    (with-open [in (clojure.java.io/input-stream statefile)]
      (let [reader (transit/reader in :json {:handlers readers})]
        (transit/read reader)))
    {}))

(defn update-state [state dataset feature]
  (let [collection (get feature "_collection")
        id (get feature "_id")
        validity (get feature "_validity")]
    (vswap! state #(assoc % [dataset collection id] validity))))

(defn get-validity [state dataset feature]
  (let [collection (get feature "_collection")
        id (get feature "_id")
        action (get feature "_action")
        validity (get feature "_validity")]
    (get @state [dataset collection id] validity)))

(defn create-filter [required]
  (let [attributes (clojure.string/split required #",")]
    (fn [feature]
      (or (= "new" (get feature "_action"))
          (some #(contains? feature %) attributes)))))

;; 2015-02-26T15:48:26.578Z
(def ^{:private true} date-time-formatter (tf/formatters :date-time))

(defn texify-validity [validity]
  (tf/unparse date-time-formatter (tc/to-date-time validity)))

(defn texify-feature [feature]
  (walk/prewalk
    (fn [e]
      (cond (and (instance? MapEntry e) (= "_validity" (first e)))
            (MapEntry. "_validity" (texify-validity (second e)))
            (and (instance? MapEntry e) (= "_current_validity" (first e)))
            (MapEntry. "_current_validity" (texify-validity (second e)))
            :else e))
    feature))

(defn write-features [filename header features]
  (with-open [w (clojure.java.io/output-stream filename)]
    (let [package (assoc header :features (map texify-feature features))]
      (cheshire.core/generate-stream package (clojure.java.io/writer w)))))

(defn -main
  "I don't do a whole lot ... yet."
  [file required & args]
  (let [state (volatile! (read-state))
        emit? (create-filter required)]
    (with-open [in (clojure.java.io/reader file)]
      (let [[meta features] (features-from-stream in)
            dataset (get meta "dataset")
            emit (filter emit? features)
            altered-current-validity (doall (map (fn [f] (if (not= "new" (get f "_action"))
                                                           (assoc f "_current_validity"
                                                                    (get-validity state dataset f))
                                                           f))
                                                 emit))
            _ (dorun (map (partial update-state state dataset) emit))
            _ (write-features (str file ".filtered.json") meta altered-current-validity)
            _ (write-state @state)]
        ))))
