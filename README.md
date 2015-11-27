# Ticketing
Ticketing service for Walmart

##Assumptions
There were a few assumptions made during the design process

- When a user tries to allocate seats in multiple levels, the system will allocate seats in the costliest(lowest) level first before allocating in cheaper (higher) levels
- When a user tries to allocate more seats that can be fit in a row, the system will try to minimize the number of rows used. For example, instead of putting 18 seats in 3 rows on 6, it will put them in 2 rows of 9
- Everything else being equal, the system will assign the seats in the lower numbered rows
- When lot of users are requesting for seats in the same level, the system will throw an error to some of the users instead of making them wait for a long time. THis allows the calling application to show a "Please try again"  message to the user rather than make them wait for an indefinite period
- Only a small percentage of holds will expire
- When the user reserves a seat(s), s/he will only be informed of which row the seat has been reserved for. The actual seating assignment will be provided after all reservations are complete. THis will be done either by the customer picking up the tickets at a "will-call" window, or an email will be sent to the customer

##Running the applications

You need to install maven to run the application. It contains an implementation of a ticketing service and a Cucumber test that tests the service. Simply run *mvn clean install* to build the application and run the tests. You are encouraged to add your own cucumber tests to the ticketing.feature file to run the Ticketing service in a scenario that you want to run. 

##Design

###First a comment on the problem
This particular problem is very analogous to a "Disk allocation" algorithm, except for a few differrences. In computer systems, there is frequently a disk that stores the user's data. The disk is made up of plates. The plates are made up of tracks. The tracks are made up of bytes. Similarily, in this problem, the Orchestra is made up of levels, the levels are made up of rows and rows are made up of seats. In a disk drive, it's advantegous to put the contents of the file in as many contigious bytes as possible. Similarily, in this problem, it's better to put the users in as many contigious seats as possible. 
The key differrences are
- In a disk storage, the size of the file is made up of many bytes. FIle sizes frequently run into 1000s of bytes, and might even go upto billions. On the other hand, customers will usually reserve 2-3 seats at a time. An algorithm that stores files onto a disk can afford to waste some bytes to make access faster. On the other hand, the ticketing system cannot waste seats
- In a ticketing system, there is a low cost to compacting the reservations because, the customers can be informed by email before they come to the theater. On the other hand, compacting files on a disk is costly since it requires physically moving data

As such, any good algorithm that can allocate the disk's file system to minimize fragmentation can be retro-fitted solve this problem. I am sure that greater minds than mine with more time on their hands have solved this problem in better ways. My assumption is that this test is a test of finding an algorithmic solution to a proble. So, I have refrained from googling for algorithms, and tried to come up with my own. In "real life", I would have found an existing solution rather than re-invent one

I did use strategies that disk allocation algorithms use
- try to assign contigious seats
- compact the seat assignment when holds  expire. 

###Program structure
The program contains of 3 modules
- Ticketing module
- Stadium module
- Reservations module
 
The Stadium module is responsible for managing the state of the seats in the stadium. It keeps track of which seats have been allocated and which are free. It is also responsoble for allocating, deallocating and comapcting the seats. The Reservations module is responsible for managing the reservations and holds. It keeps track of which seats are held and which are reserved and which have expired. The Ticketing module is responsible for providing a public API for the ticketing service. Intenrally it uses the 2 other 

Please refer to javadocs in the classes for more details

The Ticketing service doesn't contains a UI, and it doesn't persist the data. It can be easily extended to persist the data in a database. ALso, it can be easily exposed as a REST service, which allows us to bbuold an UI

###Concurrency
Since the application is meant to be used in a high demand environment, it can be expected to receive many requests concurrently. However, the requirements themselves lend to the problem of shared state. WHen a seat is being assigned to one request, it shouldn't be assigned to other. This means that we need to protect the state from concurrent access

A naive implementation would simply put each seat in it's own synchronization monitor, and allow concurrent threads to compete for access to the seat's state by competing for the synchronization monitor. However, this has compule of drawbacks
- Starvation - Since Operating systems don't release threads waiting on a monitor on a first-come first-served basis, there is a risk that some requests might starve
- High contention means high load - Requests wiating on monitors ain't cheap
- Starvation means additional load - When an user's request is on a starved thread, the user is likely to press refresh again. This means that not only will the system be nade to assign new seats to the same user, the previous holds would expire and would need to be deallocated

In cases of high demand, it is better to fail fast. If the number of users exceeds a certain limit, it's better to tell the user to try again, **even if the same request would have been succesful under low-load conditions** This results in lower load on the system and better user experience

So, to reduce contention, this implementation uses the concept of "checking out" rows. When seats in a particular row are being assigned to the user, the system *checks out* the whole row. After assigning seats, it *checks in* the row. A row can be checked out to one request only. SO, when a request has checked out one or multiple rows, they won't be available to other requests until the request is completed. This has multiple advantages
-Reduction on scope of contention - The implementation synchronizes only on the collection that holds the rows. Once a row is checked out, it is removed from the collection. When it is checked in, it is added back into the collection. All operations on the row occur only when it's checked out. This means that all operations on the row itself will be called from one thread only
-Fail fast - When all rows are checked out, the system will complain that no more seats are available. THis means that if there are too many concurrent requests for the same level, a few unlucky ones will fail

The side effect is that some of these failures will be false fails. The system will complain that there are no more seats, even if there are seats. If a 100 seat row is checked out by a request that is assigning 3 seats, all 100 seats are unavailable to other requests until the first request is done. This means that the stadium will be 97 seats short momentarily. As explained above, in condition of high concurrency winds, it's better to fail than to contend. 
