(ns build
  (:require [babashka.fs :as fs]
            [babashka.process :as pro]
            [hiccup2.core :as h]
            [clojure.string :as str]))

(def header (h/raw (slurp "templates/header.html")))
(def footer (h/raw (slurp "templates/footer.html")))

(defn- out-file-name
  "Generate an output file name from an input file name."
  [file]
  (let [basename (-> file fs/file-name fs/strip-ext)]
    (str "public/" basename ".html")))

(defn- html-content-str
  "Generate html content from input file using pandoc."
  [file]
  (:out
   (pro/shell {:out :string}
              "pandoc" "-i" file
              "--from=org-auto_identifiers"
              "--to=html"
              "--filter=filter.clj")))

(defn- html-file-str
  "Wrap content in header and footer to create a standalone file."
  [content]
  (str
   (h/html
    (h/raw "<!DOCTYPE html>")
    [:html {:lang "en"}
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:link {:rel "stylesheet" :href "/css/normalize.css"}]
      [:link {:rel "stylesheet" :href "/css/sakura.css"}]]
     [:body
      [:div#container
       header
       [:main (h/raw content)]
       footer]]])))

;; Root folder contents
(doseq [file (fs/list-dir "content")]
  (when (= (fs/extension file) "org")
    (let [out (out-file-name file)]
      (->> file
           html-content-str
           html-file-str
           (spit out))
      (println "Wrote" out))))

;; Blog

;; Regenerate blog from scratch each time
(fs/delete-tree "public/blog")

;; Atom to store information about each blog post (for use in the index)
(def blog-posts (atom []))

(defn- extract-org-title [file]
  (->> (fs/file file)
       slurp
       (re-find #"^#\+title: (.+)\n")
       second))

(defn- extract-date
  "Extract date from a filepath that includes the string YYYY-MM-DD (presumably in the dir)."
  [file]
  (->> (str file)
       (re-find #"\d\d\d\d-\d\d-\d\d")))

(defn- convert-blog-post [file _]
  (if (= (fs/extension file) "org")
    (let [out (-> file fs/strip-ext (str ".html"))
          title (extract-org-title file)
          date (extract-date file)]
      (->> file
           html-content-str
           html-file-str
           (spit out))
      (swap! blog-posts conj [date title out])
      (fs/delete file)
      :continue)
    :continue))

(println "Generating blog posts...")
(fs/copy-tree "content/blog" "public/blog" {:replace-existing true})
(doseq [file (fs/walk-file-tree "public/blog" {:visit-file convert-blog-post})])

;; Blog index
(defn- gen-blog-post-link [[date title link]]
  (let [link (->> link (re-find #"public/blog/(\S+)index.html") second)]
    [:a {:href link} (str "[" date "] " title)]))

(let [blog-index-content
      (h/html
       [:h1 "Blog"]
       [:p "Index of my blog posts"]
       (->> @blog-posts
            (sort-by first #(compare %2 %1))
            (map gen-blog-post-link)
            (interpose [:br])
            (into [:ul])))]
  (->> blog-index-content
       html-file-str
       (spit "public/blog/index.html")))
(println "Blog posts generated")

;; Static artifacts
(fs/copy-tree "content/static" "public/static" {:replace-existing true})
(println "Copied over static artifacts")
