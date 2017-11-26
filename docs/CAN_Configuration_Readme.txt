How to Configure CAN to work with the NMF:

- The MO transport layer that talks CAN, uses a library available online called kayak.
- The mityARM default OS already includes the low-level socketCAN drivers installed.
- Kayak connects to a deamon for CAN called SocketCANd that provides access to CAN via a network interface.

The complete stack looks like this:

--------------
 MO services
--------------
     MAL
--------------
     SPP
--------------
     CFP
--------------		      --------------
    kayak			socketcand
--------------                --------------
      |                              |
      |                              |
      |------------------------------|


SocketCANd doesn't come by installed by default and need to be compiled and installed for the MityARM.


Read:
https://github.com/dschanoeh/socketcand
https://en.wikipedia.org/wiki/SocketCAN


---------------------
Compilation
---------------------
To Compile it is necessary to install the compilation kit for mityARM that uses eclipse

0.Original
	Original version of socketcand

1.Configured_for_ARM
	Contains the configuration files that were generated during the build with eclipse

2.Maked_for_ARM
	Contains the files created after doing "make" with a Raspberry PI. So, it contains the compiled code for ARM.


---------------------
Instalation
---------------------

Update the time and date of the MityARM to the current one:

>>date -s "dd MMM yyyy hh:mm:ss.xxx UTC" | hwclock --systohc

Concrete example:
>>date -s "16 Apr 2016 16:35:00.000 UTC" | hwclock --systohc

Execute:
	sudo make install

Give permissions to the files on the folder /usr/local/bin with:
	chmod 755 -R /usr/local/bin


----------------------------------------------
CAN Linux Interface Configuration
----------------------------------------------

Execute:
	sudo ip link set can0 type can bitrate 1000000 triple-sampling on
	sudo ip link set dev can0 up


High bitrates is only possible for terminated ends

---------------------


----------------------------------------------
Configuring CAN Interface during startup
----------------------------------------------

Automatically configure the ccan interface during startup
Edit the file: /etc/network/interfaces

Add the following lines:

---
# CAN interface
auto can0
iface can0 inet manual
	# bitrate 1MBps with triple-sampling on
	pre-up /sbin/ip link set can0 type can bitrate 1000000 triple-sampling on
	up /sbin/ifconfig can0 txqueuelen 10000 up
	down /sbin/ifconfig can0 down
---


---------------------
Socketcand start
---------------------

Socketcand was installed in the folder /usr/local/bin

One can start it with the command:
	socketcand -v -i can0 -l localhost


----------------------------------------------
Configuring SocketCANd to start during startup
----------------------------------------------

Edit the file: /etc/init.d/socketcand
From: 
	start-stop-daemon --start --quiet --background --pidfile $PIDFILE --startas $DAEMON -m -- --daemon

To:
	start-stop-daemon --start --quiet --background --pidfile $PIDFILE --startas $DAEMON -m -- --daemon --interfaces can0 --listen localhost


Execute the command:
	update-rc.d socketcand defaults


Restart the MityARM


----------------------------------------------
Other commands that might come useful:
----------------------------------------------

ip -details -statistics link show can0
sudo ./candump can0 -a
sudo ./cansend can0 256#112233
sudo ./cansend can0 12345678#01.02.03.04.00.00.00.00


CAN bus Load:
./canbusload can0@1000000 -r -t -b -c


This might solve some crashing problems:
ifconfig can0 txqueuelen 10000

	sudo ip link set can0 type can bitrate 1000000 triple-sampling on
	sudo ip link set can0 type can tq 50 prop-seg 7 phase-seg1 7 phase-seg2 5 sjw 1
	sudo ip link set dev can0 up
	socketcand -v -i can0 -l localhost


echo "< add 1 0 123 8 11 22 33 44 55 66 77 88 >" | nc 0.0.0.0 29536

echo "< open can0 > < send 12345678 8 01 02 03 04 05 06 07 08 >" | nc 0.0.0.0 29536

echo "< open can0 > < add 1 0 123 8 11 22 33 44 55 66 77 88 >" | nc 0.0.0.0 29536


echo "< open can0 > < bcmmode > < send 123 0 >" | nc 0.0.0.0 29536




