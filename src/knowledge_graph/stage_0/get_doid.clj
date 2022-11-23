(ns knowledge-graph.stage-0.get-doid
  (:require
   [clj-http.client :as client]
   [clojure.data.xml :as d-xml]
   [clojure.data.zip.xml :refer [attr text xml->]]
   [clojure.zip :as z]
   [clojure.string :as str]
   [knowledge-graph.module.module :as kg]))

(defn class-map
  "Extract information from xml elements and create a map"
  [data]
  (let [z (z/xml-zip data)]
    (for [id             (xml-> z :Class (attr :rdf/about))
          label          (xml-> z :label text)
          source_id      (xml-> z :id text)
          subClassOf     (xml-> z :subClassOf (attr :rdf/resource))
          hasDbXref      (xml-> z :hasDbXref text)
          synonym        (xml-> z :hasExactSynonym text)]
      {:id id :label label :source_id source_id :subClassOf subClassOf :hasDbXref hasDbXref :synonym synonym})))

(defn get-results
  "Download xml file, parse for necessary information, and write as csv output"
  [url output_path]
  (->> (client/get url {:as :stream})
       :body
       (d-xml/parse)
       :content
       (filter #(= (:tag %) :Class))
       (map class-map)
       (apply concat)
       (filter #(some? (:id %)))
       (map #(assoc % :id (last (str/split (:id %) #"/"))))
       (map #(assoc % :subClassOf (last (str/split (:subClassOf %) #"/"))))
       (map #(assoc % :subClassOf (str/replace (:subClassOf %) #"_" ":")))
       (map #(assoc % :dbXref_source (kg/correct-source (first (str/split (:hasDbXref %) #":")))))
       (map #(assoc % :hasDbXref (kg/correct-xref-id (:hasDbXref %))))
       (kg/write-csv [:id :label :source_id :subClassOf :hasDbXref :dbXref_source :synonym] output_path)))

(defn run [_]
  (let [url "https://raw.githubusercontent.com/DiseaseOntology/HumanDiseaseOntology/main/src/ontology/doid.owl"
        output "./resources/stage_0_outputs/doid.csv"]
    (get-results url output)))

