#!/usr/bin/bb
(ns filter
  (:require [cheshire.core :as json]
            [clojure.walk :as walk]
            [clojure.string :as str]))

(defn- transform-link
  "Filter for hyperlinks.
  Changes links to .org files to point to .html files,
  and adds the 'button' class to any links with specified text."
  [elem]
  (cond
    (str/ends-with? (get-in elem [:c 2 0]) ".org")
    (update-in elem [:c 2 0] str/replace #"org$" "html")

    (some #{(get-in elem [:c 1 0 :c])} ["PDF" "DOI" "Manuscript"])
    (update-in elem [:c 0 1] conj "button")
    
    :else elem))

(defn- link-filter [elem]
  (if-not (= (:t elem) "Link")
    elem
    (transform-link elem)))

(->> (json/decode (slurp *in*) true)
     (walk/prewalk link-filter)
     (json/encode)
     print)
