package org.redhat.fabric.commands;

import static io.fabric8.commands.support.CommandUtils.sortProfiles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
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

	@Option(name = "--oldIdentifier", description = "environment identifier for source")
	private String oldIdentifier;

	@Option(name = "--newIdentifier", description = "environment identifier for target")
	private String newIdentifier;

	@Option(name = "--private-key", description = "The path to the private key on the filesystem. Default is ~/.ssh/id_rsa on *NIX platforms or C:\\Documents and Settings\\<UserName>\\.ssh\\id_rsa on Windows.")
	private String privateKeyFile;
	@Option(name = "--pass-phrase", description = "The pass phrase of the key. This is for use with private keys that require a pass phrase.")
	private String passPhrase;

	static final List<String> ignoreProfiles = new ArrayList<String>() {
		{
			add("default");
			add("fabric");
			add("unmanaged");
			add("karaf");
			add("openshift");
			add("acls");
			add("autoscale");
			add("hawtio");
			add("support-base");
			add("jboss-fuse-minimal");
			add("jboss-fuse-full");
			
		}
	};

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

		if (child != null && filePath != null) {
			if (hostsToCreateContainers == null || hostsToCreateContainers.size() == 0) {
				System.err.println(Ansi.ansi().fg(Color.RED).a("Error Executing Command: ").a(
						"hostsname list or hostname is needed use option --host multiple times if more than one remote machine is used\n")
						.fg(Ansi.Color.DEFAULT).toString());
			}
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

			masterContainerMap = getContainerMap(profileService, versions);

			List<EnsembleContainer> ensembleContainerList = new ArrayList<EnsembleContainer>();

			for (Map.Entry<String, Profiles> containerMap : masterContainerMap.entrySet()) {
				EnsembleContainer ensembleContainer = new EnsembleContainer();
				ensembleContainer.setContainerName(containerMap.getKey());
				ensembleContainer.setContexts(getContextList(out, fabricService.getContainer(containerMap.getKey())));
				ensembleContainer.setProfiles(containerMap.getValue().getProfileDetails());
				ensembleContainerList.add(ensembleContainer);
			}

			out.print(gson.toJson(ensembleContainerList));
		}

		else if (filePath != null && !filePath.isEmpty() && !"only".equalsIgnoreCase(synchContexts)) {

			masterContainerMap = getContainerMap(profileService, versions);
			LOG.info("Master Container Map {} ",masterContainerMap);
			List<EnsembleContainer> ensembleContainerList = new ArrayList<EnsembleContainer>();

			for (Map.Entry<String, Profiles> containerMap : masterContainerMap.entrySet()) {
				EnsembleContainer ensembleContainer = new EnsembleContainer();
				ensembleContainer.setContainerName(containerMap.getKey());
				ensembleContainer.setContexts(getContextList(out, fabricService.getContainer(containerMap.getKey())));
				ensembleContainer.setProfiles(containerMap.getValue().getProfileDetails());
				ensembleContainerList.add(ensembleContainer);
			}
			LOG.info("Master Container Map {} ",ensembleContainerList);
			try {
				getContainersToChange(profileService, filePath, ensembleContainerList, out);
			} catch (Exception e) {
				out.print("File does not exist at path {}" + filePath);
				System.exit(0);
			}

			if (Boolean.valueOf(synchContexts) == true) {
				synchContexts(ensembleContainerList, filePath, out, profileService);
			}

		} else if ("only".equalsIgnoreCase(synchContexts)) {
			LOG.info("Synching up Contexts.....");
			if (filePath == null || jmxuser == null || jmxPassword == null) {
				out.print("Input configuration file path , jmxuser or jxmpassword is missing");
				System.exit(0);
			}

			masterContainerMap = getContainerMap(profileService, versions);

			List<EnsembleContainer> ensembleContainerList = new ArrayList<EnsembleContainer>();

			for (Map.Entry<String, Profiles> containerMap : masterContainerMap.entrySet()) {
				EnsembleContainer ensembleContainer = new EnsembleContainer();
				ensembleContainer.setContainerName(containerMap.getKey());
				ensembleContainer.setContexts(getContextList(out, fabricService.getContainer(containerMap.getKey())));
				ensembleContainer.setProfiles(containerMap.getValue().getProfileDetails());
				ensembleContainerList.add(ensembleContainer);
			}
			synchContexts(ensembleContainerList, filePath, out, profileService);
		}

		// out.print( gson.toJson(containersToChange) );

		return null;
	}

	private void synchContexts(List<EnsembleContainer> ensembleContainerList, String filePath, PrintStream out,
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
					List<ProfileDetails> profiles = oldContainer.getProfiles();
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

	private Map<String, Profiles> getContainerMap(ProfileService profileService, List<String> versions) {

		Map<String, Profiles> masterContainerMap = new HashMap<String, Profiles>();
		LOG.info("Versions {} ",versions);
		for (String versionId : versions) {
			
			Version requiredVersion = profileService.getRequiredVersion(versionId);
			List<Profile> availableProfiles = requiredVersion.getProfiles();
			
			Map<String, Profiles> containerMap = printProfiles(profileService, sortProfiles(availableProfiles), System.out, versionId);
			
			for (Entry<String, Profiles> entrySet : containerMap.entrySet()) {
				if (masterContainerMap.containsKey(entrySet.getKey())) {
					Profiles associatedProfiles = masterContainerMap.get(entrySet.getKey());
					associatedProfiles.getProfileDetails()
							.addAll(containerMap.get(entrySet.getKey()).getProfileDetails());

				} else {
					masterContainerMap.put(entrySet.getKey(), entrySet.getValue());
				}
			}
			LOG.info("Master Container Map {}",masterContainerMap);
		}
		return masterContainerMap;
	}

	private List<String> getProfileNames(List<ProfileDetails> profileDetails) {

		List<String> profiles = new ArrayList<String>();
		for (ProfileDetails profileDetail : profileDetails) {
			profiles.add(profileDetail.getProfileName());
		}

		profiles.remove("default");

		return profiles;
	}

	private List<Context> getContextList(PrintStream out, Container container) {

		Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
		List<Context> contexts = new ArrayList<Context>();
		StringBuilder sb = new StringBuilder();

		if (container.isAlive() && !container.isEnsembleServer() && container.isManaged()) {

			if (sb.toString().length() > 0) {
				sb.delete(0, sb.toString().length() - 1);
			}
			String jolokiaUrl = container.getJolokiaUrl();
			jolokiaUrl = jolokiaUrl.replace("MacBook-Pro", "localhost");

			URL url = null;
			HttpURLConnection connection = null;
			try {
				url = new URL((new StringBuilder(jolokiaUrl).append(
						"/read/org.apache.camel:context=*,type=context,name=*/TotalRoutes,CamelId,State,StartedRoutes"))
								.toString());
				LOG.info("Invoking url : {} ", url);
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
				JsonObject asJsonObject = fromJson.get("value").getAsJsonObject();

				Type profileListType = new TypeToken<HashMap<String, Context>>() {
				}.getType();

				HashMap<String, Context> fromJson2 = gson.fromJson(asJsonObject.toString(), profileListType);

				for (Map.Entry<String, Context> actualEntries : fromJson2.entrySet()) {
					contexts.add(actualEntries.getValue());
				}
			} catch (MalformedURLException e1) {
				LOG.error("Unable to connect to container ...{}", container.getId());
			} catch (ProtocolException e1) {
				LOG.error("Unable to connect to container ...{}", container.getId());
			} catch (IOException e) {
				LOG.error("Unable to connect to container ...{}", container.getId());
			}

			catch (Exception e) {
				LOG.info("Skipping the response that is error", e.getMessage());
			}
		}
		return contexts;
	}

	protected Map<String, Profiles> printProfiles(ProfileService profileService, List<Profile> profiles,
			PrintStream out, String versionId) {

		Map<String, Profiles> containerMap = new HashMap<String, Profiles>();

		for (Profile profile : profiles) {

			String profileId = profile.getId();

			// skip profiles that do not exists (they may have been deleted)
			if (profileService.hasProfile(versionId, profileId)) {
				Container[] associatedContainers = fabricService.getAssociatedContainers(versionId, profileId);
				
				String containers = "";
				List<String> bundles = null;
				List<String> features = null;
				List<String> parents = null;
				List<String> repositories = null;

				for (Container associatedContainer : associatedContainers) {
					if ( !associatedContainer.isEnsembleServer()
							&& associatedContainer.isManaged()) {
						containers = containers + associatedContainer.getId() + ",";
						if (containerMap.get(associatedContainer.getId()) != null) {
							Profiles containerProfiles = containerMap.get(associatedContainer.getId());
							List<ProfileDetails> list = containerProfiles.getProfileDetails();
							ProfileDetails details = new ProfileDetails();
							bundles = profile.getBundles();
							features = profile.getFeatures();
							repositories = profile.getRepositories();
							parents = profile.getParentIds();
							Collections.sort(bundles);
							Collections.sort(features);
							Collections.sort(repositories);
							details.setBundles(bundles);
							details.setFeatures(features);
							details.setParents(parents);
							details.setRepositories(repositories);
							details.setProfileName(profileId);
							details.setProfileVersion(versionId);
							list.add(details);
						} else {
							List<ProfileDetails> list = new ArrayList<ProfileDetails>();
							ProfileDetails details = new ProfileDetails();

							bundles = profile.getBundles();
							features = profile.getFeatures();
							repositories = profile.getRepositories();
							parents = profile.getParentIds();
							Collections.sort(bundles);
							Collections.sort(features);
							Collections.sort(repositories);
							details.setBundles(bundles);
							details.setFeatures(features);
							details.setParents(parents);
							details.setRepositories(repositories);
							details.setProfileName(profileId);
							details.setProfileVersion(versionId);

							list.add(details);
							Profiles containerCap = new Profiles();
							containerCap.setProfileDetails(list);
							containerMap.put(associatedContainer.getId(), containerCap);
						}
					}

				}
			}
		}

		return containerMap;

	}

	public void getContainersToChange(final ProfileService profileService, final String oldConfigurationFile,
			final List<EnsembleContainer> ensembleContainerList, final PrintStream out) throws FileNotFoundException {

		List<EnsembleContainer> oldConfiguration = null;
		try {
			oldConfiguration = readConfigFile(oldConfigurationFile, out);
		} catch (FileNotFoundException e) {
			throw e;
		}
		ExecutorService executorService = Executors.newFixedThreadPool(1);
		LOG.info("Old Configuration {}",oldConfiguration);
		for (final EnsembleContainer oldContainer : oldConfiguration) {
			
			final String containerName = getContainerName(oldContainer.getContainerName());
			
			boolean containerExists = false;
			try {
				containerExists = fabricService.getContainer(containerName) == null ? false : true;
				LOG.info("container exists is {}",containerExists);
			} catch (Exception e) {
				LOG.error(e.getMessage());
			}
			if (!containerExists) {

				executorService.submit(new Runnable() {
					@Override
					public void run() {
						LOG.info("Container {} does not exisit attempting to create one {} {} ", containerName, jmxuser,
								jmxPassword);
						CreateContainerMetadata[] createContainers = null;
						try {
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
								String hostAddress = InetAddress.getByName(pickHost).getHostAddress();
								LOG.debug("host is {}", pickHost);
								LOG.debug(" Address is {} ", InetAddress.getByName(pickHost).getHostAddress());
								if (!profileService.getVersions().contains(oldContainer.getVersion())) {
									createVersionIfDoesnotExist(oldContainer.getVersion(), profileService);
								}
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
										.version(oldContainer.getVersion()).profiles("default")
										.dataStoreProperties(getDataStoreProperties()).uploadDistribution(true);
								createContainers = fabricService.createContainers(sshBuilder.build());
								Thread.sleep(1000L);
							}

							LOG.info(" Metat data is {}", createContainers);

							if (checkContainers(createContainers)) {
								Container container = fabricService.getContainer(containerName);
								compareAndSynch(null, oldContainer.getProfiles(), profileService, oldContainer);
								String oldVersion = container.getVersionId();
								createVersionIfDoesnotExist(oldContainer.getVersion(), profileService);
								container.setVersion(profileService.getRequiredVersion(oldContainer.getVersion()));
								LOG.info("Upgraded Container {} from version {}  to {} version ", containerName,
										oldVersion, oldContainer.getVersion());
							}
						} catch (Exception e) {
							LOG.error(e.getMessage(), e);
						}

					}
				});
			} 
			else {
				LOG.info("Synching existing container {} ", containerName);
				executorService.submit(new Runnable() {

					@Override
					public void run() {
						EnsembleContainer newContainer = null;
						try {
							for (EnsembleContainer tempContainer : ensembleContainerList) {
								LOG.info(tempContainer.getContainerName());
								if (containerName.equals(tempContainer.getContainerName())) {
									newContainer = tempContainer;
									break;
								}
							}
						} catch (Exception e) {
							LOG.error(e.getMessage(), e);
						}
						LOG.info("New Container {} ", newContainer);
						if (newContainer != null)
							compareAndSynch(newContainer.getProfiles(), oldContainer.getProfiles(), profileService,
									oldContainer);

					}

				});
			}

			// } // internal for end

		}
	}

	private String getContainerName(String containerName) {
		return containerName.replace(oldIdentifier, newIdentifier);
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
		LOG.info("Is success {} ", isSuccess);

		return isSuccess;
	}

	/*
	 * Create a in-memory profile if the give profileId does not exist with the
	 * given details
	 */
	private Profile createProfileIfNotPresent(String profileId, Version version, ProfileDetails oldProfileDetails) {

		Profile profile = version.getProfile(profileId);

		if (profile == null) {
			List<String> profileIds = version.getProfileIds();
			boolean profileExist = false;
			if (profileIds != null && profileIds.size() > 0) {
				profileExist = profileIds.contains(profileId);
			}
			if (!profileExist) {
				ProfileBuilder builder = ProfileBuilder.Factory.create(version.getId(),
						oldProfileDetails.getProfileName());
				builder.setParents(oldProfileDetails.getParents());
				builder.setBundles(oldProfileDetails.getBundles());
				builder.setFeatures(oldProfileDetails.getFeatures());
				if (oldProfileDetails.getConfigurations() != null)
					builder.setConfigurations(oldProfileDetails.getConfigurations());
				builder.version(version.getId());
				profile = builder.getProfile();
			}
		} else {
			if (!ignoreProfiles.contains(profile.getId())) {

				// Force delete the profile so that if any containers are associated it
				// works
				profileService.deleteProfile(oldProfileDetails.getProfileVersion(), profile.getId(), true);

				ProfileBuilder builder = ProfileBuilder.Factory.create(version.getId(),
						oldProfileDetails.getProfileName());
				builder.setParents(oldProfileDetails.getParents());
				builder.setBundles(oldProfileDetails.getBundles());
				builder.setFeatures(oldProfileDetails.getFeatures());

				if (oldProfileDetails.getConfigurations() != null)
					builder.setConfigurations(oldProfileDetails.getConfigurations());
				builder.version(version.getId());
				// Create the profile instead of an update( to avoid read and write lock issues)
				profileService.createProfile(builder.getProfile());
			}
		}
		return profile;
	}

	/*
	 * updates the profile as in the provided configuration
	 */
	public void updateProfile(ProfileService profileService, ProfileDetails oldProfileDetails, Version requiredVersion,
			Container container) {
		LOG.info("The profile {} exists in new env , we will update the existing profile ",
				oldProfileDetails.getProfileName());

		final Profile profile = profileService.getProfile(oldProfileDetails.getProfileVersion(),
				oldProfileDetails.getProfileName());

		// Firstly remove profiles so that we can change and re-assign them later
		// As we area not working with gitpush and gitpull
		// deleting and recreating the profile is the best part?
		if (container.getProfileIds().contains(profile.getId())) {
			removeProfiles(container, new ArrayList<String>() {
				{
					add(profile.getId());
				}
			}, false);
		}

		final Profile newProfile = createProfileIfNotPresent(oldProfileDetails.getProfileName(), requiredVersion,
				oldProfileDetails);

		addProfiles(container, new ArrayList<String>() {
			{
				add(newProfile.getId());
			}
		}, false);

	}

	/*
	 * Creates a new profile and associates it to the container as in the provided
	 * configuration
	 */
	public void createProfileAndAssociateToContainer(ProfileDetails oldProfileDetails, ProfileService profileService,
			Container container, Version requiredVersion) {

		LOG.info("The profile {} does not exist in new env , we will create  profile and add in version {}",
				oldProfileDetails.getProfileName(), requiredVersion);
		createVersionIfDoesnotExist(oldProfileDetails.getProfileVersion(), profileService);
		Profile newProfile = createProfileIfNotPresent(oldProfileDetails.getProfileName(), requiredVersion,
				oldProfileDetails);
		try {
			final Profile createProfile = profileService.createProfile(newProfile);
			container.setVersion(requiredVersion);
			addProfiles(container, new ArrayList<String>() {
				{
					add(createProfile.getId());
				}
			}, false);
		} catch (Exception e) {
			LOG.error("Profile {} not created Successfully {}", newProfile, e);
		}

	}

	/*
	 * Compares between the profiles on the file and 1. creates the version if it
	 * does not exist 2. creates the profile if it does not exist 3. updates the
	 * profile if it exists
	 */
	public void compareAndSynch(List<ProfileDetails> profilesList, List<ProfileDetails> oldProfilesList,
			ProfileService profileService, EnsembleContainer oldContainer) {

		String containerName = getContainerName(oldContainer.getContainerName());
		LOG.info("Processing for container {} ", containerName);

		if (profilesList == null) {

			Container container = fabricService.getContainer(containerName);
			List<String> profileNames = new ArrayList<String>();
			for (ProfileDetails profileDetail : oldProfilesList) {
				createVersionIfDoesnotExist(profileDetail.getProfileVersion(), profileService);
				Profile profile = profileService.getProfile(profileDetail.getProfileVersion(),
						profileDetail.getProfileName());
				Version version = profileService.getVersion(profileDetail.getProfileVersion());
				if (container == null) {
					try {
						// To wait if it is taking time to create the container
						Thread.sleep(6000L);
						container = fabricService.getContainer(containerName);
					} catch (InterruptedException e) {
						LOG.error(e.getMessage(), e);
					}
				}
				if (profile == null) {
					createProfileAndAssociateToContainer(profileDetail, profileService, container, version);
					profileNames.add(profileDetail.getProfileName());
				} else {
					updateProfile(profileService, profileDetail, version, container);
					profileNames.add(profileDetail.getProfileName());
				}
			}

		} else {
			// Remove any profiles that may not be present in the old container
			// but are associated to the container in the current env

			for (final ProfileDetails profileDet : profilesList) {

				boolean isMatch = false;
				for (final ProfileDetails oldProfileDet : oldProfilesList) {
					if (oldProfileDet.getProfileName().equalsIgnoreCase(profileDet.getProfileName())) {
						isMatch = true;
						break;
					}
				}
				if (!isMatch) {
					LOG.info("Disassociatoing profile {} from container {} ", profileDet.getProfileName(),
							containerName);
					Container container = fabricService.getContainer(containerName);
					if (!ignoreProfiles.contains(profileDet.getProfileName())) {
						removeProfiles(container, new ArrayList<String>() {
							{
								add(profileDet.getProfileName());
							}
						}, true);
					}
				}

			}
			/*
			 * Iterator<ProfileDetails> iterator = profilesList.iterator();
			 * List<ProfileDetails> newProfilesList = new
			 * ArrayList<ProfileDetails>(profilesList);
			 * 
			 * while (iterator.hasNext()) { ProfileDetails newProfileDetails =
			 * iterator.next(); for (ProfileDetails oldProfileDetails : oldProfilesList) {
			 * LOG.info("Comparing for deletion old: {} new : {} ",
			 * oldProfileDetails.getProfileName(), newProfileDetails.getProfileName()); if
			 * (oldProfileDetails.getProfileName().equalsIgnoreCase(newProfileDetails.
			 * getProfileName())) { newProfilesList.add(newProfileDetails); } } }
			 */

			LOG.info("New Profiles List {}", profilesList);
			List<ProfileDetails> profilesThatNeedtoBeProcessed = new ArrayList<ProfileDetails>(oldProfilesList);
			profilesThatNeedtoBeProcessed.removeAll(profilesList);

			for (ProfileDetails oldProfileDetails : profilesThatNeedtoBeProcessed) {
				for (ProfileDetails newProfileDetails : profilesList) {
					ProfileDetails newProfileDetail = null;
					LOG.info(" Comparing Profile  oldProfile {} with newProfile {}", oldProfileDetails.getProfileName(),
							newProfileDetails.getProfileName());
					if (oldProfileDetails.getProfileName().equalsIgnoreCase(newProfileDetails.getProfileName())) {
						if (oldProfileDetails.equals(newProfileDetails)) {
							break;
						} else {
							LOG.info(" Synching  Profile  oldProfile {} with newProfile {}", oldProfileDetails,
									newProfileDetails);
							if (!oldProfileDetails.getProfileVersion()
									.equalsIgnoreCase(newProfileDetails.getProfileVersion())) {
								// Create the new version before attempting anything else
								createVersionIfDoesnotExist(oldProfileDetails.getProfileVersion(), profileService);
							}

							Version requiredVersion = profileService.getVersion(oldProfileDetails.getProfileVersion());
							Profile profile = requiredVersion.getProfile(oldProfileDetails.getProfileName());
							Container container = fabricService.getContainer(containerName);
							if (profile != null) {
								updateProfile(profileService, oldProfileDetails, requiredVersion, container);
							} else {
								createProfileAndAssociateToContainer(oldProfileDetails, profileService, container,
										requiredVersion);
							}
							Version createVersionIfDoesnotExist = createVersionIfDoesnotExist(oldContainer.getVersion(),
									profileService);
							container.setVersion(createVersionIfDoesnotExist);

						}
					}

				}

			}

		}
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
			// TODO Auto-generated catch block
			// Skipping this as it is a rare-use module and
			// this exception will not cause that much of a
			// issue for the output
			LOG.warn(e.getMessage(), e);
		}
		return oldConfiguration;
	}

	// Need to derive logic to associate hostname and containers
	public String getHost(List<String> hosts, String containerName) {
		return hosts.get(0);
	}

}
