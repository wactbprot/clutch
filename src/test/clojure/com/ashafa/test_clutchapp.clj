(ns com.ashafa.test-clutchapp
 (:require [clojure.contrib.string :as string])
 (:use
   (com.ashafa clutch clutchapp)
   clojure.test
   (clojure.contrib
     [io :only (slurp* file as-url)])))

(defn- content-map
  "Returns a map containing entries of
   [path-relative-to-root file-content]
   for each descendant files in root-dir."
  [root-dir]
  (let [root-len (-> root-dir file .getAbsolutePath count)
        files (->> root-dir file file-seq
                (remove #(-> % .getName (.startsWith "."))))]
  (reduce
    (fn [s f]
      (if (.isDirectory f)
        s
        (conj s [(->> f .getAbsolutePath (string/drop root-len))
                 (slurp* f)])))
    {} files)))

; seems like a clone containing the same content as the source of a push
; is the best smoke-test.  Ideally, we'd do the same comparison based on
; cloning apps pushed by couchapp and pushing apps cloned by couchapp,
; but I definitely do not want to tangle with ramping up couchapp in every
; environment that might want to test clutch.  We'll just have to handle
; compatibility issues as they come up (though I doubt many people will be
; mixing couchapp and clutch). - Chas
(deftest- test-clone-and-push
  (let [clutchapp-test-db "http://localhost:5984/test-clutchapp"
        app-root-path "src/test/resources/clutchapp"
        clone-dir (doto (file (System/getProperty "java.io.tmpdir") (str "test-clutchapp-" (rand)))
                    .mkdir
                    .deleteOnExit)]
    (println "Testing com.ashafa.test-clutchapp with output directory " clone-dir)
    (is (nil? (database-info (as-url clutchapp-test-db))))
    (try
      (push-all app-root-path clutchapp-test-db)
      (clone-all clutchapp-test-db clone-dir)
      (is (= (content-map app-root-path)
            (content-map clone-dir)))
      (finally
        (delete-database (as-url clutchapp-test-db))))))
