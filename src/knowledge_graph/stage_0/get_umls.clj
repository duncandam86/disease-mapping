(ns knowledge-graph.stage-0.get-umls
  (:require
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [knowledge-graph.module.module :as kg]))

(defn get-results
  [concept-file-path semantic-file-path output-path]
  (with-open [concept-file (io/reader (io/resource concept-file-path) :encoding "UTF-8")
              semantic-file (io/reader (io/resource semantic-file-path))]
    (let [concept-data-map (->> (kg/lines-reducible concept-file)
                                vec
                                (map #(str/split % #"\|"))
                                (cons ["CUI" "LAT" "TS" "LUI" "STT" "SUI" "ISPREF" "AUI" "SAUI" "SCUI" "SDUI" "SAB" "TTY" "CODE" "STR" "SRL" "SUPPRESS" "CVF" "empty"])
                                kg/csv->map
                                (filter #(= (:LAT %) "ENG"))
                                (filter #(or (= (:SAB %) "MSH")
                                             (= (:SAB %) "HPO")
                                             (= (:SAB %) "ICD9CM")
                                             (= (:SAB %) "ICD10CM")
                                             (= (:SAB %) "ICD10")
                                             (= (:SAB %) "MDR")
                                             (= (:SAB %) "NCI")
                                             (= (:SAB %) "SNOMEDCT_US")
                                             (= (:SAB %) "MTH"))))
          semantic-data-map (->>  (kg/lines-reducible semantic-file)
                                  vec
                                  (map #(str/split % #"\|"))
                                  (cons ["CUI"  "TUI" "STN" "STY" "ATUI" "CVF" "empty"])
                                  kg/csv->map
                                  (map #(select-keys % [:CUI :STY]))
                                  ;; filter for disease and syndrome semantic types only
                                  (filter #(or
                                            (str/includes? (:STY %) "Finding")
                                            (str/includes? (:STY %) "Disease or Syndrome")
                                            (str/includes? (:STY %) "Mental or Behavioral Dysfunction")
                                            (str/includes? (:STY %) "Neoplastic Process")
                                            (str/includes? (:STY %) "Injury or Poisoning")
                                            (str/includes? (:STY %) "Pathologic Function")
                                            (str/includes? (:STY %) "Cell or Molecular Dysfunction")
                                            (str/includes? (:STY %) "Sign or Symptom"))))
          data-map (->> (kg/joiner concept-data-map semantic-data-map :CUI :CUI kg/inner-join)
                        (filter #(not (str/includes? (str/lower-case (:STR %)) "mouse")))
                        (map #(set/rename-keys % {:CUI :id
                                                  :STR :label
                                                  :TTY :label_type
                                                  :SAB :source
                                                  :CODE :hasDbXref}))
                        (map #(select-keys % [:id :label :label_type :source :hasDbXref])))
          umls-prefLabel (->> (filter #(= (:label_type %) "PT") data-map)
                              (map #(select-keys % [:id :label])))
          umls-synonym (->> (filter #(not= (:label_type %) "PT") data-map)
                            (map #(set/rename-keys % {:label :synonym}))
                            (map #(select-keys % [:id :synonym])))
          umls-hasDbXref (->> (map #(select-keys % [:id :hasDbXref :source]) data-map))
          umls (-> (kg/joiner umls-prefLabel umls-hasDbXref :id :id kg/left-join)
                   (kg/joiner umls-synonym :id :id kg/left-join)
                   distinct)]
      (->> (map #(assoc % :dbXref_source (kg/correct-source (:source %))) umls)
           (map #(assoc % :hasDbXref (kg/correct-xref-id (:hasDbXref %))))
           (map #(assoc % :source_id (:id %)))
           (map #(assoc % :subClassOf ""))
           (kg/write-csv [:id :label :source_id :subClassOf :hasDbXref :dbXref_source :synonym] output-path)))))

(defn run [_]
  (let [concept-file-path "downloads/2022AB/META/MRCONSO.RRF"
        semantic-file-path "downloads/2022AB/META/MRSTY.RRF"
        output-path "./resources/stage_0_outputs/umls.csv"]
    (get-results concept-file-path semantic-file-path output-path)))