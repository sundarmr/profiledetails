Command to Synch up Ensemble Environment with a configuration file

# Command to Print Configuration

`container-profile-synch --jmxuser <username> --jmxPassword <password>`

The above command will print out a json string with all the container details  

* Profiles associated with the container
* Bundles , fabs , features , repositories , configurations associated with the Container ( Read Profile )
* Camel Contexts running in the container.
  * No of routes for each context.
  * State of the route
  
  `container-profile-synch --jmxuser <username> --jmxPassword <password> --remoteUser <remoteusername> --remotePassword <remotesystempassword> --synchContexts false --child <true/false> --oldIdentified <sourcesytemnamingstring> --newIdentifier <targetsystemnamingstring> --hosts <remotehostname> <filePath>`
  
  The above command will read the file from filepath and apply the configurations to the target environments and synch up the ensemble to resemble the source configuration
  
 * Will Create New containers if missing in targe
 * Will create / update the profiles to reflect the source system profiles
 * Will synch up associations of the containers and profiles

   `container-profile-synch --jmxuser <username> --jmxPassword <password> --remoteUser <remoteusername> --remotePassword <remotesystempassword> --synchContexts only --child <true/false> --oldIdentified <sourcesytemnamingstring> --newIdentifier <targetsystemnamingstring> --hosts <remotehostname> <filePath>`
  
  The above command will read the file from filepath and apply the configurations to the target environments and synch up the ensemble to resemble the source configuration
  
 * Will synch up camel contexts in the target environment to reflect source
 * Will try to start up the contexts where possible
