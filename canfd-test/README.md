* I used MacOS + Java17 to build
* that's why I had to create an executable jar (with spring-boot) and copy it to the target machine

  `java -Dcan=vcan0 -jar canfd-test.jar read|write`
* the problem exists on pure vcan and on regular can-device

* Terminal1: `java -Dcan=vcan0 -jar canfd-test.jar read`
* Terminal2: `candump vcan0`
* Terminal3:
  * `java -Dcan=vcan0 -jar canfd-test.jar write`
  * regular can frame: `cansend vcan0 100#00`
  * canfd-frame with <=8 bytes `cansend vcan0 101##000`
