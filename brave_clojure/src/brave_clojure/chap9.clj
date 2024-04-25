(ns chap9)

;; the sacred art of structuring your application to safely manage multiple, simultaneously executing tasks

;; Concurrency refers to managing more than one task at the same time. 

;; I will put down this drink to text you, then put my phone away and continue drinking
;; you ^^ interleave tasks, rather than executing them at the same time. 


;; Parallelism is a subclass of concurrency. Before you execute multiple tasks
;; simultaneously, you first have to be able to MANAGE multiple tasks.

;; Distributed computing is a special version of parallel computing where the processors are in different computers and tasks are
;;  distributed to computers over a network. 
;; Not necessarily the same thing as parallel programming. 


;; Blocking + Async tasks

;; a major use of concurrent programming is for blocking tasks; this means they wait until 
;; a particular operation is finished. Usually IO. 

;;If Lady Gaga texts her interlocutor and then stands there with her phone in her hand, staring at the screen for a response and not drinking, 
;; then you would say that the read next text message operation is BLOCKING
;; and that these tasks are executing synchronously.

;; If, instead, she tucks her phone AWAY so she can drink until it ALERTS her by beeping or vibrating, then the read next text
;; message task is not blocking and you would say she’s handling the task asynchronously.



;; In Clojure, you can think of your normal, serial code as a sequence of tasks. 
;; You indicate that tasks can be performed concurrently by placing them on JVM threads.


;; Threads

;; A thread is just a subprogram. A single program can have many threads, and each executes its own set of 
;; instructions  while enjoying shared access to the program's state. 

;; A thread can spawn a new thread to execute tasks concurrently. In a single processor app, 
;; we have to do this interleaving stuff (switching back and forth between threads). 

;; !!> Although the processor exeuctes instructions on each thread in order, it makes no guarantees about
;; when it will switch back + forth between threads. 

;;  This makes the program nondeterministic. You can’t know beforehand what the result will 
;; be because you can’t know the execution order, and different execution orders can yield different results!!

;; In a multi-core processor, we can avoid this by assigning a thread to each core; allowing them to independently execute
;; their sets of instructions in a particular order. The OVERALL order is still up in the air, but at least each core is ordered
;; wrt its thread. 



;; The Three Goblins: Reference Cells, Mutual Exclusion, and Dwarven Berserkers

;; 1. (Reference Cell Problem )Instructions for a program with a nondeterministic outcome 

;; ID	Instruction
;; A1	WRITE X = 0
;; A2	READ X
;; A3	WRITE X = X + 1
;; B1	READ X
;; B2	WRITE X = X + 1

;; executing in top-down order, we get x = 2. 
;; If we happen to exeucte in a different order though, (a1, a2, b1, a3, b2), we get x = 1. 
;;The reference cell problem occurs when two threads can read and write to the same location,
;; and the value at the location DEPENDS on the order of the reads and writes.

;; 2. Mutual Exclusion

;; Imagine 2 programs trying to write to a file concurrently. 
;; Without any way to claim exclusive write access to the file, the input will end up garbled
;; because the write instructions will be interleaved


;; 3. Dwarven Berserkers (Deadlock)

;; waiting on other actions can cause programs to pause indefinitely when set up incorrectly. See dining philosophers. 





;; Futures, Delays, and Promises

;; When you write serial code, you bind together these three events:
;; Task definition
;; Task execution
;; Requiring the task’s result

;; the tools we'll learn abt allow you to decouple these things! 


;; FUTURES

;; In Clojure, you can use futures to define a task and place it on another thread 
;; without requiring the result immediately.

;; You can create a future with the future macro: 

(future (Thread/sleep 4000)
        (println "I'll print after 4 seconds"))

;; Often you'll want the result of a future. the call to future returns a value that 
;; you can use as a ticket to get the result when you need it, or wait until it's ready. 
;; This is known as `dereferencing` the future. 

(let [result (future (println "this prints once" (+ 1 1)))]
  (println "derefing" @result)
  (println "derefing" (deref result)) 
  )

;; derefing will block if a future hasn't finished running. 

;; If you want to limit how long you'll wait on a future, you can pass
;; a # of milliseconds to `deref` AND a value to return if you don't get a response
;; within that time. 

;; Wait for 10ms, and if no result then return 5: 

(deref (future (Thread/sleep 1000) 0) 10 5)

;; You can check if a future is DONE by using `realized?`: 

(realized? (future (Thread/sleep 1000)))
; => false

(let [f (future)]
  @f
  (realized? f))
; => true


;; When you dereference a future, you indicate that the result is required right now and 
;; that evaluation should stop until the result is obtained.


;; DELAYS

;; Delays allow you to define a task without having to execute it or require the result immediately

(def jackson-5-delay
  (delay (let [message "Just call my name and I'll be there"]
           (println "First deref:" message)
           message)))

;; nothing happens, bc we haven't asked the let-form to be evaluated. 

jackson-5-delay ;; => #<Delay@3bcfb125: :not-delivered>

;; To force execution of the delay, we call `force`: 

(force jackson-5-delay)

;; You can also deref it just like a future. either way works. 


;; One way you can use a delay is to fire off a statement the first time one 
;; future out of a group of related futures finishes.


(def gimli-headshots ["serious.jpg" "fun.jpg" "playful.jpg"])

(defn email-user
  [email-address]
  (println "Sending headshot notification to" email-address))

(defn upload-document
  "Needs to be implemented"
  [headshot]
  true)

(let [notify (delay (email-user "and-my-axe@gmail.com"))]
  (doseq [headshot gimli-headshots]
    (future (upload-document headshot)
            (force notify))))
;; Here, even though `force notify` will be called 3 times, 
;; the actual body of delay (i.e. the email!!!) will ONLY execute
;; for the first headshot future!

;; ^^ This technique can help protect you from the mutual exclusion Concurrency Goblin
;; (the problem of making sure that only one thread can access a particular resource at a time)


;; Promises

;; Promises allow you to express that you EXPECT a result without having to define the task that 
;; should produce it or when that task should run.

(def my-promise (promise))
(deliver my-promise (+ 1 2))
@my-promise

;; you create a promise, then deliver a value to it, then deref it to get at the value. 

;; if you had tried to dereference my-promise without first delivering a value, the 
;; program would block until a promise was delivered, just like with futures and delays

;; Use case: combine this with futures to do asynchronous api calls: 

(let [butter-promise (promise)]
  (doseq [butter [yak-butter-international butter-than-nothing baby-got-yak]]
    (future (if-let [satisfactory-butter (satisfactory? (mock-api-call butter))]
              (deliver butter-promise satisfactory-butter)))))