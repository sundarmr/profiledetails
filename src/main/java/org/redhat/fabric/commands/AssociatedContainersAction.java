package org.redhat.fabric.commands;

import static io.fabric8.commands.support.CommandUtils.sortProfiles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;
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
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileService;
import io.fabric8.api.Version;
import io.fabric8.api.VersionBuilder;
import io.fabric8.boot.commands.support.FabricCommand;
import jline.internal.Log;

@Command(name = AssociatedContainers.FUNCTION_VALUE, scope = AssociatedContainers.SCOPE_VALUE, description = AssociatedContainers.DESCRIPTION)
public class AssociatedContainersAction extends AbstractAction {

	Logger LOG = LoggerFactory.getLogger(AssociatedContainersAction.class);

	@Argument(index = 0, required = false, name = "filePath", description = "Path to the profile.")
	@CompleterValues(index = 0)
	private String filePath;

	@Option(name = "--containerslist", description = "List of containers that are different")
	private String containersList;

	private final FabricService fabricService;

	AssociatedContainersAction(FabricService fabricService) {
		this.fabricService = fabricService;
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

		if (filePath == null && containersList == null) {
			LOG.info("Printing Configuration...");

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

		else if (containersList != null && !containersList.isEmpty() && containersList.equalsIgnoreCase("listUrls")) {
			Container[] containers = fabricService.getContainers();
			Map<String, List<Context>> jolokiaMap = new HashMap<String, List<Context>>();
			for (Container container : containers) {
				getContextList(out, container);
				jolokiaMap.put(container.getId(), getContextList(out, container));
			}
		}

		/*
		 * else if (containersList != null && !containersList.isEmpty()) {
		 * List<EnsembleContainer> containersToChange =
		 * getContainersToChange(profileService, filePath, containerMap, out);
		 * StringBuffer buf = new StringBuffer(); for (EnsembleContainer container :
		 * containersToChange) { buf.append(container.getContainerName()).append(" "); }
		 * out.print(buf.toString()); out.flush(); }
		 */

		else if ((filePath != null && !filePath.isEmpty()) && containersList == null) {
			masterContainerMap = getContainerMap(profileService, versions);
			List<EnsembleContainer> containersToChange = getContainersToChange(profileService, filePath,
					masterContainerMap, out);
			for (EnsembleContainer container : containersToChange) {
				if (container.getProfiles() != null && container.getProfiles().size() > 0) {
					out.append(container.getContainerName()).append(" ");
					for (int i = 0; i < container.getProfiles().size(); i++) {
						out.append(container.getProfiles().get(i).getProfileName());
						if (i != container.getProfiles().size() - 1) {
							out.append(" ");
						}
					}
					out.append("\n");
				}
			}
		}

		// out.print( gson.toJson(containersToChange) );

		return null;
	}

	private Map<String, Profiles> getContainerMap(ProfileService profileService, List<String> versions) {

		Map<String, Profiles> masterContainerMap = new HashMap<String, Profiles>();

		for (String versionId : versions) {
			LOG.info("Version now {} ", versionId);
			Version requiredVersion = profileService.getRequiredVersion(versionId);
			List<Profile> profiles = sortProfiles(requiredVersion.getProfiles());
			Map<String, Profiles> containerMap = printProfiles(profileService, profiles, System.out, versionId);

			for (Entry<String, Profiles> entrySet : containerMap.entrySet()) {
				if (masterContainerMap.containsKey(entrySet.getKey())) {
					Profiles associatedProfiles = masterContainerMap.get(entrySet.getKey());
					associatedProfiles.getProfileDetails()
							.addAll(containerMap.get(entrySet.getKey()).getProfileDetails());

				} else {
					masterContainerMap.put(entrySet.getKey(), entrySet.getValue());
				}
			}
		}
		return masterContainerMap;
	}

	private List<Context> getContextList(PrintStream out, Container container) {

		Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();

		Container[] containers = fabricService.getContainers();
		LOG.info("working on getting the jolokia map {} ,{} ", containers, fabricService.getCurrentContainer());
		List<Context> contexts = new ArrayList<Context>();
		StringBuilder sb = new StringBuilder();

		if (container.isAlive() && !container.isRoot() && !container.isEnsembleServer() && container.isManaged()) {
			if (sb.toString().length() > 0)
				sb.delete(0, sb.toString().length() - 1);
			String jolokiaUrl = container.getJolokiaUrl();
			jolokiaUrl = jolokiaUrl.replace("MacBook-Pro", "localhost");

			URL url = null;
			HttpURLConnection connection = null;
			try {
				url = new URL((new StringBuilder(jolokiaUrl).append(
						"/read/org.apache.camel:context=*,type=context,name=*/TotalRoutes,CamelId,State,StartedRoutes"))
								.toString());

				connection = (HttpURLConnection) url.openConnection();
				String auth = "admin" + ":" + "admin";
				byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
				String authHeaderValue = "Basic " + new String(encodedAuth);
				connection.setRequestProperty("Authorization", authHeaderValue);

				connection.setRequestMethod("GET");
				connection.connect();
				if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
					BufferedReader buffer = new BufferedReader(new InputStreamReader(connection.getInputStream()));
					String line;

					while ((line = buffer.readLine()) != null) {
						sb.append(line);
					}
					// out.print("Container "+container.getId()+ " : \n "+sb.toString());
				} else {
					out.print("Error ....");
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
				LOG.info("Skipping the response that is error");
			}
		}

		// when using with a file path it means we want to compare from
		// existing environment
		if (filePath != null && !filePath.isEmpty()) {
			String fileData = readFile(filePath);

			Type fileType = new TypeToken<HashMap<String, String>>() {
			}.getType();

			HashMap<String, String> fromJson = gson.fromJson(fileData, fileType);

		}
		return contexts;
	}

	private String readFile(String configurationPath) {

		File oldJson = new File(configurationPath);

		BufferedReader bo = null;
		StringBuffer oldBuffer = new StringBuffer();

		try {
			bo = new BufferedReader(new FileReader(oldJson));
			String line = "";
			while ((line = bo.readLine()) != null) {
				oldBuffer.append(line);
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		return oldBuffer.toString();
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
					LOG.info("Container to assess .. {}", associatedContainer);
					if (!associatedContainer.isRoot() && !associatedContainer.isEnsembleServer()
							&& associatedContainer.isManaged()) {
						LOG.info("Container to assessed .. {}", associatedContainer);
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

	private List<EnsembleContainer> getContainersToChange(ProfileService profileService, String oldConfigurationFile,
			Map<String, Profiles> newConfiguration, PrintStream out) {

		File oldJson = new File(oldConfigurationFile);
		Type profileListType = new TypeToken<HashMap<String, Profiles>>() {
		}.getType();
		Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();

		BufferedReader bo = null;
		StringBuffer oldBuffer = new StringBuffer();

		try {
			bo = new BufferedReader(new FileReader(oldJson));
			String line = "";
			while ((line = bo.readLine()) != null) {
				oldBuffer.append(line);
			}
		} catch (Exception e) {
			out.print(e.getMessage());
			LOG.error(e.getMessage(), e);
		}
		LOG.info(" file Path is {} ", oldConfigurationFile);
		LOG.info(" file content is {} ", oldBuffer.toString());

		HashMap<String, Profiles> oldConfiguration = null;
		try {
			oldConfiguration = gson.fromJson(oldBuffer.toString(), profileListType);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		LOG.info("The new object is {} :", oldConfiguration);

		StringBuffer diffContainers = new StringBuffer();
		List<EnsembleContainer> ensembleDifference = new ArrayList<EnsembleContainer>();

		for (Map.Entry<String, Profiles> oldEntry : oldConfiguration.entrySet()) {
			// Need to leave out root containers
			if (!oldEntry.getKey().equalsIgnoreCase("root")) {

				List<ProfileDetails> newProfilesList = newConfiguration.get(oldEntry.getKey()) == null ? null
						: newConfiguration.get(oldEntry.getKey()).getProfileDetails();
				List<ProfileDetails> oldProfilesList = oldConfiguration.get(oldEntry.getKey()).getProfileDetails();

				if (newProfilesList == null) {
					// Scenario where a container is missing in the new environment
					// 1. Create the container
					// 2. Synch the profiles from old to the new container
					try {

						LOG.info("name:{}\nparent:root\nzkurl: {}\nzkpassword:{}\njvmopts:{}\nprofiles:{}\nversion: {}",
								oldEntry.getKey(), fabricService.getZookeeperUrl(),
								fabricService.getZookeeperPassword(), fabricService.getDefaultJvmOptions(),
								getProfileNames(oldProfilesList, profileService));

						CreateChildContainerOptions.Builder builder = CreateChildContainerOptions.builder()
								.name(oldEntry.getKey()).parent("root").ensembleServer(false)
								.zookeeperUrl(fabricService.getZookeeperUrl())
								.zookeeperPassword(fabricService.getZookeeperPassword()).jmxPassword("admin")
								.jmxUser("admin").version(fabricService.getDefaultVersionId())
								.jvmOpts(fabricService.getDefaultJvmOptions()).profiles("default");

						fabricService.createContainers(builder.build());

						compareAndSynch(oldConfiguration.get(oldEntry.getKey()).getProfileDetails(), oldProfilesList,
								profileService, fabricService, oldEntry, ensembleDifference, diffContainers);

					} catch (Exception e) {
						LOG.error("unable to create container {} ", oldEntry.getKey(), e);
					}

				} else {

					compareAndSynch(newProfilesList, oldProfilesList, profileService, fabricService, oldEntry,
							ensembleDifference, diffContainers);

				}
			}
		}

		return ensembleDifference;
	}

	private Profile createProfileIfNotPresent(String profileId, Version version, ProfileDetails oldProfileDetails) {

		Profile profile = version.getProfile(profileId);
		if (profile == null) {
			List<String> profileIds = version.getProfileIds();
			boolean profileExist = false;
			if (profileIds != null && profileIds.size() > 0) {
				profileExist = profileIds.contains(profileId);

			}
			if (profileExist) {
				ProfileBuilder builder = ProfileBuilder.Factory.create(version.getId(),
						oldProfileDetails.getProfileName());
				builder.setParents(oldProfileDetails.getParents());
				builder.setBundles(oldProfileDetails.getBundles());
				builder.setFeatures(oldProfileDetails.getFeatures());
				profile = builder.getProfile();
			}
		}
		return profile;
	}

	private void compareAndSynch(List<ProfileDetails> newProfilesList, List<ProfileDetails> oldProfilesList,
			ProfileService profileService, FabricService fabricService, Map.Entry<String, Profiles> oldEntry,
			List<EnsembleContainer> ensembleDifference, StringBuffer diffContainers) {

		for (ProfileDetails newProfileDetails : newProfilesList) {
			boolean isMatch = false;
			for (ProfileDetails oldProfileDetails : oldProfilesList) {
				Log.info(" Comparing Profile  oldProfile {} with newProfile {}", oldProfileDetails, newProfileDetails);
				if (isEqual(oldProfileDetails, newProfileDetails)) {
					Log.info("Is a match ");
					isMatch = true;
					break;
				} else {
					if (!oldProfileDetails.getProfileVersion()
							.equalsIgnoreCase(newProfileDetails.getProfileVersion())) {
						// Special Case Consideration
						// you need to check if such a version exists in the current environment
						// if so then it is straightforward to get the oldProfile replace the newprofile
						// and assign and upgrade the container else create a version then replace the
						// newer
						// version with the old version profile.
						Version requiredVersion = createVersionIfDoesnotExist(oldProfileDetails, profileService);

						Profile profile = requiredVersion.getProfile(oldProfileDetails.getProfileName());

						ProfileBuilder builder = ProfileBuilder.Factory.create(requiredVersion.getId(),
								oldProfileDetails.getProfileName());
						builder.setParents(oldProfileDetails.getParents());
						builder.setBundles(oldProfileDetails.getBundles());
						builder.setFeatures(oldProfileDetails.getFeatures());
						Profile newProfile = builder.getProfile();
						Container container = fabricService.getContainer(oldEntry.getKey());
						if (profile != null) {
							LOG.info("The profile {} exists in new env , we will update the existing profile ",
									oldProfileDetails.getProfileName());
							profileService.updateProfile(newProfile);
							container.setVersion(requiredVersion);
							container.addProfiles(newProfile);
						} else {
							LOG.info("The profile {} does not exist in new env , we will create  profile and add",
									oldProfileDetails.getProfileName());
							Profile createProfile = profileService.createProfile(newProfile);
							Profile[] profiles = container.getProfiles();
							List<String> profileNames = new ArrayList<String>();
							for (Profile profileName : profiles) {
								profileNames.add(profileName.getId());
							}

							profileNames.add(createProfile.getId());
							profileService.updateProfile(createProfile);

							Container cont = FabricCommand.getContainer(fabricService, container.getId());
							// we can only change to existing profiles
							Profile[] profs = FabricCommand.getExistingProfiles(fabricService, cont.getVersion(),
									profileNames);
							cont.setProfiles(profs);

						}

					} else {

						Version requiredVersion = profileService.getVersion(oldProfileDetails.getProfileVersion());
						LOG.info("Required Version is {} ", requiredVersion);
						if (requiredVersion == null) {
							requiredVersion = createVersionIfDoesnotExist(oldProfileDetails, profileService);
						}
						LOG.info("Retreiveing the required version in which the profile needs to be created {}",
								oldProfileDetails.getProfileVersion());
						LOG.info("Required Version is {} ", requiredVersion);
						Profile profile = requiredVersion.getProfile(oldProfileDetails.getProfileName());
						ProfileBuilder builder = ProfileBuilder.Factory.create(requiredVersion.getId(),
								oldProfileDetails.getProfileName());
						builder.setParents(oldProfileDetails.getParents());
						builder.setBundles(oldProfileDetails.getBundles());
						builder.setFeatures(oldProfileDetails.getFeatures());
						Profile newProfile = builder.getProfile();

						if (profile != null) {
							LOG.info("The profile {} exists in new env , we will update the existing profile ",
									oldProfileDetails.getProfileName());
							profileService.updateProfile(newProfile);
						} else {
							LOG.info("The profile {} does not exist in new env , we will create  profile and add",
									oldProfileDetails.getProfileName());
							// create the profile with the old profile details
							// mark the container for upgrade
							// Profile newProfile = new ProfileBuildersImpl().profileBuilder().getProfile();
							// ProfileBuilder builder =
							// ProfileBuilder.Factory.create(requiredVersion.getId(),
							// oldProfileDetails.getProfileName());
							// Profile profile2 = builder.getProfile();
							profileService.createProfile(newProfile);

						}
						Container container = fabricService.getContainer(oldEntry.getKey());
						if (container == null) {
							// ?? will this scenario ever happen
							LOG.info("Container {} does not exist .. create", oldEntry.getKey());
						}
						container.setVersion(requiredVersion);
					}
				}
			}
			if (!isMatch) {
				EnsembleContainer container = new EnsembleContainer();
				container.setContainerName(oldEntry.getKey());
				List<String> profilesAssociated = new ArrayList<String>();
				for (ProfileDetails details : oldProfilesList) {
					profilesAssociated.add(details.getProfileName());
				}
				container.setProfiles(oldProfilesList);
				ensembleDifference.add(container);
				diffContainers.append(oldEntry.getKey());
			}
		}

	}

	private Set<String> getProfileNames(List<ProfileDetails> oldProfilesList, ProfileService profileService) {

		Set<String> profileNames = new LinkedHashSet<String>();
		for (ProfileDetails profileDetails : oldProfilesList) {

			Profile requiredProfile = null;
			try {
				requiredProfile = profileService.getRequiredProfile(profileDetails.getProfileVersion(),
						profileDetails.getProfileName());
			} catch (Exception e) {
				LOG.error("Profile {} with version {} is not present", profileDetails.getProfileName(),
						profileDetails.getProfileVersion());
			}
			if (requiredProfile != null) {
				profileNames.add(profileDetails.getProfileName());
			}

		}
		if (profileNames.size() == 0) {
			profileNames.add("default");
		}
		return profileNames;
	}

	private Version createVersionIfDoesnotExist(ProfileDetails oldProfileDetails, ProfileService profileService) {

		Version requiredVersion = null;
		String productionProfileVersion = oldProfileDetails.getProfileVersion();
		try {
			requiredVersion = profileService.getRequiredVersion(productionProfileVersion);
		} catch (Exception e) {
			LOG.error(e.getMessage());
		}
		if (requiredVersion == null) {
			try {
				String sourceId = profileService.getVersions().get(profileService.getVersions().size() - 1);
				LOG.info("Parent Version {} ", sourceId);
				LOG.info("Target Version {} ", productionProfileVersion);
				if (sourceId != null) {
					Map<String, String> attributes = new HashMap<String, String>(
							Collections.singletonMap(Version.PARENT, sourceId));
					attributes.put(Version.DESCRIPTION, "Created by ansible to replicate prod");
					requiredVersion = profileService.createVersionFrom(sourceId, productionProfileVersion, attributes);
					LOG.info("Creating new version {} from source version {} ", productionProfileVersion, sourceId);
				} else {
					VersionBuilder builder = VersionBuilder.Factory.create(oldProfileDetails.getProfileVersion());
					builder.addAttribute(Version.DESCRIPTION, "Created by ansible to replicate prod");
					requiredVersion = profileService.createVersion(builder.getVersion());
					LOG.info("Creating new version with no base");
				}
			} catch (Exception e) {
				LOG.error("Unable to create new Version {} ", oldProfileDetails.getProfileVersion());
				LOG.error(e.getMessage(), e);
			}
		}
		return requiredVersion;
	}

	private boolean isEqual(ProfileDetails oldObj, ProfileDetails newObj) {
		if (oldObj.getBundles() == null)
			if (newObj.getBundles() != null)
				return false;
		if (!newObj.getBundles().equals(oldObj.getBundles()))
			return false;

		if (oldObj.getFabs() == null)
			if (oldObj.getFabs() != null)
				return false;
		if (newObj.getFabs() == null || !newObj.getFabs().equals(oldObj.getFabs()))
			return false;

		if (oldObj.getFeatures() == null)
			if (oldObj.getFeatures() != null)
				return false;
		if (newObj.getFeatures() == null || !newObj.getFeatures().equals(oldObj.getFeatures()))
			return false;

		if (oldObj.getParents() == null)
			if (oldObj.getParents() != null)
				return false;
		if (newObj.getParents() == null || !newObj.getParents().equals(oldObj.getParents()))
			return false;

		if (oldObj.getPids() == null)
			if (oldObj.getPids() != null)
				return false;
		if (newObj.getPids() == null || !newObj.getPids().equals(oldObj.getPids()))
			return false;

		if (oldObj.getProfileConfig() == null)
			if (oldObj.getProfileConfig() != null)
				return false;
		if (newObj.getProfileConfig() == null || !newObj.getProfileConfig().equals(oldObj.getProfileConfig()))
			return false;

		if (oldObj.getProfileName() == null)
			if (oldObj.getProfileName() != null)
				return false;
		if (newObj.getProfileName() == null || !newObj.getProfileName().equals(oldObj.getProfileName()))
			return false;

		if (oldObj.getProfileVersion() == null)
			if (oldObj.getProfileVersion() != null)
				return false;
		if (newObj.getProfileVersion() == null || !newObj.getProfileVersion().equals(oldObj.getProfileVersion()))
			return false;
		if (oldObj.getRepositories() == null)
			if (oldObj.getRepositories() != null)
				return false;
		if (newObj.getRepositories() == null || !newObj.getRepositories().equals(oldObj.getRepositories()))
			return false;

		return true;

	}
}
