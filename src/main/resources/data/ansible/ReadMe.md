Copy the jboss-a-mq zip file to the ActiveMqNob directory before running the script

Prerequisites

1. Vagrant 
2. Java
3. Ansible 

run vagrant up to get a cluster of 4 activemq nodes 

1. First Run the command ansible-playbook copyConfig.yml , this will connect to the provided host source in the host file and get the config.json to the local directory as in the groupvars/fuse localPath
2. Then run the command ansible-playbook synch.yml , this will connect to the target node and execute the command to synch it
