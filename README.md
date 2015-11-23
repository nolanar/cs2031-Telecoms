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
