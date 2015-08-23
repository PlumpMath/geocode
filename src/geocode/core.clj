(ns geocode.core
  (:gen-class))

(require '[clojure.data.csv :as csv]
         '[clojure.java.io :as io]
         '[clojure.data.json :as json])

(def in-file "/Users/rca/Dropbox/Work/Net Metering/PA_DEF.csv")

(def cquery "http://geocoding.geo.census.gov/geocoder/locations/address?")
(def oquery "http://openstates.org/api/v1//legislators/geo/?")

(defn cleaner
  "make a string url-ready"
  [in-string]
  (clojure.string/replace 
    (clojure.string/replace 
      (clojure.string/replace in-string #"[,|\.]" "")
    #"\s$" "")
  #" " "+")
  )

(defn geocode-address
  "Takes a vector and gets it ready to geocode"
  [in-vector]
  (def rname (nth in-vector 3))
  (def url (str
    cquery
    "street="
    (cleaner (nth in-vector 5))
    "&city="
    (cleaner (nth in-vector 6))
    "&state="
    (cleaner (nth in-vector 7))
    "&zip="
    (nth in-vector 8)
    "&benchmark=Public_AR_Current&format=json"
    ))

  (def json-hashmap (get (get (json/read-str (slurp url)) "result") "addressMatches" ))
  (if (> (count json-hashmap) 0)
    (def coord (get (first json-hashmap) "coordinates"))
    (def coord nil))

  (print rname ": ")
  coord
  )

(defn get-sen-and-rep
  "given lat-lon, get senator and representative"
  [coords oapikey]
  (if coords
    (do
      (def url (str
             oquery
             "lat="
             (get coords "y")
             "&long="
             (get coords "x")
             "&apikey="
             oapikey))
      (def jget (json/read-str(slurp url)))
      (def result (str
                   "Rep: "
                   (get (first jget) "full_name")
                   ", Sen: "
                   (get (second jget) "full_name"))
        )
      )
      (def result "no data"))
  (println result))



(defn -main
  "I don't do a whole lot ... yet."
  [& args]

  ; load the api-key
  (def api-key (get (json/read-str
                 (slurp "/Users/rca/.api-keys.json"))
                    "openstates"))
  (println api-key) 

  (with-open [in-file (io/reader in-file)]
      (doall
        (map #(get-sen-and-rep %1 api-key) 
        (map geocode-address (csv/read-csv in-file)))
      )
  )

  (println "Done Processing")
)
