#!/usr/bin/bb
(ns filter
  (:require [cheshire.core :as json]
            [clojure.walk :as walk]
            [clojure.string :as str]))

(defn org->html-ext [fname]
  (if (str/ends-with? fname ".org")
    (str/replace fname #"org$" "html")
    fname))

(defn- transform-link-target
  "If link points to an org file, change it to html."
  [elem]
  (if-not (= (:t elem) "Link")
    elem
    (update-in elem [:c 2 0] org->html-ext)))

(->> (json/decode (slurp *in*) true)
     (walk/prewalk transform-link-target)
     (json/encode)
     print)
