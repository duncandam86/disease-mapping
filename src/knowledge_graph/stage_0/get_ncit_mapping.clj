(ns knowledge-graph.stage-0.get-ncit-mapping
  (:require
   [camel-snake-kebab.core :as csk]
   [clj-http.client :as client]
   [clojure.data.csv :as csv]
   [clojure.set :as set]
   [clojure.string :as str]
   [knowledge-graph.module.module :as kg]))

(defn csv->map
  [csv-data]
  (map zipmap
       (->> (first csv-data)
            (map #(csk/->camelCase %))
            (map keyword)
            repeat)
       (rest csv-data)))

(defn get-ncit-mapping
  [neoplasm-url meddra-url output-path]
  (let [data-neoplasm-map (->> (client/get neoplasm-url {:as :reader})
                                :body
                                csv/read-csv
                                csv->map)
        data-meddra-map (->> (client/get meddra-url {:as :reader})
                              :body
                              csv/read-csv
                              csv->map)
        ncit-umls-mapping (->> (map #(set/rename-keys % {:ncItCode :id :ncImCui :hasDbXref :ncImPreferredName :synonym}) data-neoplasm-map)
                               (map #(assoc % :hasDbXref (str/join ":" ["UMLS" (:hasDbXref %)])))
                               (map #(assoc % :source "UMLS"))
                               (mapv #(select-keys % [:id :hasDbXref :source :synonym])))
        ncit-medgen-mapping (->> (map #(set/rename-keys % {:ncItCode :id :ncImCui :hasDbXref :ncImPreferredName :synonym}) data-neoplasm-map)
                                 (map #(assoc % :hasDbXref (str/join ":" ["MEDGEN" (:hasDbXref %)])))
                                 (map #(assoc % :source "MEDGEN"))
                                 (mapv #(select-keys % [:id :hasDbXref :source :synonym])))
        ncit-neoplasm-mapping (->> (map #(set/rename-keys % {:ncItCode :id :sourceCode :hasDbXref :ncImSource :source :sourceTerm :synonym}) data-neoplasm-map)
                                   (map #(assoc % :hasDbXref (str/join ":" [(:source %) (:hasDbXref %)])))
                                   (mapv #(select-keys % [:id :hasDbXref :source :synonym])))
        ncit-meddra-mapping (->> (map #(assoc % :source "MedDRA") data-meddra-map) 
                                 (map #(set/rename-keys % {:CODE :id :TARGET_CODE :hasDbXref :TARGET_TERM :synonym}))
                                 (map #(assoc % :hasDbXref (str/join ":" ["MEDDRA" (:hasDbXref %)])))
                                 (mapv #(select-keys % [:id :hasDbXref :source :synonym])))
        ncit-mapping (->> (concat ncit-umls-mapping ncit-medgen-mapping ncit-neoplasm-mapping ncit-meddra-mapping)
                          distinct)]
        (->> (filter #(not= (:hasDbXref %) ":") ncit-mapping)
             (mapv #(select-keys % [:id :hasDbXref :source :synonym]))
             (kg/write-csv [:id :hasDbXref :source :synonym] output-path))))

(defn run
  [_]
  (let [neoplasm-url "https://evs.nci.nih.gov/ftp1/NCI_Thesaurus/Neoplasm/Neoplasm_Core_Mappings_NCIm_Terms.csv"
        meddra-url "https://ncit.nci.nih.gov/ncitbrowser/ajax?action=export_maps_to_mapping&target=MedDRA"
        output-path "./resources/stage_0_outputs/ncit_mapping.csv"]
    (get-ncit-mapping neoplasm-url meddra-url output-path)))