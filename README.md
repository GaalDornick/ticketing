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

##Design

###First a comment on the problem
This particular problem is very analogous to a "Disk allocation" algorithm, except for a few differrences. In computer systems, there is frequently a disk that stores the user's data. The disk is made up of plates. The plates are made up of tracks. The tracks are made up of bytes. Similarily, in this problem, the Orchestra is made up of levels, the levels are made up of rows and rows are made up of seats. In a disk drive, it's advantegous to put the contents of the file in as many contigious bytes as possible. Similarily, in this problem, it's better to put the users in as many contigious seats as possible. 
The key differrences are
- In a disk storage, the size of the file is made up of many bytes. FIle sizes frequently run into 1000s of bytes, and might even go upto billions. On the other hand, customers will usually reserve 2-3 seats at a time. An algorithm that stores files onto a disk can afford to waste some bytes to make access faster. On the other hand, the ticketing system cannot waste seats
- In a ticketing system, there is a low cost to compacting the reservations because, the customers can be informed by email before they come to the theater. On the other hand, compacting files on a disk is costly since it requires physically moving data

As such, any good algorithm that can allocate the disk's file system to minimize fragmentation can be retro-fitted solve this problem. I am sure that greater minds than mine with more time on their hands have solved this problem in better ways. My assumption is that this test is a test of finding an algorithmic solution to a proble. So, I have refrained from googling for algorithms, and tried to come up with my own. In "real life", I would have found an existing solution rather than re-invent one

###Program structure