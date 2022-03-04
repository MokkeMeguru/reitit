(defproject metosin/reitit-swagger "0.5.16"
  :description "Reitit: OpenAPI-support (alpha)"
  :url "https://github.com/MokkeMeguru/reitit"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :scm {:name "git"
        :url "https://github.com/MokkeMeguru/reitit"
        :dir "../.."}
  :plugins [[lein-parent "0.3.2"]]
  :parent-project {:path "../../project.clj"
                   :inherit [:deploy-repositories :managed-dependencies]}
  :dependencies [[metosin/reitit-core]])
