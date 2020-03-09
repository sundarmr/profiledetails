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
 
 
`container-profile-synch --help`
`DESCRIPTION`
        `fabric:container-profile-synch`

	`Displays all profiles with associated containers and bundles list , provide an argument for a specific profile`

`SYNTAX`
        `fabric:container-profile-synch [options] [filePath] `

`ARGUMENTS`
        `filePath`
                `Path to the profile.`

`OPTIONS`
        `--resolver`
                `The resolver policy for this container(s). Possible values are: localip, localhostname, publicip, publichostname, manualip. Defaults to the fabric's default resolver policy.`
       ` -m, --manual-ip`
               ` An address to use, when using the manualip resolver.`
        `-b, --bind-address`
                `The default bind address.`
        `--datastore-option`
               ` Options to pass to the container's datastore. To specify multiple options, use this flag multiple times.`
       ` --remoteUser`
                `Remote user in case if we need to create a missing container`
       ` --oldIdentifier`
                `environment identifier for source`
       ` --pass-phrase`
                `The pass phrase of the key. This is for use with private keys that require a pass phrase.`
        --jmxPassword
                JmxPassword
        --ensemble-server
                [Deprecated] Whether the new container should be a fabric ensemble server (ZooKeeper ensemble server). Note that this does not add the new container to the current fabric but creates a wholly new fabric.
        --hosts
                hosts on which containers need to be created
        --child
                If missing containers should be child (true ) or ssh (false ) 
        --remotePassword
                Remote user password to ssh to the host
        --jmxuser
                JmxUser
        --zookeeper-password
                The ensemble password to use (one will be generated if not given)
        --synchContexts
                Should contexts be synched up takes 
                 1. true : does synch along with profile synch activity 
                2. false : does not synch up contexts 
                3. only synchs contexts 
        --profile
                The profile IDs to associate with the new container(s). For multiple profiles, specify the flag multiple times. Defaults to the profile named "default".
        --help
                Display this help message
        --newIdentifier
                environment identifier for target
        --version
                The version of the new container (must be an existing version). Defaults to the current default version.
        --noOfThreads
                No of threads to execute
                (defaults to 10)
        --jvm-opts
                Options to pass to the container's JVM.
        --private-key
                The path to the private key on the filesystem. Default is ~/.ssh/id_rsa on *NIX platforms or C:\Documents and Settings\<UserName>\.ssh\id_rsa on Windows.

 `
