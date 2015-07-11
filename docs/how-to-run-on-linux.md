# Running the Meta64 Server (Simple way)

* Note that the spring.config.location argument (as in the command below) you are required to include the main application.properties file as well as one of the profile ones (ending in -test, -dev, or -production)

    Sell script content for Linux:
    
    nohup java -jar com.meta64.mobile-0.0.1-SNAPSHOT.jar --jcrAdminPassword=yourPasswordHere --spring.config.location=classpath:/application.properties,classpath:/application-test.properties &
    
* Note, the above is not the "best" way to start an app on linux, but is the simplest.

## Get your Process ID

Run this command, to find your java Process ID

    jps -ml

## Killing the Process by ID

To stop a process by PID (NNNN=PID you got from jps command):
	
    kill NNNN	

## Or Even better (Kill the process on one step)

    pkill -f com.meta64.mobile
    
Looks up the process(es) that contain 'com.meta64.mobile' and kills them. This should safely target and kill the meta64 app. See also 'pgrep' which lists them without killing.

----

# Running the Meta64 Server (Proper way for Production)

Linux service init script are stored into /etc/init.d. You can copy and customize /etc/init.d/skeleton file, and then call

    service [yourservice] start|stop|restart
    
see: http://www.ralfebert.de/archive/java/debian_daemon/
(cf: I have not tired this yet)    
   
    