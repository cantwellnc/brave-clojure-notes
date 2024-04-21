(ns brave-clojure.core)

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))

(defmacro infix [infixed] (list (second infixed) (first infixed) (last infixed)))

(infix (1 + 2))

(macroexpand '(infix (1 + 2)))

(defmacro infix-2 [operand1 op operand2] (list op operand1 operand2))
;; you can destructure arguments just like you can in functions

;; variadic stuff works just fine too

(defmacro and-

  ([] true)
  ([x] x)
  ([x & next] ;; variadics!
   `(let [and# ~x]
      (if and# (and- ~@next) and#)))) ;; recursion is fair game!

;; Macro writing is all about building a LIST for clojure to evaluate.
;; need to quote expressions to get UNEVALUATED data structures in your final list
;; generally need to be careful about a symbol vs its value.

;; Ex: create a macro that takes an expr and prints AND returns its value. Different from println, bc that returns nil.

;; your macro should produce code like this:

(let [result expression]
  (println result)
  result)

;; Think of this as the LIST you want to produce.

(defmacro my-print-whoopsie
  [expression]
  (list let [result expression]
        (list println result)
        result))
;; this doesn't work, ^^ bc the macro body is trying to get the VALUE that the SYMBOL `let` refers to.
;; We want to return the actual symbol!!
;; this same mistake is present in `result` and `println` as well; we're taking the actual value, instead of passing the symbol.

(defmacro my-print
  [expression]
  (list 'let ['result expression]
        (list 'println 'result)
        'result))
;; ^^ better

(macroexpand '(my-print (inc 1)))

;; QUOTING

(quote (+ 1 2)) ;; this yields the unevaluted expression (+ 1 2)

;; the single quote character ' is a "reader macro" for (quote x)

(defmacro when-
  [test & body]
  (list 'if test (cons 'do body)))

(macroexpand '(when (the-cows-come :home)
                (call me :pappy)
                (slap me :silly)))

(defmacro unless
  "Inverted 'if'"
  [test & branches]
  (conj (reverse branches) test 'if))

(macroexpand '(unless (done-been slapped? me)
                      (slap me :silly)
                      (say "I reckon that'll learn me")))

;; Syntax Quoting

;; returns uneval'd data structures, similar to normal quoting.
;; Differences:
;; - returns fully qualified symbols (includes namespace)

'+ ;; this just evaluates to +

`+ ;; this returns clojure.core/+

;; quoting a list recursively quotes all the elements

'(+ 1 2)

;; syntax quoting a list recursively SYNTAX quotes all the elements

`(+ 1 2)

;; this helps you avoid namespace collisions!

;; The other big difference is that ` allows the use of ~, the UNQUOTE.
;; whenever ~ appears INSIDE a syntax quote, the power to add unevaluated namespaced symbols is removed.

`(+ 1 ~(inc 1)) ;; this returns (1 2), so it evaluted (inc 1)!

;; In the same way that string interpolation leads to clearer + more concise code, syntax quoting + unquoting
;; allows you to create lists much more effectively.
;; Via our previous approach, this is how you would write the above with only the simple quote:

(list '+ 1 (inc 1))

;; Actually Using Syntax Quoting in Macros

(defmacro code-critic
  "Phrases are courtesy Hermes Conrad from Futurama"
  [bad good]
  (list 'do
        (list 'println
              "Great squid of Madrid, this is bad code:"
              (list 'quote bad))
        (list 'println
              "Sweet gorilla of Manila, this is good code:"
              (list 'quote good))))

(code-critic (1 + 1) (+ 1 1))

;; we're going to rewrite this using syntax quoting + unquote.

(defmacro code-critic
  [bad good]
  `(do
     (println "Great squid of Madrid, this is bad code:" (quote ~bad))
     (println "Sweet gorilla of Manila, this is good code:" (quote ~good))))

;;In this case, you want to quote everything except for the symbols good and bad. In the original version, you have to quote each piece individ­ually and explicitly place it in a list in an unwieldy fashion, just to prevent those two symbols from being quoted. With syntax quoting, you can just wrap the entire do expression in a quote and simply unquote the two symbols that you want to evaluate.

(code-critic (1 + 1) (+ 1 1))
(macroexpand '(code-critic (1 + 1) (+ 1 1)))

;; Summary: macros recieve unevaluated, arbitrary data structures as arguments and return data structures that CLojure evaluates.

;; Refactoring a Macro + Unquote Splicing

(defn criticize-code
  [criticism code]
  `(println ~criticism (quote ~code)))

;; pull out duplicate functionality to a function that actually handles the guts of the macro
;; still returns a syntax-quoted list!!

(defmacro code-critic
  [bad good]
  `(do ~(criticize-code "Cursed bacteria of Liberia, this is bad code:" bad)
       ~(criticize-code "Sweet sacred boa of Western and Eastern Samoa, this is good code:" good)))

(code-critic (1 m) (m 1))

(defmacro code-critic
  [bad good]
  `(do ~(map #(apply criticize-code %)
             [["Great squid of Madrid, this is bad code:" bad]
              ["Sweet gorilla of Manila, this is good code:" good]])))


(macroexpand '(code-critic (1 m) (m 1)))

;; This code ^^ throws a null pointer exception. Do you see why? 


;; Splice Unquote to the rescue!
;; It takes everything in a sexp and explodes it out into the surrounding s-expression
;; You use it like `(some stuff ~@(+ 1 2 3))

`(+ ~@(list 1 2 3))
; => (clojure.core/+ 1 2 3)

(defmacro code-critic
  [bad good]
  `(do ~@(map #(apply criticize-code %)
             [["Great squid of Madrid, this is bad code:" bad]
              ["Sweet gorilla of Manila, this is good code:" good]])))

(macroexpand '(code-critic (1 m) (m 1)))

;; now the above code works, since we are placing 2 expressions into the body of do, rather than a seq of expressions in the body of the do!







;; Things to watch out for....

;; Variable Capture (aka Shadowing...)
;; a macro introduces a binding that (unknown to the caller), eclipses an existing binding.
;; ex:

(def message "Good job!")

(defmacro with-mischief
  [& stuff-to-do]
  (concat (list 'let ['message "Oh, big deal!"])
          stuff-to-do))

(with-mischief
  (println "Here's how I feel about that thing you did: " message))

;; In the above code, we expect message to be "Good job!", but bc the macro introduces a let in the body,
;; it overrides the value of `message` in the outer scope when println is called.
;; Note that we did NOT use syntax quoting. Trying to do so actually throws:

(def message "Good job!")
(defmacro with-mischief
  [& stuff-to-do]
  `(let [message "Oh, big deal!"]
     ~@stuff-to-do))

(with-mischief
  (println "Here's how I feel about that thing you did: " message))


;; Syntax quoting THROWS in this situation to PREVENT you from shadowing vars in your macro, so it just prevents you from using `let` at all.



;; If you want to use bindings in your macro body, you have to use `gensym`; this produces a unique symbol every time you call it.
(gensym)

;; You can also pass a symbol prefix: 
(gensym 'message)


;; To avoid shadowing, let's use gensym in the previous example.
(def message "hello")
(defmacro without-mischief
  [& stuff-to-do]
  (let [macro-message (gensym 'message)]
    `(let [~macro-message "Oh, big deal!"]
       ~@stuff-to-do
       (println "I still need to say: " ~macro-message))))

(without-mischief
  (println "Here's how I feel about that thing you did: " message))

;; This pattern of creating a UNIQUE symbol with an EASY NAME TO WRITE is so common that there's a shorthand for it:

`(let [my-sym# "tea"] (println my-sym#))


(defmacro without-mischief-nice
  [& stuff-to-do]
    `(let [macro-message# "Oh, big deal!"]
       ~@stuff-to-do
       (println "I still need to say: " macro-message#)))

(without-mischief
  (println "Here's how I feel about that thing you did: " message))
;; Same result!



;; Double Evaluation
;; occurs when a form passed to a macro as an argument gets evaluated more than once. Consider the following:

(defmacro report
  [to-try]
  `(if ~to-try
     (println (quote ~to-try) "was successful:" ~to-try)
     (println (quote ~to-try) "was not successful:" ~to-try)))

;; Thread/sleep takes a number of milliseconds to sleep for
(report (do (Thread/sleep 1000) (+ 1 1)))

;; This function ends up sleeping for 2 seconds, NOT 1. This is bc to-try is evaluated TWICE in either branch in the macro body.

;; If you only want the body evaluted ONCE but need to use it in multiple places, use a let + genysym:

(defmacro report
  [to-try]
  `(let [result# ~to-try]
     (if result#
       (println (quote ~to-try) "was successful:" result#)
       (println (quote ~to-try) "was not successful:" result#))))

report (do (Thread/sleep 1000) (+ 1 1)))

;; By placing to-try in a let expression, you only evaluate that code once and bind the result to an auto-gensym’d symbol, result#, which you can now reference without reevaluating the to-try code.


;; Macros all the way down

;; They can only ocmpose with each other, so writing one can result in writing many, depending on where you want to use it.

(doseq [code ['(= 1 1) '(= 1 2)]]
  (report code))
;; This gives us back (=1 1) (resp (= 1 2)) as the result; which is wrong! we want the actual truth value.

;; to make this work with doseq, we need another macro :(

(defmacro doseq-macro
  [macroname & args]
  `(do
     ~@(map (fn [arg] (list macroname arg)) args)))

(doseq-macro report (= 1 1) (= 1 2))


(+)
