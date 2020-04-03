Command to Synch up Ensemble Environment with a configuration file

# Command to Print Configuration

`container-profile-synch  --remoteUser <user> --remotePassword <password> --storeFile <FilePath>`

The above command will store the configuration of the ensemble to the file path provided at --storeFile , if the option is not provided will default to /tmp/config.json  

* Profiles associated with the container
* Bundles , fabs , features , repositories , configurations associated with the Container ( Read Profile )
* Camel Contexts running in the container.
  * No of routes for each context.
  * State of the route
  
`container-profile-synch  --jmxuser <user> --jmxPassword <password> --contextFromFabric false --storeFile <FilePath>`

The above command will do what exactly the first command does but will get the context information from jmx instead of fabric

`container-profile-synch  --remoteUser <remoteusername> --remotePassword <remotesystempassword> --synchContexts false --child <true/false> --environment <d/q/s/p> --zoneName <edc/rdc> --autoRecheckCount 10 --runStatusCheck true --statusCheckInterval 60000 <configFilePath>`

							
The above command will read the file from filepath and apply the configurations to the target environments and synch up the ensemble to resemble the source configuration
*Will Create New containers if missing in target
*Will create / update the profiles to reflect the source system profiles
*Will synch up associations of the containers and profiles	
*Once all the containers have been created or synched up the code will run for every statusCheckInterval to verify the ensemble environment to check if all the containers are provisioned and if not will try and correct their state so that they are given an opportunity to start fast. This will occur autoRecheckCount times.						


`container-profile-synch  --remoteUser <remoteusername> --remotePassword <remotesystempassword> --synchContexts false --child <true/false> --environment <d/q/s/p> --zoneName <edc/rdc> --autoRecheckCount 10 --runStatusCheck true --statusCheckInterval 60000 --demoRun <configFilePath>`

The above command will create a log trail of activities that it would perform if run actually and with a demoRun , this can be useful to understand what the environment will look like and how many profiles and containers will be modified.
  

`container-profile-synch --checkAndRestartContainers true`

 The above command will check all the existing containers in the ensemble and restart the ones which are not provisioned , errored or stopped. It will identify the inconsistencies in the container and attempt to restore the containers.
