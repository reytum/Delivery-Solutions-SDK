# Delivery-Solutions-SDK
Android SDK for Delivery solutions

## Installation:
1) Clone this repo to a directory and checkout the master branch.
2) In your android studio select File -> New -> Import module -> Select delivery-solution modules from the cloned location.
3) In your top level build.gradle file add the below maven repo:
    ```
       maven {
            url("${project(':background-geolocation').projectDir}/libs")
        }
    ```
    
## Start tracking location:
 To start location tracking, call the below function:
 
 ```java
   DeliverySolutions.startLocationMonitoring(activityContext);  //Only Activity context is accepted
 ```

