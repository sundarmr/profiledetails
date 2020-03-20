
package org.redhat.fabric.commands;

import static io.fabric8.commands.support.CommandUtils.sortProfiles;

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
import org.redhat.fabric.commands.model.Profiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import io.fabric8.api.Container;
import io.fabric8.api.Containers;
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
		List<String> versions = profileService.getVersions();
		Map<String, Profiles> masterContainerMap = null;
		PrintStream out = System.out;
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		if (checkAndRestartContainers != null) {
			checkAndRestartContainers();
		}
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
			HashSet<EnsembleContainer> ensembleContainerList = getContainerMap(profileService, versions);
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
					out.print(e.getMessage());
				}
			} else {
				out.print(configString);
			}
			long timeAfter = System.currentTimeMillis();
			LOG.info("Execution time is {}", (timeAfter - currentTimeMillis) / 1000);
		}

		else if (filePath != null && !filePath.isEmpty() && !"only".equalsIgnoreCase(synchContexts)) {
			long currentTimeMillis = System.currentTimeMillis();
			HashSet<EnsembleContainer> ensembleContainerList = getContainerMap(profileService, versions);
			LOG.debug("Master Container Map {} ", ensembleContainerList);
			LOG.info("Current Environment already contains {} containers ", ensembleContainerList.size());

			try {
				getContainersToChange(profileService, filePath, ensembleContainerList, out);

			} catch (Exception e) {
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
				out.print("Input configuration file path , jmxuser or jxmpassword is missing");
				System.exit(0);
			}

			HashSet<EnsembleContainer> ensembleContainerList = getContainerMap(profileService, versions);

			synchContexts(ensembleContainerList, filePath, out, profileService);
		}

		// out.print( gson.toJson(containersToChange) );

		return null;
	}

	private void checkAndRestartContainers() {

		Container[] containers = fabricService.getContainers();
		List<Container> stoppedContainers = new ArrayList<Container>();
		for (Container container : containers) {
			LOG.info("{}", container.getProfiles());
			if (container.getProvisionStatus().contains("error") || container.getProvisionResult().equals("false")) {
				stoppedContainers.add(container);
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

	private void stopContainers(List<Container> stoppedContainers, final ExecutorService service,
			final CountDownLatch latch) {

		for (final Container container : stoppedContainers) {
			service.submit(new Runnable() {

				@Override
				public void run() {
					try {
						fabricService.stopContainer(container, true);
					} finally {
						latch.countDown();
					}

				}
			});
		}
	}

	private void startContainers(List<Container> stoppedContainers, final ExecutorService service,
			final CountDownLatch latch) {
		for (final Container container : stoppedContainers) {
			service.submit(new Runnable() {

				@Override
				public void run() {
					try {
						fabricService.startContainer(container, true);
					} finally {
						latch.countDown();
					}

				}
			});
		}
	}

	private void synchContexts(HashSet<EnsembleContainer> ensembleContainerList, String filePath, PrintStream out,
			ProfileService profileService) throws FileNotFoundException {
		List<EnsembleContainer> oldConfiguration = null;
		try {
			oldConfiguration = readConfigFile(filePath, out);
		} catch (FileNotFoundException e) {
			throw e;
		}
		for (EnsembleContainer newContainer : ensembleContainerList) {
			if (oldConfiguration.contains(newContainer)) {
				EnsembleContainer oldContainer = oldConfiguration.get(oldConfiguration.indexOf(newContainer));
				if (!oldContainer.getContexts().equals(newContainer.getContexts())) {
					LOG.info("reloading profiles....");
					ConcurrentHashSet<ProfileDetails> profiles = oldContainer.getProfiles();
					Container container = fabricService.getContainer(getContainerName(oldContainer.getContainerName()));
					List<String> profileNames = getProfileNames(profiles);
					removeProfiles(container, profileNames, true);
					addProfiles(container, profileNames, true);

				}
			}
		}
	}

	private void removeProfiles(Container container, List<String> profileNames, boolean isWaitNeeded) {
		try {
			if (container.isProvisioningPending() && isWaitNeeded) {
				LOG.info("Container is provisioning waiting before retrying to remove profile");
				Thread.sleep(6000l);
				removeProfiles(container, profileNames, isWaitNeeded);
			}
		} catch (InterruptedException e) {
			LOG.error("Unexpected Exception while waiting for container {} to provision", container.getId());
		}
		List<String> profileIds = new ArrayList<>();
		for (Profile profile : FabricCommand.getProfiles(fabricService, container.getVersion(), profileNames)) {
			profileIds.add(profile.getId());
		}
		container.removeProfiles(profileIds.toArray(new String[profileIds.size()]));
	}

	private void addProfiles(Container container, List<String> profileNames, boolean isWaitNeeded) {
		LOG.debug(container.isProvisioningPending() == true ? " Wait for the container to be provisioned "
				: "Adding Profiles {}", profileNames);
		try {
			if (container.isProvisioningPending() && isWaitNeeded) {
				LOG.info("Container is provisioning waiting before retrying to add profile");
				Thread.sleep(6000l);
				addProfiles(container, profileNames, isWaitNeeded);
			}
			// Let the container recover from the profile removal and
			// fabric service get to know that it has happened.
			Thread.sleep(6000l);
			Profile[] profs = FabricCommand.getExistingProfiles(fabricService, container.getVersion(), profileNames);
			container.setProfiles(profs);
		} catch (InterruptedException e) {
			LOG.error("Unexpected Exception while waiting for container {} to provision", container.getId());
		}

	}

	private HashSet<EnsembleContainer> getContainerMap(final ProfileService profileService, List<String> versions) {

		final HashSet<EnsembleContainer> ensembleContainers = new HashSet<EnsembleContainer>();

		ExecutorService versionExecutor = Executors.newFixedThreadPool(versions.size());
		final CountDownLatch latch = new CountDownLatch(versions.size());

		for (final String versionId : versions) {

			versionExecutor.submit(new Runnable() {
				@Override
				public void run() {
					try {
						Version requiredVersion = profileService.getRequiredVersion(versionId);
						List<Profile> availableProfiles = requiredVersion.getProfiles();
						LOG.info("Started to process version{}", versionId);
						List<Profile> sortProfiles2 = sortProfiles(availableProfiles);

						printProfiles(profileService, sortProfiles2, System.out, versionId, ensembleContainers);

					} catch (Exception e) {
						LOG.error("Could not process {} {}", versionId, e.getMessage(), e);
					} finally {
						LOG.info("Completed Processing version {} {}", versionId, latch.getCount());
						latch.countDown();
					}

				}
			});
		}
		try {
			LOG.info("Waiting for the latch");
			latch.await();
			versionExecutor.shutdown();
		} catch (InterruptedException e) {
			LOG.error("Thread intterupted", e);
		}
		return ensembleContainers;
	}

	private List<String> getProfileNames(ConcurrentHashSet<ProfileDetails> profileDetails) {

		List<String> profiles = new ArrayList<String>();
		for (ProfileDetails profileDetail : profileDetails) {
			profiles.add(profileDetail.getProfileName());
		}

		profiles.remove("default");

		return profiles;
	}

	private HashSet<Context> getContextList(PrintStream out, Container container) {

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
				LOG.info("Skipping the response that is error", e.getMessage());
			}
		}
		return contexts;
	}

	private void printProfiles(final ProfileService profileService, final List<Profile> profiles, final PrintStream out,
			final String versionId, final HashSet<EnsembleContainer> ensembleContainers) {
		try {

			final Map<String, Profiles> containerMap = new HashMap<String, Profiles>();

			ExecutorService profileExecutor = Executors.newFixedThreadPool(10);
			LOG.info("version {} profile size {}", versionId, profiles.size());

			final CountDownLatch profileLatch = new CountDownLatch(profiles.size());

			final HashSet<String> processedContainers = new HashSet<String>();
			final Set<EnsembleContainer> newKeySet = ConcurrentHashMap.newKeySet();

			for (final Profile profile : profiles) {
				profileExecutor.execute(new Runnable() {
					@Override
					public void run() {
						try {
							String profileId = profile.getId();
							// skip profiles that do not exists (they may have been deleted)

							Container[] associatedContainers = null;
							try {
								if (!ignoreProfiles.contains(profileId)) {
									associatedContainers = fabricService.getAssociatedContainers(versionId, profileId);
								}
							} catch (Exception e) {
								LOG.error("Associated containers for profile {} in version{} could not be obainted {}",
										profileId, versionId, e.getMessage());
							}

							LOG.debug("Associated Containers {} {}", associatedContainers, profileId);
							if (associatedContainers != null && associatedContainers.length > 0) {
								for (Container associatedContainer : associatedContainers) {

									if (!associatedContainer.isEnsembleServer() && associatedContainer.isManaged()) {
										if (containerMap.get(associatedContainer.getId()) != null) {
											Profiles containerProfiles = containerMap.get(associatedContainer.getId());
											ConcurrentHashSet<ProfileDetails> list = containerProfiles
													.getProfileDetails();
											ProfileDetails details = new ProfileDetails();

											details.setBundles(profile.getBundles());
											details.setFeatures(profile.getFeatures());
											details.setParents(profile.getParentIds());
											details.setRepositories(profile.getRepositories());
											details.setProfileName(profileId);
											details.setProfileVersion(versionId);
											details.setConfigurations(profile.getConfigurations());
											list.add(details);
										} else {
											ConcurrentHashSet<ProfileDetails> list = new ConcurrentHashSet<ProfileDetails>();
											ProfileDetails details = new ProfileDetails();

											details.setBundles(profile.getBundles());
											details.setFeatures(profile.getFeatures());
											details.setParents(profile.getParentIds());
											details.setRepositories(profile.getRepositories());
											details.setProfileName(profileId);
											details.setProfileVersion(versionId);
											details.setConfigurations(profile.getConfigurations());

											list.add(details);
											Profiles containerCap = new Profiles();
											containerCap.setProfileDetails(list);
											containerMap.put(associatedContainer.getId(), containerCap);
										}
									}
									processedContainers.add(associatedContainer.getId());
									EnsembleContainer container = new EnsembleContainer();
									if (containerMap.get(associatedContainer.getId()) != null) {
										container.setContainerName(associatedContainer.getId());
										container.setProfiles(
												containerMap.get(associatedContainer.getId()).getProfileDetails());
										container.setParent(associatedContainer.getParent() != null
												? associatedContainer.getParent().getId()
												: null);
										container.setVersion(associatedContainer.getVersionId());
										if (!processedContainers.contains(container.getContainerName())) {
											container.setContexts(getContextList(out, associatedContainer));
										}
										container.setEnvDefaultVersion(fabricService.getDefaultVersionId());

										newKeySet.add(container);
									}
								}
							}

						} finally {
							LOG.debug("Completed processing for profile {} {}", profile, profileLatch);
							profileLatch.countDown();
						}

					}
				});
			}
			try {
				profileLatch.await();
				profileExecutor.shutdown();
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}

			Iterator<EnsembleContainer> iterator = newKeySet.iterator();
			while (iterator.hasNext())
				ensembleContainers.add(iterator.next());
		} catch (Exception e) {
			LOG.error("Issue occured {}", e.getMessage(), e);
		}
	}

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

		for (EnsembleContainer container : oldConfiguration) {
			LOG.info("{}", container.getContainerName());
		}

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

									LOG.debug("Container  {}  will be created on  {}",
											getContainerName(oldContainer.getContainerName()), pickHost);
									LOG.info("{}:{}", pickHost, getContainerName(oldContainer.getContainerName()));
									String hostAddress = InetAddress.getByName(pickHost).getHostAddress();

									LOG.debug("host is {}", pickHost);
									LOG.debug(" Address is {} ", InetAddress.getByName(pickHost).getHostAddress());

									if (!profileService.getVersions().contains(oldContainer.getVersion())) {
										createVersionIfDoesnotExist(oldContainer.getVersion(), profileService);
									}
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
									LOG.debug("path is {}", actualPath);
									CreateSshContainerOptions.Builder sshBuilder = CreateSshContainerOptions.builder()
											.name(containerName).ensembleServer(isEnsembleServer).resolver(resolver)
											.bindAddress(bindAddress).manualIp(manualIp).number(1).host(pickHost)
											.preferredAddress(hostAddress).username(remoteUser).password(remotePassword)
											.proxyUri(fabricService.getMavenRepoURI())
											.zookeeperUrl(fabricService.getZookeeperUrl())
											.zookeeperPassword(
													isEnsembleServer && zookeeperPassword != null ? zookeeperPassword
															: fabricService.getZookeeperPassword())
											.jvmOpts(jvmOpts != null ? jvmOpts : fabricService.getDefaultJvmOptions())
											.version(oldContainer.getVersion())

											.dataStoreProperties(getDataStoreProperties()).uploadDistribution(false)
											.path(actualPath).waitForProvision(false);
									createContainers = fabricService.createContainers(sshBuilder.build());

									Thread.sleep(1000L);
								}

								if (checkContainers(createContainers)) {
									Container container = fabricService.getContainer(containerName);
									stopContainer(container);
									List<String> associatedProfiles = new ArrayList<String>();
									ConcurrentHashSet<ProfileDetails> oldProfieDetails = oldContainer.getProfiles();
									for (ProfileDetails oldProfile : oldProfieDetails) {
										associatedProfiles.add(oldProfile.getProfileName());
									}
									Profile[] availableProfiles = null;
									try {
										availableProfiles = getProfiles(fabricService, oldContainer.getVersion(),
												associatedProfiles, oldProfieDetails, oldContainer.getContainerName());
									} catch (Exception e) {
										LOG.warn(e.getMessage(), e);
									}

									container.addProfiles(availableProfiles);
									container.removeProfiles("default");
									LOG.info("Profiles {} have been added to container {} ", availableProfiles,
											container.getId());

									/*
									 * try { fabricService.startContainer(container, true); } catch (Exception e) {
									 * LOG.warn("Unable to start container {} ", container.getId()); }
									 */
								}

							} else {

								EnsembleContainer newEnsembleontainer = null;
								LOG.debug("New Container List is {}", ensembleContainerList);

								for (EnsembleContainer newEnsemble : ensembleContainerList) {
									if (newEnsemble.getContainerName().equalsIgnoreCase(containerName)
											&& !ignoreContainer.equalsIgnoreCase(newEnsemble.getContainerName())) {

										LOG.info("Synching up newEnsemble Container {} with {}", containerName,
												oldContainer.getContainerName());
										newEnsembleontainer = newEnsemble;
										break;
									}
								}
								LOG.info(" Container found {}",
										newEnsembleontainer == null ? "" : newEnsembleontainer.getContainerName());
								if (newEnsembleontainer != null) {
									// public void synchProfiles(FabricService fabricService,
									// ConcurrentHashSet<ProfileDetails> oldProfileDetails,
									// ConcurrentHashSet<ProfileDetails> newProfileDetails, List<String>
									// containerProfiles) {
									LOG.info("Synching existing container {} ", newEnsembleontainer.getContainerName());
									synchProfiles(fabricService, oldContainer.getProfiles(),
											newEnsembleontainer.getProfiles(), newContainer.getProfileIds(),
											newContainer);
									LOG.info("New container {} created with profiles {}", newContainer.getId(),
											newContainer.getProfileIds());
								}
							}

						} catch (Exception e) {
							LOG.error(e.getMessage(), e);
						} finally {
							containerCountDownLatch.countDown();
						}

					}

					private void stopContainer(Container container) {
						if (container.isProvisioningPending()) {

							try {
								Thread.sleep(1000);
							} catch (Exception e) {

							}
						} else if (container.isProvisioningComplete()) {
							fabricService.stopContainer(container);
						}
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
		restartContainers(oldConfiguration);
	}

	private boolean isProfileSame(ProfileDetails profileDetail, Profile profile) {

		if (profileDetail.getBundles() == null && profile.getBundles() != null)
			return false;
		if (profileDetail.getBundles() != null && profileDetail.getBundles().equals(profile.getBundles()))
			return true;
		if (profileDetail.getFabs() == null && profile.getFabs() != null)
			return false;
		if (profileDetail.getFabs() != null && profileDetail.getFabs().equals(profile.getFabs()))
			return true;
		if (profileDetail.getRepositories() == null && profile.getRepositories() != null)
			return false;
		if (profileDetail.getRepositories() != null
				&& profileDetail.getRepositories().equals(profile.getRepositories()))
			return true;
		if (profileDetail.getFeatures() == null && profile.getFeatures() != null)
			return false;
		if (profileDetail.getFeatures() != null && profileDetail.getFeatures().equals(profile.getFeatures()))
			return true;
		if (profileDetail.getParents() == null && profile.getParentIds() != null)
			return false;
		if (profileDetail.getParents() != null && profileDetail.getParents().equals(profile.getParentIds()))
			return true;

		if (profileDetail.getConfigurations() == null && profile.getConfigurations() != null)
			return false;
		if (profileDetail.getConfigurations() != null
				&& profileDetail.getConfigurations().equals(profile.getConfigurations()))
			return true;

		if (profileDetail.getProfileVersion() == null && profile.getVersion() != null)
			return false;

		if (profileDetail.getProfileVersion() != null && profileDetail.getProfileVersion().equals(profile.getVersion()))
			return true;

		return false;
	}

	private void restartContainers(List<EnsembleContainer> oldConfiguration) {

		List<String> amqContainers = new ArrayList<String>();
		List<String> otherContainers = new ArrayList<String>();

		for (EnsembleContainer container : oldConfiguration) {
			String containerName = getContainerName(container.getContainerName());
			if (containerName.contains("amq")) {
				amqContainers.add(containerName);
			} else {
				otherContainers.add(containerName);
			}
		}

		ExecutorService amqExecutorService = null;
		CountDownLatch amqcountDownLatch = null;
		ExecutorService otherContainerService = null;
		CountDownLatch otherContainerLatch = null;

		if (amqContainers.size() > 0) {
			amqExecutorService = Executors.newFixedThreadPool(amqContainers.size());
			amqcountDownLatch = new CountDownLatch(amqContainers.size());
			startContainers(amqExecutorService, amqcountDownLatch, amqContainers);

			try {
				amqcountDownLatch.await();
			} catch (Exception e) {
				LOG.error("Error when waiting for amq containers to start {}", e.getMessage());
			}
			amqExecutorService.shutdown();
		}
		if (otherContainers.size() > 0) {
			otherContainerService = Executors.newFixedThreadPool(otherContainers.size());
			otherContainerLatch = new CountDownLatch(otherContainers.size());
			startContainers(otherContainerService, otherContainerLatch, otherContainers);
			try {
				otherContainerLatch.await();
			} catch (Exception e) {
				LOG.error("Error when waiting for other containers to start {}", e.getMessage());
			}
			otherContainerService.shutdown();
		}

	}

	public void stopContainers(final ExecutorService service, final CountDownLatch countDownLatch,
			final List<String> containerNames) {
		for (final String containerName : containerNames) {
			service.submit(new Runnable() {

				@Override
				public void run() {
					try {
						Container container = fabricService.getContainer(containerName);
						if (container != null) {
							LOG.info("Container {} provision status is {}", container.getId(),
									container.getProvisionStatus());
						}

						if (container != null && (container.getProvisionStatus().contains("error")
								|| (container.getProvisionStatus() == null
										|| container.getProvisionStatus().equals("")))) {
							fabricService.stopContainer(containerName, true);
						}
					} catch (Exception e) {
						LOG.warn("Error when trying to restart the container {}", e.getMessage());
					} finally {
						countDownLatch.countDown();
					}

				}
			});
		}

	}

	public void startContainers(final ExecutorService service, final CountDownLatch countDownLatch,
			final List<String> containerNames) {

		for (final String containerName : containerNames) {
			service.submit(new Runnable() {

				@Override
				public void run() {
					try {
						Container container = fabricService.getContainer(containerName);
						if (container != null && !container.isAliveAndOK()) {
							fabricService.startContainer(containerName, true);
						}
					} finally {
						countDownLatch.countDown();
					}

				}
			});
		}

	}

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
			LOG.error("Required Version {} does not exist in current environment ", productionProfileVersion);
		}
		if (requiredVersion == null) {
			try {
				String sourceId = profileService.getVersions().get(profileService.getVersions().size() - 1);
				LOG.debug("Parent Version {} ", sourceId);
				LOG.debug("Target Version {} ", productionProfileVersion);
				if (sourceId != null) {
					Map<String, String> attributes = new HashMap<String, String>(
							Collections.singletonMap(Version.PARENT, sourceId));
					attributes.put(Version.DESCRIPTION, "Created by ansible to replicate prod");
					requiredVersion = profileService.createVersionFrom(sourceId, productionProfileVersion, attributes);
					LOG.info("Creating new version {} from source version {} ", productionProfileVersion, sourceId);
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
	 * to check if the container has been created without any issues
	 */
	protected boolean checkContainers(CreateContainerMetadata[] metadatas) {
		boolean isSuccess = false;
		List<CreateContainerMetadata> success = new ArrayList<CreateContainerMetadata>();
		List<CreateContainerMetadata> failures = new ArrayList<CreateContainerMetadata>();
		for (CreateContainerMetadata metadata : metadatas) {
			(metadata.isSuccess() ? success : failures).add(metadata);
		}
		if (success.size() > 0) {
			isSuccess = true;
		}
		LOG.debug("Is success {} ", isSuccess);

		return isSuccess;
	}

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

	// Need to derive logic to associate hostname and containers
	public String getHost(List<String> hosts, String containerName) {

		String[] split = containerName.split("_");
		LOG.debug("container host {}", containerName);
		StringBuilder serverName = new StringBuilder();
		serverName.append(serverFirstLetter.get(zoneName)).append(environment).append("lesb")
				.append(firstNumber.get(environment)).append(secondNumber.get(split[0])).append(split[2].charAt(2));

		// LOG.info("Server is {}" ,serverName);

		return serverName.toString();
	}

	public Profile[] getProfiles(FabricService fabricService, String versionId, List<String> names,
			ConcurrentHashSet<ProfileDetails> profileDetails, String containerName) {

		LOG.info("Total profiles in source for container {} is  {}", containerName, profileDetails.size());

		ProfileService profileService = fabricService.adapt(ProfileService.class);
		List<Profile> allProfiles = profileService.getRequiredVersion(versionId).getProfiles();
		List<Profile> profiles = new ArrayList<>();
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
			if (profile == null) {
				for (ProfileDetails oldProfileDetails : profileDetails) {
					if (oldProfileDetails.getProfileName().equalsIgnoreCase(profileId)) {
						ProfileBuilder builder = ProfileBuilder.Factory.create(versionId, profileId);
						builder.setParents(new ArrayList<String>(oldProfileDetails.getParents()));
						builder.setBundles(new ArrayList<String>(oldProfileDetails.getBundles()));
						builder.setFeatures(new ArrayList<String>(oldProfileDetails.getFeatures()));
						if (oldProfileDetails.getConfigurations() != null) {
							builder.setConfigurations(oldProfileDetails.getConfigurations());
						}
						builder.version(oldProfileDetails.getProfileVersion());
						try {
							profile = profileService.createProfile(builder.getProfile());
						} catch (Exception e) {
							LOG.info("Unable to create profile {}", profile);
						}
					}
				}
			}
			if (profile != null && !profile.getId().equalsIgnoreCase("default"))
				profiles.add(profile);
		}

		LOG.info("Acquired profiles {} for container {}", profiles.size(), containerName);

		return profiles.toArray(new Profile[profiles.size()]);
	}

	public void synchProfiles(FabricService fabricService, ConcurrentHashSet<ProfileDetails> oldProfileDetails,
			ConcurrentHashSet<ProfileDetails> newProfileDetails, List<String> containerProfiles,
			Container newContainer) {
		ConcurrentHashSet<ProfileDetails> missingProfiles = new ConcurrentHashSet<ProfileDetails>();
		List<String> missingProfileIds = new ArrayList<String>();

		LOG.info("container {} Old profiles {} new profiles {}", newContainer.getId(), oldProfileDetails.size(),
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
		LOG.info("Container {} is missing profiles {}", newContainer.getId(), missingProfileIds);
		if (missingProfiles != null && missingProfiles.size() > 0) {
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
			if (profiles2 != null)
				newContainer.addProfiles(profiles2);
			else {
				LOG.error("Container {} count not be assigned any new profiles ", newContainer.getId());
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
						LOG.info("Old profile {} and new profile {} are not same ", oldProfileDetail, newProfileDetail);

						ProfileService profileService = fabricService.adapt(ProfileService.class);
						try {
							Profile newProfile = profileService.getProfile(oldProfileDetail.getProfileVersion(),
									oldProfileDetail.getProfileName());
							ProfileBuilder builder = null;
							if (newProfile == null) {

								builder = ProfileBuilder.Factory.create(oldProfileDetail.getProfileVersion(),
										oldProfileDetail.getProfileName());
								builder.setParents(new ArrayList<String>(oldProfileDetail.getParents()));
								builder.setBundles(new ArrayList<String>(oldProfileDetail.getBundles()));
								builder.setFeatures(new ArrayList<String>(oldProfileDetail.getFeatures()));
								if (oldProfileDetail.getConfigurations() != null) {
									builder.setConfigurations(oldProfileDetail.getConfigurations());
								}
								builder.version(oldProfileDetail.getProfileVersion());
								try {
									profileService.createProfile(builder.getProfile());
								} catch (Exception e) {
									LOG.warn("Profile could not be created {}", oldProfileDetail.getProfileName());
								}
							} else {
								builder = ProfileBuilder.Factory.createFrom(newProfile);
								builder.setParents(new ArrayList<String>(oldProfileDetail.getParents()));
								builder.setBundles(new ArrayList<String>(oldProfileDetail.getBundles()));
								builder.setFeatures(new ArrayList<String>(oldProfileDetail.getFeatures()));
								if (oldProfileDetail.getConfigurations() != null) {
									builder.setConfigurations(oldProfileDetail.getConfigurations());
								}
								builder.version(oldProfileDetail.getProfileVersion());
								try {
									profileService.updateProfile(builder.getProfile());
								} catch (Exception e) {

									LOG.warn("Profile could not be updated {} ", newProfile.getId());
								}
							}
						} catch (Exception e) {
							LOG.error("Profile {} could not be assigned to {} {} {}", oldProfileDetail.getProfileName(),
									newContainer.getId(), newContainer.getProfileIds(), e.getMessage());
						}
					}

				}
			}
		}

	}

}
