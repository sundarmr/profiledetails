
package org.redhat.fabric.commands;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
import org.apache.mina.util.ConcurrentHashSet;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.redhat.fabric.commands.model.Context;
import org.redhat.fabric.commands.model.EnsembleContainer;
import org.redhat.fabric.commands.model.ProfileDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import io.fabric8.api.Container;
import io.fabric8.api.CreateChildContainerOptions;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileService;
import io.fabric8.api.Version;
import io.fabric8.api.VersionBuilder;
import io.fabric8.api.ZooKeeperClusterService;
import io.fabric8.boot.commands.support.AbstractContainerCreateAction;
import io.fabric8.boot.commands.support.FabricCommand;
import io.fabric8.service.ssh.CreateSshContainerOptions;
import io.fabric8.service.ssh.CreateSshContainerOptions.Builder;

@Command(name = AssociatedContainers.FUNCTION_VALUE, scope = AssociatedContainers.SCOPE_VALUE, description = AssociatedContainers.DESCRIPTION)
public class AssociatedContainersAction extends AbstractContainerCreateAction {

	Logger LOG = LoggerFactory.getLogger(AssociatedContainersAction.class);

	@Argument(index = 0, required = false, name = "filePath", description = "Path to the profile.")
	@CompleterValues(index = 0)
	private String filePath;

	@Option(name = "--child", description = "If missing containers should be child (true ) or ssh (false ) ")
	private String child;

	@Option(name = "--jmxuser", description = "JmxUser", required = true)
	private String jmxuser;

	@Option(name = "--jmxPassword", description = "JmxPassword", required = true)
	private String jmxPassword;

	@Option(name = "--remoteUser", description = "Remote user in case if we need to create a missing container")
	private String remoteUser;

	@Option(name = "--remotePassword", description = "Remote user password to ssh to the host")
	private String remotePassword;

	@Option(name = "--hosts", description = "hosts on which containers need to be created", multiValued = true)
	private List<String> hostsToCreateContainers;

	@Option(name = "--synchContexts", description = "Should contexts be synched up takes \n 1. true : does synch along with profile synch activity \n2. false : does not synch up contexts \n3. only synchs contexts ")
	private String synchContexts;

	@Option(name = "--private-key", description = "The path to the private key on the filesystem. Default is ~/.ssh/id_rsa on *NIX platforms or C:\\Documents and Settings\\<UserName>\\.ssh\\id_rsa on Windows.")
	private String privateKeyFile;

	@Option(name = "--pass-phrase", description = "The pass phrase of the key. This is for use with private keys that require a pass phrase.")
	private String passPhrase;

	@Option(name = "--noOfThreads", description = "No of threads to execute")
	private int noOfThreads = 10;

	@Option(name = "--storeFile", description = "Path to write the config file defaults to /tmp/config.json")
	private String storeFile = "/tmp/config.json";

	@Option(name = "--zoneName", description = "which zone are the containers being created in")
	private String zoneName;

	@Option(name = "--environment", description = "environments test-t , dev-d production-p and staging -s")
	private String environment;

	@Option(name = "--ignoreContainer", description = "the container in which the management code is runningge")
	private String ignoreContainer;

	@Option(name = "--path", description = "Path where the container is to be created in remote server")
	private String path = "/ifs/fuse/";

	@Option(name = "--checkAndRestartOnly", description = "Path where the container is to be created in remote server")
	private String checkAndRestartContainers;

	@Option(name = "--baseVersion", description = "Ensembles base version from which other versions are derived")
	private String baseVersion = "1.0";

	@Option(name = "--autoRecheckCount", description = "No of times the thread checks to see if all the containers have started")
	private Integer autoRecheckCount = 10;

	@Option(name = "--runStatusCheck", description = "The code will automatically run the background for 100 minutes and check for the status of the containers every 10 minutes and restart the containers where applicable")
	private String runStatusCheck;

	@Option(name = "--demoRun", required = true, description = "Simulates the actual actions that will be run")
	private Boolean demoRun;
	
	@Option(name = "--statusCheckInterval",  description = "Time Interval to check  the status of the containers after creation")
	private long statusCheckInterval=600000L;

	static final List<String> ignoreProfiles = new ArrayList<String>() {
		{

			add("openshift");
			add("shss_shss_ansible");
			add("default");
			add("fabric-ensemble-0000-1");
			add("fabric");
			add("fabric-ensemble-0001");
			add("fabric-ensemble-0000");
			add("fabric-ensemble-0001-1");
			add("fabric-ensemble-0001-3");
			add("autoscale");
			add("fabric-ensemble-0001-2");
			add("acls");
			add("fabric-ensemble-0001-4");
			add("fabric-ensemble-0001-5");
			add("feature-camel");
			add("feature-camel-jms");
			add("feature-cxf");
			add("feature-fabric-web");
			add("gateway-http");
			add("feature-dosgi");
			add("hawtio");
			add("gateway-mq");
			add("insight-camel");
			add("insight-activemq");
			add("insight-core");
			add("insight-elasticsearch.basicauth");
			add("insight-elasticsearch.datastore");
			add("insight-console");
			add("insight-elasticsearch.node");
			add("insight-metrics.elasticsearch");
			add("insight-logs.elasticsearch");
			add("insight-metrics.base");
			add("jboss-fuse-full");
			add("karaf");
			add("mq-amq");
			add("mq-client-default");
			add("mq-client-base");
			add("jboss-fuse-minimal");
			add("mq-default");
			add("mq-base");
			add("mq-client");
			add("mq-client-local");

		}
	};

	private static final HashMap<String, String> secondNumber;
	private static final HashMap<String, String> firstNumber;
	private static final HashMap<String, String> containerFirstLetter;
	private static final HashMap<String, String> serverFirstLetter;

	static {
		secondNumber = new HashMap<String, String>();
		secondNumber.put("bay", "2");
		secondNumber.put("ent", "4");
		secondNumber.put("val", "3");
		firstNumber = new HashMap<String, String>();
		firstNumber.put("d", "9");
		firstNumber.put("q", "8");
		firstNumber.put("s", "2");
		firstNumber.put("p", "1");

		containerFirstLetter = new HashMap<String, String>();
		containerFirstLetter.put("edc", "1");
		containerFirstLetter.put("rdc", "2");

		serverFirstLetter = new HashMap<String, String>();
		serverFirstLetter.put("edc", "dc");
		serverFirstLetter.put("rdc", "rd");
	}

	public AssociatedContainersAction(FabricService fabricService, ZooKeeperClusterService zooKeeperClusterService) {
		super(fabricService, zooKeeperClusterService);
	}

	public FabricService getFabricService() {
		return fabricService;
	}

	@Override
	protected Object doExecute() throws Exception {
		ProfileService profileService = fabricService.adapt(ProfileService.class);
		PrintStream out = System.out;
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		if ("true".equalsIgnoreCase(checkAndRestartContainers)) {
			checkAndRestartContainers();
		} else {

			if (child != null && filePath != null) {

				if ((remoteUser == null || remotePassword == null) && privateKeyFile == null) {
					System.err.println(Ansi.ansi().fg(Color.RED).a("Error Executing Command: ").a(
							"Remote User and Password / Private Key is needed  use options --remoteUser and --remotePassword\n")
							.fg(Ansi.Color.DEFAULT).toString());
				}
			}
			if (child != null && filePath == null) {
				System.out.println(Ansi.ansi().fg(Color.RED).a("Warning Executing Command: ").a(
						"Generating configuration file , if the intention of using the command is to synch then please provide the path to the configuraiton file\n")
						.fg(Ansi.Color.DEFAULT).toString());
			}
			if (filePath == null) {
				long currentTimeMillis = System.currentTimeMillis();
				HashSet<EnsembleContainer> ensembleContainerList = getDetails();
				Type profileListType = new TypeToken<HashSet<EnsembleContainer>>() {
				}.getType();
				String configString = gson.toJson(ensembleContainerList);
				if (storeFile != null) {
					try {
						File file = new File(storeFile);
						BufferedWriter writer = new BufferedWriter(new FileWriter(file));
						writer.write(gson.toJson(ensembleContainerList, profileListType));
						writer.close();
						file.setReadable(true, false);
						LOG.info("Config Written to file {} ", storeFile);
					} catch (Exception e) {
						System.err.println(Ansi.ansi().fg(Color.RED).a(e.getMessage()).toString());
					}
				} else {
					out.print(configString);
				}
				long timeAfter = System.currentTimeMillis();
				LOG.info("Execution time is {}", (timeAfter - currentTimeMillis) / 1000);
				if ("true".equalsIgnoreCase(runStatusCheck))
					(new StatusCheck()).run();
			}

			else if (filePath != null && !filePath.isEmpty() && !"only".equalsIgnoreCase(synchContexts)) {
				long currentTimeMillis = System.currentTimeMillis();
				HashSet<EnsembleContainer> ensembleContainerList = getDetails();
				LOG.debug("Master Container Map {} ", ensembleContainerList);
				LOG.info("Current Environment already contains {} containers ", ensembleContainerList.size());

				try {
					getContainersToChange(profileService, filePath, ensembleContainerList, out);

				} catch (Exception e) {
					System.err.println(Ansi.ansi().fg(Color.RED).a("Error when working to synch conainers {}")
							.a(e.getMessage()).toString());
					LOG.error("Error when working to synch conainers {}", e.getMessage(), e);
				}
				LOG.info("Time to synch up containers {}", System.currentTimeMillis() - currentTimeMillis);
				LOG.info("Execution run completed ...");
				if (Boolean.valueOf(synchContexts) == true) {
					synchContexts(ensembleContainerList, filePath, out, profileService);
					LOG.info("Time to synch up all contexts {}", System.currentTimeMillis() - currentTimeMillis);
				}

			} else if ("only".equalsIgnoreCase(synchContexts)) {

				LOG.info("Synching up Contexts.....");
				if (filePath == null || jmxuser == null || jmxPassword == null) {
					System.err.println(Ansi.ansi().fg(Color.RED)
							.a("Input configuration file path , jmxuser or jxmpassword is missing").toString());
				}

				HashSet<EnsembleContainer> ensembleContainerList = getDetails();

				synchContexts(ensembleContainerList, filePath, out, profileService);
			}
		}
		// out.print( gson.toJson(containersToChange) );

		return null;
	}

	/*
	 * Checks if the container is stopped and restarts it , also checks if the
	 * container is not managed and not ensemble server and recreates it
	 */
	private void checkAndRestartContainers() {

		Container[] containers = fabricService.getContainers();
		List<Container> stoppedContainers = new ArrayList<Container>();
		for (Container container : containers) {

			if (container.getProvisionStatus().contains("error") || container.getProvisionStatus().equals("")
					|| !container.isAlive() || container.getProvisionStatus().contains("stop")) {
				stoppedContainers.add(container);
				java.util.Arrays.asList(Profile.class);
			}
			if (!container.isManaged() && !container.isEnsembleServer()) {
				Set profiles = new HashSet<Profile>(java.util.Arrays.asList(container.getProfiles()));
				CreateSshContainerOptions.Builder sshBuilder = CreateSshContainerOptions.builder()
						.name(container.getId()).ensembleServer(container.isEnsembleServer()).resolver(resolver)
						.bindAddress(bindAddress).manualIp(manualIp).number(1).host(container.getLocalHostname())
						.preferredAddress(container.getLocalHostname()).username(remoteUser).password(remotePassword)
						.proxyUri(fabricService.getMavenRepoURI()).zookeeperUrl(fabricService.getZookeeperUrl())
						.zookeeperPassword(isEnsembleServer && zookeeperPassword != null ? zookeeperPassword
								: fabricService.getZookeeperPassword())
						.jvmOpts(jvmOpts != null ? jvmOpts : fabricService.getDefaultJvmOptions())
						.version(container.getVersion().getId()).profiles(profiles)
						.dataStoreProperties(getDataStoreProperties()).uploadDistribution(false).path(path)
						.waitForProvision(false);
				fabricService.destroyContainer(container, true);
				fabricService.createContainers(sshBuilder.build());
			}
		}
		if (stoppedContainers.size() > 0) {
			ExecutorService service = Executors.newFixedThreadPool(stoppedContainers.size());
			CountDownLatch latch = new CountDownLatch(stoppedContainers.size());
			stopContainers(stoppedContainers, service, latch);
			try {
				latch.await();
				service.shutdown();
			} catch (Exception e) {

			}
			service = Executors.newFixedThreadPool(stoppedContainers.size());
			latch = new CountDownLatch(stoppedContainers.size());
			startContainers(stoppedContainers, service, latch);
			try {
				latch.await();
				service.shutdown();
			} catch (Exception e) {

			}
		}

	}

	/*
	 * Method to stop containers.
	 */
	private void stopContainers(List<Container> stoppedContainers, final ExecutorService service,
			final CountDownLatch latch) {

		for (final Container container : stoppedContainers) {
			service.submit(new Runnable() {

				@Override
				public void run() {
					try {
						if (!demoRun) {

							fabricService.stopContainer(container, true);
							LOG.info("Stopped container {}", container.getId());
						} else {
							LOG.info("Container {} will be stopped", container.getId());
						}
					} finally {
						latch.countDown();
					}

				}
			});
		}
	}

	/*
	 * Starts all the provided list of containers
	 */
	private void startContainers(List<Container> stoppedContainers, final ExecutorService service,
			final CountDownLatch latch) {
		for (final Container container : stoppedContainers) {
			service.submit(new Runnable() {

				@Override
				public void run() {
					try {
						if (!demoRun) {
							fabricService.startContainer(container, true);

							LOG.info("Started container {}", container.getId());
						} else {
							LOG.info("Started container {}", container.getId());
						}
					} finally {
						latch.countDown();
					}

				}
			});
		}
	}

	/*
	 * Synchs context by taking the source and attempting to start the contexts in
	 * the destination
	 */
	private void synchContexts(HashSet<EnsembleContainer> ensembleContainerList, String filePath, PrintStream out,
			ProfileService profileService) throws FileNotFoundException {
		List<EnsembleContainer> oldConfiguration = null;
		try {
			oldConfiguration = readConfigFile(filePath, out);
		} catch (FileNotFoundException e) {
			throw e;
		}

		List<EnsembleContainer> contextContainers = new ArrayList<EnsembleContainer>();

		for (EnsembleContainer newContainer : ensembleContainerList) {
			if (oldConfiguration.contains(newContainer)) {
				EnsembleContainer oldContainer = oldConfiguration.get(oldConfiguration.indexOf(newContainer));
				if (oldContainer.getContexts()!=null && !oldContainer.getContexts().equals(newContainer.getContexts())) {
					contextContainers.add(oldContainer);
				}
			}
		}
		if(contextContainers.size() > 0 )
			LOG.info("The containers {} have differences in contexts ", contextContainers);
		else
			LOG.info("No context differences found" );
		if (contextContainers.size() > 0 && !demoRun) {
			ExecutorService contextService = Executors.newFixedThreadPool(contextContainers.size());
			for (final EnsembleContainer ensembleContainer : contextContainers) {
				contextService.submit(new Runnable() {

					@Override
					public void run() {
						LOG.debug("reloading profiles....");
						ConcurrentHashSet<ProfileDetails> profiles = ensembleContainer.getProfiles();
						Container container = fabricService
								.getContainer(getContainerName(ensembleContainer.getContainerName()));
						List<String> profileNames = getProfileNames(profiles);
						removeProfiles(container, profileNames, true, 10);
						addProfiles(container, profileNames, true, 10);

					}
				});

			}
			shutDownExecutorService(contextService);
		}
	}

	/*
	 * Recursive method to remove profiles with 6 second retry when profile lock
	 * error occurs
	 */
	private void removeProfiles(Container container, List<String> profileNames, boolean isWaitNeeded, int count) {
		try {
			fabricService.stopContainer(container, true);
		} catch (Exception e) {
			LOG.error("Unexpected Exception while waiting for container {} to provision", container.getId());
		}
		List<String> profileIds = new ArrayList<>();
		for (Profile profile : FabricCommand.getProfiles(fabricService, container.getVersion(), profileNames)) {
			profileIds.add(profile.getId());
		}
		container.removeProfiles(profileIds.toArray(new String[profileIds.size()]));
	}

	/*
	 * Recursive method to add profiles container by waiting for 6 seconds if
	 * profile lock error happens
	 */
	private void addProfiles(Container container, List<String> profileNames, boolean isWaitNeeded, int count) {
		LOG.debug(container.isProvisioningPending() == true ? " Wait for the container to be provisioned "
				: "Adding Profiles {}", profileNames);
		try {
			Profile[] profs = FabricCommand.getExistingProfiles(fabricService, container.getVersion(), profileNames);
			container.setProfiles(profs);

			fabricService.startContainer(container, true);

		} catch (Exception e) {
			LOG.error("Unexpected Exception while adding profiles for container {} to provision", container.getId());
		}

	}

	/*
	 * Returns the profile names from the list of profiles by removing default
	 * profie
	 */
	private List<String> getProfileNames(ConcurrentHashSet<ProfileDetails> profileDetails) {

		List<String> profiles = new ArrayList<String>();
		for (ProfileDetails profileDetail : profileDetails) {
			profiles.add(profileDetail.getProfileName());
		}
		if (profiles.size() > 0)
			profiles.remove("default");
		return profiles;
	}
	/*
	 * Method enquires the container via Jolokia and gets all the running camel
	 * contexts and routes
	 */

	private HashSet<Context> getContextList(Container container) {

		Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
		HashSet<Context> contexts = new HashSet<Context>();
		StringBuilder sb = new StringBuilder();

		if (container.isAlive() && !container.isEnsembleServer() && container.isManaged()) {

			if (sb.toString().length() > 0) {
				sb.delete(0, sb.toString().length() - 1);
			}
			String jolokiaUrl = container.getJolokiaUrl();
			URL url = null;
			HttpURLConnection connection = null;
			try {
				url = new URL((new StringBuilder(jolokiaUrl).append(
						"/read/org.apache.camel:context=*,type=context,name=*/TotalRoutes,CamelId,State,StartedRoutes"))
								.toString());

				connection = (HttpURLConnection) url.openConnection();
				String auth = jmxuser + ":" + jmxPassword;
				byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
				String authHeaderValue = "Basic " + new String(encodedAuth);
				connection.setRequestProperty("Authorization", authHeaderValue);
				connection.setConnectTimeout(1000);
				connection.setRequestMethod("GET");
				connection.connect();
				if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
					BufferedReader buffer = new BufferedReader(new InputStreamReader(connection.getInputStream()));
					String line;

					while ((line = buffer.readLine()) != null) {
						sb.append(line);
					}

				}
				JsonObject fromJson = gson.fromJson(sb.toString(), JsonObject.class);
				JsonObject asJsonObject = null;
				if (fromJson.get("value") != null) {
					asJsonObject = fromJson.get("value").getAsJsonObject();

					Type profileListType = new TypeToken<HashMap<String, Context>>() {
					}.getType();

					HashMap<String, Context> fromJson2 = gson.fromJson(asJsonObject.toString(), profileListType);

					for (Map.Entry<String, Context> actualEntries : fromJson2.entrySet()) {
						contexts.add(actualEntries.getValue());
					}
				}
				connection.disconnect();
			} catch (MalformedURLException e1) {
				LOG.error("Unable to connect to container ...{}", container.getId());
			} catch (ProtocolException e1) {
				LOG.error("Unable to connect to container ...{}", container.getId());
			} catch (IOException e) {
				LOG.error("Unable to connect to container ...{}", container.getId());
			} catch (Exception e) {
				LOG.info("Skipping the response that is error for container {}", container.getId(), e.getMessage());
			}
		}
		return contexts;
	}

	/*
	 * Synchs up the containers in destination based on source information
	 */
	public void getContainersToChange(final ProfileService profileService, final String oldConfigurationFile,
			final HashSet<EnsembleContainer> ensembleContainerList, final PrintStream out)
			throws FileNotFoundException {

		List<EnsembleContainer> oldConfiguration = null;
		try {
			oldConfiguration = readConfigFile(oldConfigurationFile, out);
			LOG.info("Old Environment contains {} containers ", oldConfiguration.size());
		} catch (FileNotFoundException e) {
			throw e;
		}

		ExecutorService containerExecutorService = Executors.newFixedThreadPool(oldConfiguration.size());
		final CountDownLatch containerCountDownLatch = new CountDownLatch(oldConfiguration.size());
		final List<String> profilesToSynch = new ArrayList<String>();
		for (final EnsembleContainer oldContainer : oldConfiguration) {

			if (!oldContainer.getContainerName().equalsIgnoreCase(ignoreContainer)) {

				containerExecutorService.submit(new Runnable() {
					@Override
					public void run() {
						try {
							final String containerName = getContainerName(oldContainer.getContainerName());

							Container newContainer = null;
							try {
								newContainer = fabricService.getContainer(containerName);
							} catch (Exception e) {
								LOG.debug("Container {} does not exist ", containerName);
							}
							if (newContainer == null) {
								LOG.debug("Container {} does not exisit attempting to create one {} {} ", containerName,
										jmxuser, jmxPassword);
								CreateContainerMetadata[] createContainers = null;

								if (Boolean.valueOf(child)) {
									CreateChildContainerOptions.Builder builder = CreateChildContainerOptions.builder()
											.name(containerName)
											// TODO: what if parent also does not exist
											// Write a recursive function to get this done ???
											.parent(oldContainer.getParent()).ensembleServer(false)
											.zookeeperUrl(fabricService.getZookeeperUrl())
											.zookeeperPassword(fabricService.getZookeeperPassword())
											.jmxPassword(jmxPassword).jmxUser(jmxuser)
											.version(fabricService.getDefaultVersionId())
											.jvmOpts(fabricService.getDefaultJvmOptions()).profiles("default");
									try {
										createContainers = fabricService.createContainers(builder.build());
									} catch (Exception e) {

									}

								} else {
									String pickHost = getHost(hostsToCreateContainers, oldContainer.getContainerName());

									LOG.info("Container  {}  will be created on  {}",
											getContainerName(oldContainer.getContainerName()), pickHost);

									String actualPath = null;
									if (containerName.contains("amq")) {
										StringBuffer pathBuffer = new StringBuffer();
										pathBuffer.append(path).append("/amq/630434");
										actualPath = pathBuffer.toString();
									} else {
										StringBuffer pathBuffer = new StringBuffer();
										pathBuffer.append(path).append("/camel/630434");
										actualPath = pathBuffer.toString();
									}

									List<String> associatedProfiles = new ArrayList<String>();
									ConcurrentHashSet<ProfileDetails> oldProfieDetails = oldContainer.getProfiles();
									for (ProfileDetails oldProfile : oldProfieDetails) {
										associatedProfiles.add(oldProfile.getProfileName());
									}

									Profile[] availableProfiles = null;
									try {
										availableProfiles = getProfiles(fabricService, oldContainer.getVersion(),
												associatedProfiles, oldProfieDetails, oldContainer.getContainerName());
										associatedProfiles.clear();

										for (Profile profile : availableProfiles) {
											String id = profile.getId();
											if (!"default".equalsIgnoreCase(id))
												associatedProfiles.add(profile.getId());
										}
									} catch (Exception e) {
										LOG.warn(e.getMessage(), e);
									}

									if (!demoRun) {
										String hostAddress = InetAddress.getByName(pickHost).getHostAddress();

										LOG.debug("host is {}", pickHost);
										LOG.debug(" Address is {} ", InetAddress.getByName(pickHost).getHostAddress());

										Version requiredVersion = null;
										try {
											requiredVersion = profileService
													.getRequiredVersion(oldContainer.getVersion());
										} catch (Exception e) {
											LOG.warn("Version {}  could not be created ", oldContainer.getVersion());
										}

										if (requiredVersion != null && requiredVersion.getId()
												.equalsIgnoreCase(oldContainer.getVersion())) {
											createVersionIfDoesnotExist(oldContainer.getVersion(), profileService);
										}

										CreateSshContainerOptions.Builder sshBuilder = CreateSshContainerOptions
												.builder().name(containerName).ensembleServer(isEnsembleServer)
												.resolver(resolver).bindAddress(bindAddress).manualIp(manualIp)
												.number(1).host(pickHost).preferredAddress(hostAddress)
												.username(remoteUser).password(remotePassword)
												.proxyUri(fabricService.getMavenRepoURI())
												.zookeeperUrl(fabricService.getZookeeperUrl())
												.zookeeperPassword(isEnsembleServer && zookeeperPassword != null
														? zookeeperPassword
														: fabricService.getZookeeperPassword())
												.jvmOpts(jvmOpts != null ? jvmOpts
														: fabricService.getDefaultJvmOptions())
												.version(oldContainer.getVersion()).profiles(associatedProfiles)
												.dataStoreProperties(getDataStoreProperties()).uploadDistribution(false)
												.path(actualPath).waitForProvision(false);

										createContainers = waitAndStartContainer(sshBuilder);
									}
									Thread.sleep(1000L);
								}

							} else {

								EnsembleContainer newEnsembleontainer = null;
								LOG.debug("New Container List is {}", ensembleContainerList);

								for (EnsembleContainer newEnsemble : ensembleContainerList) {
									if (newEnsemble.getContainerName().equalsIgnoreCase(containerName)
											&& !ignoreContainer.equalsIgnoreCase(newEnsemble.getContainerName())) {

										LOG.debug("Synching up newEnsemble Container {} with {}", containerName,
												oldContainer.getContainerName());
										newEnsembleontainer = newEnsemble;
										break;
									}
								}
								LOG.debug(" Container found {}",
										newEnsembleontainer == null ? "" : newEnsembleontainer.getContainerName());
								if (newEnsembleontainer != null) {
									// public void synchProfiles(FabricService fabricService,
									// ConcurrentHashSet<ProfileDetails> oldProfileDetails,
									// ConcurrentHashSet<ProfileDetails> newProfileDetails, List<String>
									// containerProfiles) {
									LOG.debug("Synching existing container {} ",
											newEnsembleontainer.getContainerName());

									synchProfiles(fabricService, oldContainer.getProfiles(),
											newEnsembleontainer.getProfiles(), newContainer.getProfileIds(),
											newContainer, profilesToSynch);

									LOG.debug("New container {} created with profiles {}", newContainer.getId(),
											newContainer.getProfileIds());

								}
							}

						} catch (Exception e) {
							LOG.error(e.getMessage(), e);
						} finally {
							containerCountDownLatch.countDown();
						}

					}

					private CreateContainerMetadata[] waitAndStartContainer(Builder sshBuilder) {
						CreateContainerMetadata[] createContainers = null;
						try {
							createContainers = fabricService.createContainers(sshBuilder.build());
						} catch (Exception e) {
							if (e.getMessage().contains("lock")) {
								LOG.info("Profile lock occured for container creation {} {}", sshBuilder.getName(),
										e.getMessage());
								try {
									Thread.sleep(2000L);
									waitAndStartContainer(sshBuilder);

								} catch (InterruptedException e1) {
									LOG.warn("Unknown Error {}", e.getMessage());
								}

							}

						}
						return createContainers;
					}

				});
			} else {
				LOG.info("Skipping container {}", ignoreContainer);
			}
		}

		try {
			containerCountDownLatch.await();
		} catch (Exception e) {
			LOG.warn("Issue with Container creation thread {}", e.getMessage());
		}
		shutDownExecutorService(containerExecutorService);

		if (profilesToSynch.size() > 0 && !demoRun) {
			List uniqueProfileNamesObj = profilesToSynch.stream().distinct()
					.collect(java.util.stream.Collectors.toList());
			final List<ProfileDetails> uniqueProfiles = getUniqueProfiles(oldConfiguration);
			List<String> uniqueProfileNames = (ArrayList<String>) uniqueProfileNamesObj;
			LOG.info(" profilesToBeSynched {} {} {}  ", uniqueProfileNames, uniqueProfileNames.size(),
					uniqueProfiles.size());
			if (uniqueProfileNames.size() > 0) {
				containerExecutorService = Executors.newFixedThreadPool(uniqueProfileNames.size());
				for (final String profile : uniqueProfileNames) {
					containerExecutorService.submit(new Runnable() {
						@Override
						public void run() {
							for (ProfileDetails profileDetail : uniqueProfiles) {
								if (profileDetail.getProfileName().equalsIgnoreCase(profile)) {
									synchProfile(profileDetail, profile);
									break;
								}
							}

						}
					});
				}
			}
		}

		shutDownExecutorService(containerExecutorService);
	}

	/*
	 * Provides a de-duplicated list of profile details by combining profiles
	 * assigned to all containers
	 */
	private List<ProfileDetails> getUniqueProfiles(List<EnsembleContainer> oldConfiguration) {
		List<ProfileDetails> profileDetails = new ArrayList<ProfileDetails>();
		for (EnsembleContainer container : oldConfiguration) {
			profileDetails.addAll(new ArrayList<ProfileDetails>(container.getProfiles()));

		}
		List deduplicates = profileDetails.stream().distinct().collect(java.util.stream.Collectors.toList());
		return ((ArrayList<ProfileDetails>) deduplicates);
	}

	/*
	 * Calculates the containername of the destination based on the environment zone
	 * it needs to be created
	 */
	private String getContainerName(String containerName) {

		StringBuffer newContainerName = new StringBuffer();
		try {
			String[] split = containerName.split("_");

			newContainerName.append(split[0]).append("_").append(split[1]).append("_").append(environment)
					.append(containerFirstLetter.get(zoneName)).append(split[2].charAt(2)).append("_").append(split[3])
					.append("_").append(split[4]);
		} catch (Exception e) {

		}

		return newContainerName.toString();

	}

	/*
	 * Creates a version provided if it does not exist or returns the version
	 */
	private Version createVersionIfDoesnotExist(String productionProfileVersion, ProfileService profileService) {

		Version requiredVersion = null;

		try {
			requiredVersion = profileService.getRequiredVersion(productionProfileVersion);
		} catch (Exception e) {
			if (!demoRun)
				LOG.error("Required Version {} does not exist in current environment ", productionProfileVersion);
		}
		if (requiredVersion == null && !demoRun) {
			try {

				LOG.debug("Parent Version {} ", baseVersion);
				LOG.debug("Target Version {} ", productionProfileVersion);

				if (baseVersion != null) {
					Map<String, String> attributes = new HashMap<String, String>(
							Collections.singletonMap(Version.PARENT, baseVersion));
					attributes.put(Version.DESCRIPTION, "Created by ansible to replicate prod");
					requiredVersion = profileService.createVersionFrom(baseVersion, productionProfileVersion,
							attributes);
					LOG.info("Creating new version {} from source version {} ", productionProfileVersion, baseVersion);
				} else {
					VersionBuilder builder = VersionBuilder.Factory.create(productionProfileVersion);
					builder.addAttribute(Version.DESCRIPTION, "Created by ansible to replicate prod");
					requiredVersion = profileService.createVersion(builder.getVersion());
					LOG.info("Creating new version with no base");
				}
			} catch (Exception e) {
				LOG.warn("Unable to create new Version {} ERROR: ", productionProfileVersion, e.getMessage(), e);
			}
		}
		return requiredVersion;

	}

	/*
	 * Reads the source configuration file and creates the list of containers with
	 * all of its configuration details
	 */
	public List<EnsembleContainer> readConfigFile(String oldConfigurationFile, PrintStream out)
			throws FileNotFoundException {
		List<EnsembleContainer> oldConfiguration = null;
		File oldJson = null;
		try {
			oldJson = new File(oldConfigurationFile);

			Type profileListType = new TypeToken<ArrayList<EnsembleContainer>>() {
			}.getType();
			Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();

			BufferedReader bo = null;
			StringBuffer oldBuffer = new StringBuffer();

			bo = new BufferedReader(new FileReader(oldJson));
			String line = "";
			while ((line = bo.readLine()) != null) {
				oldBuffer.append(line);
			}
			bo.close();
			LOG.debug(" file Path is {} ", oldConfigurationFile);
			LOG.debug(" file content is {} ", oldBuffer.toString());

			oldConfiguration = gson.fromJson(oldBuffer.toString(), profileListType);
		} catch (FileNotFoundException e) {
			LOG.error(e.getMessage(), e);
			throw e;
		} catch (IOException e) {
			LOG.warn(e.getMessage(), e);
		}
		return oldConfiguration;
	}

	/*
	 * Derives the host name based on the container , environment and zone names
	 * provided this is where the respective container will be physically created
	 */
	public String getHost(List<String> hosts, String containerName) {

		String[] split = containerName.split("_");
		LOG.debug("container host {}", containerName);
		StringBuilder serverName = new StringBuilder();
		serverName.append(serverFirstLetter.get(zoneName)).append(environment).append("lesb")
				.append(firstNumber.get(environment)).append(secondNumber.get(split[0])).append(split[2].charAt(2));
		return serverName.toString();
	}

	/*
	 * Gets all the profiles with the <code>names</code> and creates the missing
	 * profiles using the <code>profileDetails</code>
	 */
	public Profile[] getProfiles(FabricService fabricService, String versionId, List<String> names,
			ConcurrentHashSet<ProfileDetails> profileDetails, String containerName) {

		LOG.info("Total profiles in source for container {} is  {}", containerName, profileDetails.size());

		createVersionIfDoesnotExist(versionId, profileService);

		ProfileService profileService = fabricService.adapt(ProfileService.class);

		List<Profile> allProfiles = null;
		List<Profile> profiles = new ArrayList<>();
		try {
			allProfiles = profileService.getRequiredVersion(versionId).getProfiles();

			if (names == null) {
				return new Profile[0];
			}
			for (String profileId : names) {
				Profile profile = null;
				for (Profile p : allProfiles) {
					if (profileId.equals(p.getId())) {
						profile = p;
						break;
					}
				}

				if (profile == null && !demoRun) {
					for (ProfileDetails oldProfileDetails : profileDetails) {
						if (oldProfileDetails.getProfileName().equalsIgnoreCase(profileId)) {
							buildAndCreateProfile(oldProfileDetails);
						}
					}

					LOG.info("Acquired profiles {} for container {}", profiles.size(), containerName);
				} else {
					if (profile != null && !profile.getId().equalsIgnoreCase("default")
							&& !demoRun) {
						profiles.add(profile);
					}
					LOG.info("A profile with id {} will be created and associated to container {}", profileId,
							containerName);
				}

			}
		} catch (Exception e) {
			if (!demoRun)
				LOG.error("Unknown Exception {}", e.getMessage());
		}
		return profiles.toArray(new Profile[profiles.size()]);
	}

	/*
	 * Creates the profile based on source details
	 */
	private Profile buildAndCreateProfile(ProfileDetails profileDetails) {

		Profile profile = null;
		ProfileBuilder builder = ProfileBuilder.Factory.create();
		buildProfile(builder, profileDetails);
		try {
			profile = profileService.createProfile(builder.getProfile());
		} catch (Exception e) {
			LOG.info("Unable to create profile {}", profile);
		}
		return profile;
	}

	/*
	 * Updates the profile based on source details
	 */
	private Profile buildAndUpdateProfile(ProfileDetails profileDetails, Profile profile) {
		Profile newProfile = null;
		ProfileBuilder builder = ProfileBuilder.Factory.createFrom(profile);
		try {
			newProfile = profileService.updateProfile(builder.getProfile());
		} catch (Exception e) {
			LOG.info("Unable to create profile {}", profile);
		}
		return newProfile;
	}

	/*
	 * Builds profiles using profile builder provided and source profile information
	 */
	private void buildProfile(ProfileBuilder builder, ProfileDetails profileDetails) {
		if (profileDetails.getParents() != null)
			builder.setParents(new ArrayList<String>(profileDetails.getParents()));
		if (profileDetails.getBundles() != null)
			builder.setBundles(new ArrayList<String>(profileDetails.getBundles()));
		if (profileDetails.getFeatures() != null)
			builder.setFeatures(new ArrayList<String>(profileDetails.getFeatures()));
		if (profileDetails.getConfigurations() != null) {
			builder.setConfigurations(profileDetails.getConfigurations());
		}
		builder.version(profileDetails.getProfileVersion());
	}

	/*
	 * Checks the destination container and syncs up the destination
	 * profiles<code>newProfileDetails</code> as per the
	 * source<code>oldProfileDetails</code>
	 */
	private void synchProfiles(FabricService fabricService, ConcurrentHashSet<ProfileDetails> oldProfileDetails,
			ConcurrentHashSet<ProfileDetails> newProfileDetails, List<String> containerProfiles, Container newContainer,
			List<String> profilesToSynch) {

		ConcurrentHashSet<ProfileDetails> missingProfiles = new ConcurrentHashSet<ProfileDetails>();
		List<String> missingProfileIds = new ArrayList<String>();

		LOG.debug("container {} Old profiles {} new profiles {}", newContainer.getId(), oldProfileDetails.size(),
				newProfileDetails.size());
		if (oldProfileDetails.size() > newProfileDetails.size()) {

			for (ProfileDetails oldProfile : oldProfileDetails) {
				boolean isMatch = false;
				for (ProfileDetails newProfile : newProfileDetails) {
					if (oldProfile.getProfileName().equalsIgnoreCase(newProfile.getProfileName())
							&& !ignoreProfiles.contains(oldProfile.getProfileName())) {
						isMatch = true;
						break;
					}
				}
				if (!isMatch) {
					missingProfiles.add(oldProfile);
					missingProfileIds.add(oldProfile.getProfileName());
				}
			}
		}

		if (missingProfiles != null && missingProfiles.size() > 0) {

			LOG.info("Synching Container {} as it is missing profiles {} ", newContainer.getId(), missingProfileIds);

			Iterator<ProfileDetails> iterator = oldProfileDetails.iterator();
			String version = null;
			while (iterator.hasNext()) {
				version = (iterator.next()).getProfileVersion();
			}
			Profile[] profiles2 = null;
			try {
				profiles2 = getProfiles(fabricService, version, missingProfileIds, missingProfiles,
						newContainer.getId());
			} catch (Exception e) {
				LOG.warn("Exception when getting profiles {} ", e.getMessage());
			}
			if (profiles2 != null && !demoRun) {
				newContainer.addProfiles(profiles2);
				try {
					fabricService.stopContainer(newContainer, true);
					fabricService.startContainer(newContainer, true);
				} catch (Exception e) {
					LOG.error("Error after reassigning profiles {}", e.getMessage());
				}
			} 
		}

		for (ProfileDetails oldProfileDetail : oldProfileDetails) {
			for (ProfileDetails newProfileDetail : newProfileDetails) {
				if (newProfileDetail.getProfileName().equalsIgnoreCase(oldProfileDetail.getProfileName())
						&& !ignoreProfiles.contains(oldProfileDetail.getProfileName())) {
					if (oldProfileDetail.equals(newProfileDetail)) {
						LOG.debug(" Old Profile {} and new Profile{} are same ", oldProfileDetail.getProfileName(),
								newProfileDetail.getProfileName());
					} else {
						// Not attempting to create synch the profile here
						// as there are multiple threads doing this operation
						// and a same profile can be assigned to different
						// contai
						LOG.info("Marking profile {} to be synched up from container {}", newContainer.getId());
						profilesToSynch.add(oldProfileDetail.getProfileName());
					}

				}
			}
		}
		
	}

	/*
	 * Compares the source and destination profiles and synches up the destination
	 * profile to match the source
	 */
	public void synchProfile(ProfileDetails oldProfileDetail, String profileId) {

		LOG.info("Old profile  and new profile  are not same for {}", oldProfileDetail.getProfileName());

		try {
			// ProfileService profileService = fabricService.adapt(ProfileService.class);
			Profile newProfile = profileService.getProfile(oldProfileDetail.getProfileVersion(),
					oldProfileDetail.getProfileName());
			LOG.info("Synching up profile details for {} with ", newProfile.getId());

			buildAndUpdateProfile(oldProfileDetail, newProfile);

		} catch (Exception e) {
			LOG.error("Profile {} could not be updated to {} ", oldProfileDetail.getProfileName(), e.getMessage(), e);
		}

	}

	/*
	 * Gets the details of all the containers in all versions in the ensemble
	 */
	public HashSet<EnsembleContainer> getDetails() {

		final HashSet<EnsembleContainer> ensembleContainers = new HashSet<EnsembleContainer>();

		Container[] containers = fabricService.getContainers();
		if (containers != null && containers.length > 0) {

			final Set<EnsembleContainer> ensembleSet = ConcurrentHashMap.newKeySet();
			ExecutorService containerExecutorService = Executors.newFixedThreadPool(containers.length);
			final CountDownLatch latch = new CountDownLatch(containers.length);

			for (final Container container : containers) {
				containerExecutorService.submit(new Runnable() {
					@Override
					public void run() {
						try {
							if (!container.getId().equalsIgnoreCase(ignoreContainer) && !container.isEnsembleServer()
									&& container.isManaged()) {

								EnsembleContainer ensembleContainer = new EnsembleContainer();
								ConcurrentHashSet<ProfileDetails> profileDetailList = new ConcurrentHashSet<ProfileDetails>();
								for (Profile profile : container.getProfiles()) {
									ProfileDetails profileDetail = new ProfileDetails();
									profileDetail.setProfileName(profile.getId());
									profileDetail.setBundles(profile.getBundles());
									profileDetail.setConfigurations(profile.getConfigurations());
									profileDetail.setFabs(profile.getFabs());
									profileDetail.setRepositories(profile.getRepositories());
									profileDetail.setProfileVersion(profile.getVersion());
									profileDetail.setFeatures(profile.getFeatures());
									profileDetail.setAttributes(profile.getAttributes());
									profileDetail.setLibraries(profile.getLibraries());
									profileDetailList.add(profileDetail);
								}
								ensembleContainer.setContainerName(container.getId());
								ensembleContainer.setParent(
										container.getParent() == null ? null : container.getParent().getId());
								ensembleContainer.setEnvDefaultVersion(fabricService.getDefaultVersionId());
								ensembleContainer.setProfiles(profileDetailList);
								ensembleContainer.setVersion(container.getVersionId());
								ensembleContainer.setContexts(getContextList(container));
								ensembleSet.add(ensembleContainer);
								LOG.debug("Container {} Processed", container.getId());

							}
						} finally {

							latch.countDown();
						}
					}
				});

			}
			try {
				latch.await();
			} catch (InterruptedException e) {
				LOG.warn("Unknown error {}", e.getMessage());
			}
			Iterator<EnsembleContainer> iterator = ensembleSet.iterator();
			while (iterator.hasNext())
				ensembleContainers.add(iterator.next());
			shutDownExecutorService(containerExecutorService);
		}

		return ensembleContainers;
	}

	private void shutDownExecutorService(ExecutorService executorService) {

		executorService.shutdown();
		try {
			if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
				executorService.shutdownNow();
			}
		} catch (InterruptedException e) {
			executorService.shutdownNow();
		}
	}

	public class StatusCheck extends Thread {

		public void run() {
			restartContainers(autoRecheckCount);
		}

		public void restartContainers(int count) {

			Container[] containers = fabricService.getContainers();
			final List<Container> restartList = new ArrayList<Container>();
			for (Container container : containers) {
				if (container.getProvisionStatus() != null && (container.getProvisionStatus().trim().equals("")
						|| container.getProvisionStatus().equals(Container.PROVISION_ERROR)
						|| container.getProvisionStatus().equals(Container.PROVISION_STOPPING)
						|| container.getProvisionStatus().equals(Container.PROVISION_STOPPED))) {
					restartList.add(container);
				}
			}
			if (restartList.size() > 0) {
				ExecutorService service = Executors.newFixedThreadPool(restartList.size());
				for (final Container restartContainer : restartList) {
					service.submit(new Runnable() {

						@Override
						public void run() {
							if (!restartContainer.getProvisionStatus().equalsIgnoreCase(Container.PROVISION_STOPPED)) {
								fabricService.stopContainer(restartContainer, true);
								fabricService.startContainer(restartContainer, true);
							}
						}
					});

				}
				shutDownExecutorService(service);
				try {
					Thread.sleep(statusCheckInterval);
					if (count > 1) {
						restartContainers(count--);
					}
				} catch (InterruptedException e) {
					LOG.error("Exception when waiting to check the status of the containers ", e.getMessage(), e);
				}
			} else {
				LOG.info("All the containers have been successfully configured ");
			}

		}
	}
}
