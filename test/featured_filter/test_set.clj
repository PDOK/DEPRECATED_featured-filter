(ns featured-filter.test-set
  (:require [featured-filter.core :as core]
            [clj-time
             [core :as t]
             [format :as tf]
             [local :refer [local-now]]
             [coerce :refer [to-local-date-time to-date-time]]]
            [cheshire.core]))


(def required-attributes ["required-1" "required-2"])
(def optional-attributes ["optional-1" "optional-2"])

(defn random-word [length]
  (let [chars (map char (range 97 123))
        word (take length (repeatedly #(rand-nth chars)))]
    (reduce str word)))

(defn random-attribute [& choice-lists]
  (let [all-choices (apply concat choice-lists)]
    (rand-nth all-choices)))

(defn random-new-feature
  ([collection id validity]
   (let [feature (transient {"_action"     "new"
                             "_collection" collection
                             "_id"        id
                             "_validity" validity})
         feature (assoc! feature (random-attribute required-attributes) (random-word 5))]
     (persistent! feature))))

(defn random-change-feature [collection id current-validity validity]
  (let [feature (transient {"_action"           "change"
                            "_collection"       collection
                            "_id"               id
                            "_current_validity" current-validity
                            "_validity"         validity})
        feature (assoc! feature (random-attribute required-attributes optional-attributes) (random-word 5))]
    (persistent! feature)))

(defn create-json-files [dataset collection n-total n-updates]
  (clojure.java.io/make-parents ".test-files")
  (let [header {:_meta    {}
                :dataset  dataset}
        ids (take n-total (repeatedly #(random-word 10)))
        datetimes (take n-total (repeatedly #(local-now)))
        new-features (map (partial random-new-feature collection) ids datetimes)]
    (core/write-features ".test-files/inputset-0.json" header new-features)
    (doseq [i (range 1 (inc n-updates))]
      (let [current-times (map #(t/plus % (t/minutes (dec i))) datetimes)
            update-times (map #(t/plus % (t/minutes i)) datetimes)
            update-features (map (partial random-change-feature collection) ids current-times update-times)]
        (core/write-features (str ".test-files/inputset-" i ".json") header update-features)))))