# Flow Control

This is a java implementation of various flow control protocols commonly used in data-communication, namely:
* Stop and Wait ARQ
* Go-Back-N ARQ
* Selective Repeat ARQ

See the following article for my detailed analysis:
[Flow Control](http://nolanar.github.io/articles/flow-control.html).

To summarise, these protocols are used when every data-packet sent must be received and processed in the correct order.
* **Stop and Wait** sends a single packet repeatedly until it is acknowledged as received.
* **Go-Back-N** sends a *window* of packets, but receives one at a time.
* **Selective Repeat** sends a window of packets, and can receive packets out of order within a window.

### Demo

Three demo classes are available, one for each of the protocols. Configuration options for these include:
* Size of windows.
* Rate of packet loss.
* Whether debug messages are displayed.

The GUI uses the [DragonConsole v3](https://github.com/bbuck/dragonconsole) library. This offers a terminal style window. 

## Implementation

The window classes `SenderWindow` and `ReceiverWindow` (and there data type classes) essentially are the implementation of the protocols. The remaining classes are there to facilitate in using these. These include various client, server, and packet classes.

### Window Data-Structure

The `ArrayBlockingList` class is the data-structure that models the windows. It is similar to the [`ArrayBlockingQueue`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ArrayBlockingQueue.html) class found in the Java [`concurrent`](http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/package-summary.html) collection, with added functionality:
* You can insert into the middle of the list by index.
* You can get elements from anywhere in the list by index.

If an inserted element links previously floating elements (a `null` as the previous element) to the queue, these all become part of the queue.

### Timer

The packets are repeatedly sent until an acknowledgement is received to say they got through. A timer is needed for this. Rather than creating an instance of a timer for each sent packet (it is messy trying to get separate threads to operate in lock-step), a [`DelayQueue`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/DelayQueue.html) is used instead. 

Whenever a packet is sent, it is added to the `DelayQueue`, with a fixed delay. When this delay expires it is taken from the queue, sent again, and put back into the queue with another delay. 

A negative-acknowledgement for a given packet causes the packet to be removed from the `DelayQueue` immediately, sent again, and added back into the queue with a delay.

### Using the window classes

`SenderWindow` and `ReceiverWindow` are abstract classes. A new instance of these classes must implement the input and output methods:
##### `getPacket()`
This must be a blocking method that gets packets. For example: Used in the `Client` class to create packets from strings stored in the `Terminal`'s input buffer, which is a [`LinkedBlockingQueue`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/LinkedBlockingQueue.html).
##### `sendPacket()`
This method should be used to send the specified packet to it's destination. For example: Put the packet in a buffer that will send the packet at the appropriate time.
##### `outputPacket()`
This method should be used to handle the packets that have been successfully received in the correct order. For example, used in the `Server` class to print to its `Terminal` messages sent by the `Client`.
