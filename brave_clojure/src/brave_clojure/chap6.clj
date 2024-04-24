(ns brave-clojure.chap6)

;; Namespaces

;; Motivation: we need a way to organize our code
;; the functions and data we create exist somewhere in 
;; memory, and we need an easy way of keeping track of
;; what functions/data live where. We use an identifier to
;; reference the place in memory where a function/datum lives.

;; Namespaces contain maps between human-readable symbols
;; and references to addresses in memory (called vars). Think of 
;; the namespace as a shelf in a bookshelf. then each function name 
;; is a namespace is a book on that shelf. 

;; refer to the current namespace using *ns*, and get its name
;; using (ns-name *ns*)

(ns-name *ns*)

;; In clojure programs, you are ALWAYS in a namespace.

;; symbols are things like `map`, `inc`, etc. 
;; Clojure goes and look sup the corresponding var in the current namespace
;; gets its "shelf address", and retrieves the `map` book from that shelf. 

;; Storing objects with def 

(def great-books ["boop" "bop"])

;; This code tells clojure: 
;; 1. update the current namespace's map with a key named great-books
;; and a value equal to the VAR this name points to
;; 2. find a free storage shelf
;; 3. store ["boop" "bop"] on the shelf
;; 4. write the address of the shelf on the VAR. 


;; 5. return the var => #'brave-clojure.chap6/great-books

;; this is known as `interning` a var. you can interact w the map of 
;; names to vars using ns-interns

(def not-so-great-books ["beep" "bip"])
(ns-interns *ns*)

;; you can also look at ns-map, the complete map of values (including clojure-defined ones) for
;; your ns, but it's huge..

(ns-map *ns*)

;; you can use get to retrieve a specific var

(get (ns-interns *ns*) 'not-so-great-books)
;; Notice that not-so-great-books is quoted!! We are treating it as a symbol;
;; we do NOT want clojure to evaluate it to its actual value.

;; You may be wondering what that #' prefix is all about. 

;; This is the `reader forms` of a var. you can use #' to grab 
;; hold of the VAR corresponding to the the symbol that follows. 
;; The quote is making sure we refer to the thing we pass to # AS A 
;; SYMBOL, and not a value. 

;; we can get at the value that is stored in the location specified
;; by the var by using `deref`:

(deref #'brave-clojure.chap6/not-so-great-books)

;; but normally you would just use the symbol not-so-great-books, 
;; which accomplishes the same thing. 

;; Let's call def again and overwrite one of our symbols. 

(def not-so-great-books ["gloob", "shoob"])

;; Now the var has been updated with the address of this new list of
;; books. we can no longer get at the original value for not-so-great-books!

;; This is known as a `name collision`. bad!
;; fortunately, clojure allows you to create as many namespaces as you want to 
;; avoid name collisions with your codebase or external libs you use. 


;; CREATING AND SWITCHING TO NAMESPACES

;; 3 tools for create namespaces: 
;; - create-ns (fn)
;; - in-ns (fn)
;; - ns (a macro)

;; create-ns takes a symbol + creates a namespace with that name if it does't already exist,
;; and returns that namespace

(create-ns 'cheese.taxonomy)

(ns-name (create-ns 'cheese.taxonomy))

;; you will likely never use this in your code. we need to be able to MOVE INTO THE 
;; NAMESPACE we create, otherwise it's not really useful. 

;; in-ns creates the ns if it doesn't exist, AND moves into it
(in-ns 'cheese.analysis)

;; What if you want to use functions and data from other namespaces? 
;; you can use a `fully qualified symbol`. The general form is `namespace/name`. 
(in-ns 'cheese.taxonomy)
(def cheddars ["mild" "medium" "strong"])
(in-ns 'cheese.analysis)
;; Ex: cheese.taxonomy/cheddars
cheese.taxonomy/cheddars

;; "My voice gets hoarse. I need some way to tell Clojure what objects to get me without having to use the fully qualified symbol every. damn. time."

;; We can use `refer` and `alias` tools to let us do this!

;; refer gives you fine control over how to refer to objects in other ns's.

;; if we create some cheddars and bries in the cheese.taxonomy ns, and then move into
;; the analysis namespace, we can use refer to get access to all the items defined in cheese.taxonomy: 
(clojure.core/refer 'cheese.taxonomy)
cheddars

;; It does this by updating the current ns's symbol map. 
;; It's as if clojure: 
;; 1. calls ns-interns on the cheese.taxonomy ns
;; 2. merges that map with the ns-map of the current ns
;; 3. makes the result the new ns-map of the current ns

;; When you call refer, you can use the filters :only, :exclude, :rename. 
;; :only and :exclude restrict what symbol->var mapping get merged into the current
;; ns. 
(in-ns 'brave-clojure.chap6)

(refer 'cheese.taxonomy :only ['cheddars])
(refer 'cheese.taxonomy :exclude ['cheddars])

(get (ns-map *ns*) 'cheddars)
;; AGAIN, note that both the ns AND the symbol for cheddars are quoted!!!

;; :rename lets you use different symbols for the vars being merged in. 
(refer 'cheese.taxonomy :rename {'cheddars 'chdrs})
cheddars
chdrs


;;You can make your life easier by evaluating (clojure.core/refer-clojure) when you create a new namespace; 
;;this will refer the clojure.core namespace, and I’ll be using it from now on.

;; Private Functions

;; sometimes you only want a fn to be available to other fns within the same ns. 
;; clojure allows private functions using `defn-`

(in-ns 'cheese.analysis)
(clojure.core/refer-clojure)

(defn- big-private-cheese
  "doesn't do anything"
  [])

;; If you try to call this from another ns, clojure will throw an exception. 
;; Even if you explicitly `refer` the fn in another namespace, you STILL can't use it
;; bc you made it private!


;; Aliasing

;; lets you shorten a NAMESPACE for fully qualified suymbols
(alias 'taxonomy 'cheese.taxonomy)
taxonomy/cheddars


;; Real Project Organization 

;; The name of the namespace is the-divine-cheese-code.core. In Clojure, there’s a one-to-one mapping between a namespace name and the path of the file where the namespace is declared, according to the following conventions:
;; When you create a directory with lein (as you did here), the source code’s root is src by default.
;; Dashes in namespace names correspond to underscores in the file­system. So the-divine-cheese-code is mapped to the_divine_cheese_code on the filesystem.
;; The component preceding a period (.) in a namespace name corresponds to a directory. For example, since the-divine-cheese-code.core is the namespace name, the_divine_cheese_code is a directory.
;; The final component of a namespace corresponds to a file with the .clj extension; core is mapped to core.clj.

;; Ex: src/brave_clojure/chap6.clj => #'brave-clojure.chap6


;; Requiring vs. Using Namespaces

;; to use the code from our new ns visualization.svg, we'll need to require it here. 
;; require takes a symbol repping a ns name and evaluates all the code in that file.

(require 'brave-clojure.visualization.svg)

;; After requiring a ns, you can refer it. 

(refer 'brave-clojure.visualization.svg)
;; cheese heists

;; Require also allows you to alias using :as
(require '[brave-clojure.visualization.svg :as svg])
;; it only takes one arg, so we have to pass this as a quoted list


;; Instead of calling `require` and then `refer` separately, you can instead call `use`!!
;; this just wraps both calls up. Frowned upon in production code, but fine for experimentation. 
(require 'foo)
(refer 'foo)
;; is equivalent to
(use 'foo)

;; You can do the same aliasing as you could w require
(use '[foo :as f])


;; The ns macro

;; It refers the clojure.core ns by default, so you don't need to use qualified names for core fns.
;; you can control WHAT gets referred in clojure.core by using :refer-clojure

(ns brave-clojure.chap6
  (:refer-clojure :exclude [println]))
;; excludes println
;; is equivalent to 
(in-ns brave-clojure.chap6)
(refer clojure.core :exclude ['println])

;; There are 6 possible kinds of "references" like :refer-clojure: 
;; (:refer-clojure)
;; covered above
;; (:require)
;; (:use)
;; (:import)
;; (:load)
;; (:gen-class)

;; :require works like require, :use works like :use p much
(ns brave-clojure.chap6
  (:require [foo :as f]))
;; is equiv to
(in-ns brave-clojure.chap6)
(require '[foo :as f])
;; NOTE: you do not have to quote your ns or fn names in the ns macro.

;; you can require multiple libs
(ns brave-clojure.chap6
  (:require [foo :as f]
            [bar :as b]))

;; You can also refer things directly inside :require
(ns brave-clojure.chap6
  (:require [foo :as f :refer [my-fun]]))
;; is equiv to 
(in-ns brave-clojure.chap6)
(require '[foo :as f])
(refer 'foo :only ['my-fun])

;; You can refer all the functions using :all
(ns brave-clojure.chap6
  (:require [foo :as f] :refer :all))
;; is equiv to 
(in-ns brave-clojure.chap6)
(require '[foo :as f])
(refer 'foo)


;; EXAMPLE
;; (ns brave-clojure.chap6
;;   (:require [brave-clojure.visualization.svg :as svg :refer [points]]))


(ns brave-clojure.chap6 
  (:require [clojure.java.browse :as browse]
            [brave-clojure.visualization.svg :refer [xml]]))

(def heists [{:location "Cologne, Germany"
              :cheese-name "Archbishop Hildebold's Cheese Pretzel"
              :lat 50.95
              :lng 6.97}
             {:location "Zurich, Switzerland"
              :cheese-name "The Standard Emmental"
              :lat 47.37
              :lng 8.55}
             {:location "Marseille, France"
              :cheese-name "Le Fromage de Cosquer"
              :lat 43.30
              :lng 5.37}
             {:location "Zurich, Switzerland"
              :cheese-name "The Lesser Emmental"
              :lat 47.37
              :lng 8.55}
             {:location "Vatican City"
              :cheese-name "The Cheese of Turin"
              :lat 41.90
              :lng 12.45}])

(defn url
  [filename]
  (str "file:///"
       (System/getProperty "user.dir")
       "/"
       filename))

(defn template
  [contents]
  (str "<style>polyline { fill:none; stroke:#5881d8; stroke-width:3}</style>"
       contents))

(defn -main
  [& args]
  (let [filename "map.html"]
    (->> heists
         (xml 50 100)
         template
         (spit filename))
    (browse/browse-url (url filename))))





























