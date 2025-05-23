---
layout: default
title: CodeNarc - Concurrency Rules
---  

# Concurrency Rules  ("*rulesets/concurrency.xml*")

## BusyWait Rule

*Since CodeNarc 0.13*

Busy waiting (forcing a `Thread.sleep()` while waiting on a condition) should be avoided. Prefer using the gate and
barrier objects in the `java.util.concurrent` package.

Example of violations:

```
    while (x) { Thread.sleep(1000) }
    while (x) { Thread.sleep(1000) { /* interruption handler */} }
    for (int x = 10; x; x--) {
        sleep(1000)     // sleep is added to Object in Groovy
    }

    // here is the proper way to wait:
    countDownLatch.await()

    // this is weird code to write, but does not cause a violation
    for (def x : collections) {
        sleep(1000)
    }

    while (x) {
        // you should use a lock here, but technically you are
        // not just busy waiting because you are doing other work
        doSomething()
        sleep(1000)
    }
```


## DoubleCheckedLocking Rule

*Since CodeNarc 0.13*

This rule detects double checked locking, where a 'lock hint' is tested for null before initializing an object within
a synchronized block. Double checked locking does not guarantee correctness and is an anti-pattern.

A full explanation of why double checked locking is broken in Java is available on Wikipedia:
<http://en.wikipedia.org/wiki/Double-checked_locking>

Example of violations:

```
    if (object == null) {
        synchronized(this) {
            if (object == null) {
                // createObject() could be called twice depending
                // on the Thread Scheduler.
                object = createObject()
            }
        }
    }

    // there are several idioms to fix this problem.
    def result = object;
    if (result == null) {
        synchronized(this) {
            result = object;
            if (result == null)
                object = result = createObject()
        }
    }

    // and a better solution for a singleton:
    class myClass  {
        private static class ObjectHolder {
           public static Object object = createObject()
        }
        public static Object getObject() {
            return ObjectHolder.object;
        }
    }
```


## InconsistentPropertyLocking Rule

*Since CodeNarc 0.13*

Class contains similarly-named get and set methods where one method of the pair is marked either @WithReadLock
or @WithWriteLock and the other is not locked at all. This may result in incorrect behavior at runtime, as
callers of the get and set methods will not necessarily lock correctly and my see an inconsistent state for the object.
The get and set method should both be guarded by @WithReadLock/@WithWriteLock or neither should be guarded.

Example of violations:

```
    class Person {
        String name
        Date birthday
        boolean deceased
        boolean parent

        @WithWriteLock setName(String name) {
            this.name = name
        }
        // violation, get method should be locked
        String getName() {
            name
        }

        // violation, set method should be locked
        void setBirthday(Date birthday) {
            this.birthday = birthday
        }

        @WithReadLock String getBirthday() {
            birthday
        }

        // violation, set method should be locked
        void setDeceased(boolean deceased) {
            this.deceased = deceased
        }

        @WithReadLock boolean isDeceased() {
            deceased
        }

        @WithWriteLock void setParent(boolean parent) {
            this.parent = parent
        }

        // violation, get method should be locked
        boolean isParent() {
            parent
        }
    }
```

## InconsistentPropertySynchronization Rule

*Since CodeNarc 0.13*

Class contains similarly-named get and set methods where the set method is synchronized and the get method is not,
or the get method is synchronized and the set method is not. This may result in incorrect behavior at runtime, as
callers of the get and set methods will not necessarily see a consistent state for the object. The get and set method
should both be synchronized or neither should be synchronized.

Example of violations:

```
    class Person {
        String name
        Date birthday
        boolean deceased
        boolean parent
        int weight

        synchronized setName(String name) {
            this.name = name
        }
        // violation, get method should be synchronized
        String getName() {
            name
        }

        // violation, set method should be synchronized
        void setBirthday(Date birthday) {
            this.birthday = birthday
        }

        synchronized String getBirthday() {
            birthday
        }

        // violation, set method should be synchronized
        void setDeceased(boolean deceased) {
            this.deceased = deceased
        }

        synchronized boolean isDeceased() {
            deceased
        }

        synchronized void setParent(boolean parent) {
            this.parent = parent
        }

        // violation, get method should be synchronized
        boolean isParent() {
            parent
        }

        // violation get method should be synchronized
        @groovy.transform.Synchronized
        void setWeight(int value) {
            weight = value
        }
    }
```


## NestedSynchronization Rule


This rule reports occurrences of nested `synchronized` statements.

Nested `synchronized` statements should be avoided. Nested `synchronized` statements
are either useless (if the lock objects are identical) or prone to deadlock.

Note that a *closure* or an *anonymous inner class* carries its own context (scope).
A `synchronized` statement within a *closure* or an *anonymous inner class* defined
within an outer `synchronized` statement does not cause a violation (though nested
`synchronized` statements within either of those will).

Here is an example of code that produces a violation:

```
    def myMethod() {
        synchronized(this) {
            // do something ...
            synchronized(this) {
                // do something else ...
            }
        }
    }
```


## NoScriptBindings Rule

This rule reports occurrences of global variables that are bound to a script.

These should be avoided as concurrent executions can modify and read the shared
variable, leading to concurrency bugs.

Examples (within a script):

```
    b = 1                       // violation
    def myMethod() {
        a = 1                   // violation
    }

    // These are fields and local variables; they are OK
    int b = 1
    def myMethod2() {
        Integer a = 1
    }
```


## StaticCalendarField Rule

*Since CodeNarc 0.13*

`Calendar` objects should not be used as `static` fields. Calendars are inherently unsafe for multithreaded use. Sharing a
single instance across thread boundaries without proper synchronization will result in erratic behavior of the application.
Under 1.4 problems seem to surface less often than under Java 5 where you will probably see random `ArrayIndexOutOfBoundsException`
or `IndexOutOfBoundsException` in `sun.util.calendar.BaseCalendar.getCalendarDateFromFixedDate()`. You may also experience
serialization problems. Using an instance field or a `ThreadLocal` is recommended.

For more information on this see Sun Bug #6231579 and Sun Bug #6178997.

Examples:

```
    // Violations
    class MyClass {
        static Calendar calendar1
        static java.util.Calendar calendar2

        static final CAL1 = Calendar.getInstance()
        static final CAL2 = Calendar.getInstance(Locale.FRANCE)
        static def cal3 = Calendar.getInstance(timezone)
        static Object cal4 = Calendar.getInstance(timezone, locale)
    }

    // These usages are OK
    class MyCorrectClass {
        private final Calendar calendar1
        static ThreadLocal*Calendar* calendar2
    }
```


## StaticConnection Rule

*Since CodeNarc 0.14*

Creates violations when a `java.sql.Connection` object is used as a `static` field. Database connections
stored in `static` fields will be shared between threads, which is unsafe and can lead to race conditions.

A transactional resource object such as database connection can only be associated with one transaction at a time.
For this reason, a connection should not be shared between threads and should not be stored in a static field.
See Section 4.2.3 of the *J2EE Specification* for more details.

References:
  * Standards Mapping - Security Technical Implementation Guide Version 3 - (STIG 3) APP3630.1 CAT II
  * Standards Mapping - Common Weakness Enumeration - (CWE) CWE ID 362, CWE ID 567
  * Standards Mapping - SANS Top 25 2009 - (SANS 2009) Insecure Interaction - CWE ID 362
  * Standards Mapping - SANS Top 25 2010 - (SANS 2010) Insecure Interaction - CWE ID 362
  * Java 2 Platform Enterprise Edition Specification, v1.4 Sun Microsystems


## StaticDateFormatField Rule

*Since CodeNarc 0.13*

`DateFormat` objects should not be used as `static` fields. DateFormats are inherently unsafe for multithreaded use. Sharing a
single instance across thread boundaries without proper synchronization will result in erratic behavior of the application.
Under 1.4 problems seem to surface less often than under Java 5 where you will probably see random `ArrayIndexOutOfBoundsException`
or `IndexOutOfBoundsException` in `sun.util.calendar.BaseCalendar.getCalendarDateFromFixedDate()`. You may also experience
serialization problems. Using an instance field or a `ThreadLocal` is recommended.

For more information on this see Sun Bug #6231579 and Sun Bug #6178997.

Examples:

```
    // Violations
    class MyClass {
        static DateFormat dateFormat1
        static java.text.DateFormat dateFormat2

        static final DATE1 = DateFormat.getDateInstance(DateFormat.LONG, Locale.FRANCE)
        static final def DATE2 = DateFormat.getDateInstance(DateFormat.LONG)
        static Object date3 = DateFormat.getDateInstance()

        static final DATETIME1 = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT, Locale.FRANCE)
        static final def DATETIME2 = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT)
        static final Object DATETIME3 = DateFormat.getDateTimeInstance()

        static final TIME1 = DateFormat.getTimeInstance(DateFormat.LONG, Locale.FRANCE)
        static final def TIME2 = DateFormat.getTimeInstance(DateFormat.LONG)
        static final Object TIME3 = DateFormat.getTimeInstance()
    }

    // These usages are OK
    class MyCorrectClass {
        private DateFormat calendar1
        static ThreadLocal*DateFormat* calendar2
    }
```


## StaticMatcherField Rule

*Since CodeNarc 0.13*

Matcher objects should not be used as static fields. Calendars are inherently unsafe for multithreaded use. Sharing a single
instance across thread boundaries without proper synchronization will result in erratic behavior of the application.

Example of violations:

```
    // two violations
    class MyClass {
      static Matcher matcher1
      static java.util.regex.Matcher matcher2
    }

    // these usages are OK
    class MyCorrectClass {
      private Matcher matcher1
      static ThreadLocal<Matcher> matcher2
    }
```


## StaticSimpleDateFormatField Rule

*Since CodeNarc 0.14*

`SimpleDateFormat` objects should not be used as `static` fields. SimpleDateFormats are inherently unsafe for
multithreaded use. Sharing a single instance across thread boundaries without proper synchronization will result in
erratic behavior of the application. Under 1.4 problems seem to surface less often than under Java 5 where you will
probably see random `ArrayIndexOutOfBoundsException` or `IndexOutOfBoundsException` in
`sun.util.calendar.BaseCalendar.getCalendarDateFromFixedDate()`. You may also experience
serialization problems. Using an instance field or a `ThreadLocal` is recommended.

For more information on this see Sun Bug #6231579 and Sun Bug #6178997.

Examples:

```
    // Violations
    class MyClass {
        static SimpleDateFormat dateFormat1
        static java.text.SimpleDateFormat dateFormat2

        static final DATE1 = new SimpleDateFormat()
        static final DATE2 = new SimpleDateFormat('MM/dd')
        static final DATE3 = new SimpleDateFormat('MM/dd', DateFormatSymbols.instance)
        static date4 = new SimpleDateFormat('MM/dd', Locale.FRANCE)
        static date5 = new java.text.SimpleDateFormat('MM/dd')
    }

    // These usages are OK
    class MyCorrectClass {
        private SimpleDateFormat calendar1
        static ThreadLocal<SimpleDateFormat> calendar2
    }
```


## SynchronizedMethod Rule

This rule reports uses of the `synchronized` keyword on methods. Synchronized methods
are the same as synchronizing on 'this', which effectively make your synchronization policy
public and modifiable by other objects. To avoid possibilities of deadlock, it is better to
synchronize on internal objects.

Here is an example of code that produces a violation:

```
    synchronized def myMethod() {
        // do stuff ...
    }
```


## SynchronizedOnGetClass Rule

*Since CodeNarc 0.11*

Checks for synchronization on `getClass()` rather than class literal. This instance method
synchronizes on `this.getClass()`. If this class is subclassed, subclasses will synchronize
on the class object for the subclass, which isn't likely what was intended.


## SynchronizedOnBoxedPrimitive Rule

*Since CodeNarc 0.13*

The code synchronizes on a boxed primitive constant, such as an Integer. Since Integer objects can be cached and shared,
this code could be synchronizing on the same object as other, unrelated code, leading to unresponsiveness and possible
deadlock.

Example of violations:

```
    class MyClass {
        Byte byte1 = 100
        Short short1 = 1
        Double double1 = 1
        Integer integer1 = 1
        Long long1 = 1
        Float float1 = 1
        Character char1 = 1

        byte byte2 = getValue()
        short short2 = getValue()
        double double2 = getValue()
        int integer2 = getValue()
        long long2 = getValue()
        float float2 = getValue()
        char char2 = getValue()

        def byte3 = new Byte((byte)100)
        def short3 = new Short((short)1)
        def double3 = new Double((double)1)
        def integer3 = new Integer(1)
        def long3 = new Long(1)
        def float3 = new Float(1)
        def char3 = new Character((char)'1')

        def byte4 = 1 as byte
        def short4 = 1 as short
        def double4 = 1 as double
        def integer4 = 1 as int
        def long4 = 1 as long
        def float4 = 1 as float
        def char4 = 1 as char

        def byte5 = 1 as Byte
        def short5 = 1 as Short
        def double5 = 1 as Double
        def integer5 = 1 as Integer
        def long5 = 1 as Long
        def float5 = 1 as Float
        def char5 = 1 as Character

        def byte6 = (byte)1
        def short6 = (short)1
        def double6 = (double)1
        def integer6 = (int)1
        def long6 = (long)1
        def float6 = (float)1
        def char6 = (char)1

        def method() {
            // all of these synchronization blocks produce violations
            synchronized(byte1) {}
            synchronized(short1) {}
            synchronized(double1) {}
            synchronized(integer1) {}
            synchronized(long1) {}
            synchronized(float1) {}
            synchronized(char1) {}

            synchronized(byte2) {}
            synchronized(short2) {}
            synchronized(double2) {}
            synchronized(integer2) {}
            synchronized(long2) {}
            synchronized(float2) {}
            synchronized(char2) {}

            synchronized(byte3) {}
            synchronized(short3) {}
            synchronized(double3) {}
            synchronized(integer3) {}
            synchronized(long3) {}
            synchronized(float3) {}
            synchronized(char3) {}

            synchronized(byte4) {}
            synchronized(short4) {}
            synchronized(double4) {}
            synchronized(integer4) {}
            synchronized(long4) {}
            synchronized(float4) {}
            synchronized(char4) {}

            synchronized(byte5) {}
            synchronized(short5) {}
            synchronized(double5) {}
            synchronized(integer5) {}
            synchronized(long5) {}
            synchronized(float5) {}
            synchronized(char5) {}

            synchronized(byte6) {}
            synchronized(short6) {}
            synchronized(double6) {}
            synchronized(integer6) {}
            synchronized(long6) {}
            synchronized(float6) {}
            synchronized(char6) {}
        }
    }
```

And here is an in-depth example of how it works within inner classes and such:

```
    class MyClass {

        final String lock = false

        def method() {
            // violation
            synchronized(lock) { }
        }
    }

    class MyClass {

        final String lock = false

        class MyInnerClass {
            def method() {
                // violation
                synchronized(lock) { }
            }
        }
    }

    class MyClass {
        // implicit typing
        final def lock = true

        def method() {
            // violation
            synchronized(lock) { }
        }
    }

    class MyClass {
        // implicit typing
        final def lock = new Object[0] // correct idiom

        def method() {
            return new Runnable() {
                final def lock = false // shadows parent from inner class
                public void run() {
                    // violation
                    synchronized(stringLock) { }
                }
            }
        }
    }

    class MyClass {
        // implicit typing
        final def lock = new Object[0] // correct idiom

        class MyInnerClass {

            final def lock = true // shadows parent from inner class
            def method() {
                // violation
                synchronized(stringLock) { }
            }
        }
    }
```


## SynchronizedOnString Rule

*Since CodeNarc 0.13*

Synchronization on a String field can lead to deadlock. Constant Strings are interned and shared across all other
classes loaded by the JVM. Thus, this could is locking on something that other code might also be locking. This could
result in very strange and hard to diagnose blocking and deadlock behavior.

See [JETTY-352](http://www.javalobby.org/java/forums/t96352.html) and <http://jira.codehaus.org/browse/JETTY-352>.

Examples:

```
    class MyClass {

        final String stringLock = "stringLock"

        def method() {
            // violation
            synchronized(stringLock) { }
        }
    }

    class MyClass {

        final String stringLock = "stringLock"

        class MyInnerClass {
            def method() {
                synchronized(stringLock) { }
            }
        }
    }

    class MyClass {
        // implicit typing
        final def stringLock = "stringLock"

        def method() {
            // violation
            synchronized(stringLock) { }
        }
    }

    class MyClass {
        // implicit typing
        final def lock = new Object[0] // correct idiom

        def method() {
            return new Runnable() {
                final def lock = "" // shadows parent from inner class
                public void run() {
                    // violation
                    synchronized(stringLock) { }
                }
            }
        }
    }

    class MyClass {
        // implicit typing
        final def lock = new Object[0] // correct idiom

        class MyInnerClass {

            final def lock = "" // shadows parent from inner class
            def method() {
                // violation
                synchronized(stringLock) { }
            }
        }
    }
```


## SynchronizedOnThis Rule

This rule reports uses of the `synchronized` blocks where the synchronization reference
is 'this'. Doing this effectively makes your synchronization policy public and modifiable
by other objects. To avoid possibilities of deadlock, it is better to synchronize on internal objects.

Here is an example of code that produces a violation:

```
    def method3() {
        synchronized(this) {
            // do stuff ...
        }
    }
```


## SynchronizedReadObjectMethod Rule

*Since CodeNarc 0.13*

Catches Serializable classes that define a synchronized readObject method. By definition, an object created by
deserialization is only reachable by one thread, and thus there is no need for readObject() to be synchronized. If
the readObject() method itself is causing the object to become visible to another thread, that is an example of very
dubious coding style.

Examples:

```
    class MyClass implements Serializable {

        private synchronized void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
            // violation, no need to synchronized
        }
    }

    class MyClass implements Serializable {

        private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
            synchronized(lock) {
                // violation, no need to synchronized
            }
        }
    }

    // OK, class not Serializable
    class MyClass {

        private synchronized void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException { }
    }

    // OK, class not Serializable
    class MyClass {

        private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
            synchronized(lock) { }
        }
    }

    class MyClass implements Serializable {

        private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
            // OK, this block is more than just a simple sync statement
            synchronized(lock) { }
            doSomething()
        }
    }
```


## SynchronizedOnReentrantLock Rule

*Since CodeNarc 0.13*

Synchronizing on a ReentrantLock field is almost never the intended usage. A ReentrantLock should be obtained using
the lock() method and released in a finally block using the unlock() method.

This rule take from Alex Miller's [Java Concurrency in Practice](http://www.slideshare.net/alexmiller/java-concurrency-gotchas-3666977) slides.

Here is the proper usage of ReentrantLock:

```
    import java.util.concurrent.locks.ReentrantLock;
    final lock = new ReentrantLock();
    def method()  {
       //Trying to enter the critical section
       lock.lock(); // will wait until this thread gets the lock
       try {
          // critical section
       } finally {
          //releasing the lock so that other threads can get notifies
          lock.unlock();
       }
    }
```

Example of violations:

```
    class MyClass {

        final ReentrantLock lock = new ReentrantLock()

        def method() {
            // violation
            synchronized(lock) { }
        }
    }

    class MyClass {

        final ReentrantLock lock = new ReentrantLock()

        class MyInnerClass {
            def method() {
                synchronized(lock) { }
            }
        }
    }

    class MyClass {
        // implicit typing
        final def lock = new ReentrantLock()

        def method() {
            // violation
            synchronized(lock) { }
        }
    }

    class MyClass {
        // implicit typing
        final def lock = new Object[0] // correct idiom

        def method() {
            return new Runnable() {
                final def lock = new ReentrantLock() // shadows parent from inner class
                public void run() {
                    // violation
                    synchronized(lock) { }
                }
            }
        }
    }

    class MyClass {
        // implicit typing
        final def lock = new Object[0] // correct idiom

        class MyInnerClass {

            final def lock = new ReentrantLock() // shadows parent from inner class
            def method() {
                // violation
                synchronized(lock) { }
            }
        }
    }
```


## SystemRunFinalizersOnExit Rule


This rule reports uses of the `System.runFinalizersOnExit()` method.

Method calls to `System.runFinalizersOnExit()` should not be allowed. This method is inherently
non-thread-safe, may result in data corruption, deadlock, and may affect parts of the program
far removed from it's call point. It is deprecated, and it's use strongly discouraged.

Here is an example of code that produces a violation:

```
    def method() {
        System.runFinalizersOnExit(true)
    }
```


## ThisReferenceEscapesConstructor Rule

*Since CodeNarc 0.19*

Reports constructors passing the 'this' reference to other methods.
This equals exposing a half-baked objects and can lead to race conditions during initialization.
For reference, see [Java Concurrency in Practice](http://www.slideshare.net/alexmiller/java-concurrency-gotchas-3666977/38) by Alex Miller
and [Java theory and practice: Safe construction techniques](http://www.ibm.com/developerworks/java/library/j-jtp0618/index.html) by Brian Goetz.

Example of violations:

```
    class EventListener {
        EventListener(EventPublisher publisher) {
            publisher.register(this)
            new WorkThread(publisher, this).start()
            new AnotherWorkThread(listener: this)
        }
    }
```


## ThreadGroup Rule

*Since CodeNarc 0.13*

Avoid using `ThreadGroup`; although it is intended to be used in a threaded environment it contains methods
that are not thread safe.

Here is an example of code that produces a violation:

```
    new ThreadGroup("...")
    new ThreadGroup(tg, "my thread group")
    Thread.currentThread().getThreadGroup()
    System.getSecurityManager().getThreadGroup()
```


## ThreadLocalNotStaticFinal Rule


This rule reports definition of the `ThreadLocal` fields that are not `static` and `final`.

*ThreadLocal* fields should be `static` and `final`. In the most common case a
`java.lang.ThreadLocal` instance associates state with a thread. A non-`static`
non-`final` `java.lang.ThreadLocal` field associates state with an instance-thread combination.
This is seldom necessary and often a bug which can cause memory leaks and possibly incorrect behavior.

Here is an example of code that produces a violation:

```
    private static ThreadLocal local1 = new ThreadLocal()
    private final ThreadLocal local2 = new ThreadLocal()
    protected ThreadLocal local3 = new ThreadLocal()
    ThreadLocal local4 = new ThreadLocal()
```


## ThreadYield Rule


This rule reports uses of the `Thread.yield()` method.

Method calls to `Thread.yield()` should not be allowed. This method has no useful guaranteed
semantics, and is often used by inexperienced programmers to mask race conditions.

Here is an example of code that produces a violation:

```
     def method() {
         Thread.yield()
     }
```


## UseOfNotifyMethod Rule

*Since CodeNarc 0.11*

Checks for code that calls `notify()` rather than `notifyAll()`. Java monitors are often used
for multiple conditions. Calling `notify()` only wakes up one thread, meaning that the awakened
thread might not be the one waiting for the condition that the caller just satisfied.

Also see [**Java_Concurrency_in_Practice**](http://www.javaconcurrencyinpractice.com/), Brian Goetz, p 303.


## VolatileArrayField Rule

*Since CodeNarc 0.13*

Volatile array fields are unsafe because the contents of the array are not treated as volatile. Changing the entire
array reference is visible to other threads, but changing an array element is not.

This rule take from Alex Miller's *Java Concurrency in Practice* slides, available at
<http://www.slideshare.net/alexmiller/java-concurrency-gotchas-3666977>

Example of violations:

```
    class MyClass {
        private volatile Object[] field1 = value()
        volatile field2 = value as Object[]
        volatile field3 = (Object[])foo
    }
```

## VolatileLongOrDoubleField Rule

This rule reports on `long` or `double` fields that are declared `volatile`.

Long or double fields should not be declared as `volatile`. Java specifies that reads and
writes from such fields are atomic, but many JVM's have violated this specification. Unless you
are certain of your JVM, it is better to synchronize access to such fields rather than declare
them `volatile`. This rule flags fields marked `volatile` when their type is `double`
or `long` or the name of their type is "Double" or "Long".

Here is an example of code that produces a violation:

```
     def method() {
         private volatile double d
         private volatile long f
     }
```


## WaitOutsideOfWhileLoop Rule

*Since CodeNarc 0.13*

Calls to `Object.wait()` must be within a `while` loop. This ensures that the awaited condition
has not already been satisfied by another thread before the `wait()` is invoked. It also ensures that
the proper thread was resumed and guards against incorrect notification. See [1] and [3].

As a more modern and flexible alternative, consider using the Java *concurrency utilities* instead of
`wait()` and `notify()`. See discussion in *Effective Java* [2].

Example of violation:

```
    class MyClass {
        private data

        void processData()
            synchronized(data) {
                if (!data.isReady()) {
                    data.wait()
                }
                data.calculateStatistics()
            }
        }
    }
```

Example of correct usage:

```
    class MyClass {
        private data

        void processData()
            synchronized(data) {
                while (!data.isReady()) {
                    data.wait()
                }
                data.calculateStatistics()
            }
        }
    }
```

### References

  * [1] **Effective Java, Programming Language Guide**, by Joshua Bloch. Addison Wesley (2001).
    Chapter 50 (1st edition) is entitled "Never invoke wait outside a loop."

  * [2] **Effective Java**, 2nd edition, by Joshua Bloch, Addison Wesley (2008).
    Item #69: *Prefer concurrency utilities to wait and notify*.

  * [3] Software Engineering Institute - Secure Coding
     [discussion of this issue](https://www.securecoding.cert.org/confluence/display/java/THI03-J.+Always+invoke+wait()+and+await()+methods+inside+a+loop)

