* I used MacOS + Java17 to build
* that's why I had to create an executable jar (with spring-boot) and copy it to the target machine
* the original problem exists on vcan and on a regular can-device
* built my stuff with 
  ```
  mvn clean verify -DskipTests -pl canfd-test -am -P-single-architecture` on root
  ```
* execute
  ```
  java -Dcan=vcan0 -jar canfd-test.jar read|write`
  ```
* Terminal1: 
  ```
  java -Dcan=vcan0 -jar canfd-test.jar read
  ```
* Terminal2: 
  ```
  candump vcan0
  ```
* Terminal3:
  ```
  java -Dcan=vcan0 -jar canfd-test.jar write
  # non canfd frame: 
  cansend vcan0 100#00
  # canfd-frame with <=8 bytes 
  cansend vcan0 101##000
  ```
