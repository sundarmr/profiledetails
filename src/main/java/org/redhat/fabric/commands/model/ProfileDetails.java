package org.redhat.fabric.commands.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;

import io.fabric8.api.Profile;

public class ProfileDetails implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5069214002075202593L;
	private String profileName;
	private String profileVersion;
	private HashSet<String> bundles;
	private HashSet<String> parents;
	private HashSet<String> repositories;
	private HashSet<String> fabs;
	private HashSet<String> features;
	private HashSet<String> pids;
	private Map<String, Map<String, String>> configurations;
	private Map<String,String> profileConfig ;
	
	public ProfileDetails() {
		super();
	}
	
	public ProfileDetails(String profileName, String profileVersion, HashSet<String> bundles, HashSet<String> parents,
			HashSet<String> repositories, HashSet<String> fabs, HashSet<String> features, HashSet<String> pids,
			Map<String, Map<String, String>> configurations, Map<String, String> profileConfig) {
		super();
		this.profileName = profileName;
		this.profileVersion = profileVersion;
		this.bundles = bundles;
		this.parents = parents;
		this.repositories = repositories;
		this.fabs = fabs;
		this.features = features;
		this.pids = pids;
		this.configurations = configurations;
		this.profileConfig = profileConfig;
	}

	public String getProfileName() {
		return profileName;
	}
	public void setProfileName(String profileName) {
		this.profileName = profileName;
	}
	public String getProfileVersion() {
		return profileVersion;
	}
	public void setProfileVersion(String profileVersion) {
		this.profileVersion = profileVersion;
	}
	public HashSet<String> getBundles() {
		return bundles;
	}
	public void setBundles(HashSet<String> bundles) {
		this.bundles = bundles;
	}
	public HashSet<String> getParents() {
		return parents;
	}
	public void setParents(HashSet<String> parents) {
		this.parents = parents;
	}
	public HashSet<String> getRepositories() {
		return repositories;
	}
	public void setRepositories(HashSet<String> repositories) {
		this.repositories = repositories;
	}
	public HashSet<String> getFabs() {
		return fabs;
	}
	public void setFabs(HashSet<String> fabs) {
		this.fabs = fabs;
	}
	public HashSet<String> getFeatures() {
		return features;
	}
	public void setFeatures(HashSet<String> features) {
		this.features = features;
	}
	public HashSet<String> getPids() {
		return pids;
	}
	public void setPids(HashSet<String> pids) {
		this.pids = pids;
	}
	public Map<String, Map<String, String>> getConfigurations() {
		return configurations;
	}
	public void setConfigurations(Map<String, Map<String, String>> configurations) {
		this.configurations = configurations;
	}
	public Map<String, String> getProfileConfig() {
		return profileConfig;
	}
	public void setProfileConfig(Map<String, String> profileConfig) {
		this.profileConfig = profileConfig;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bundles == null) ? 0 : bundles.hashCode());
		result = prime * result + ((configurations == null) ? 0 : configurations.hashCode());
		result = prime * result + ((fabs == null) ? 0 : fabs.hashCode());
		result = prime * result + ((features == null) ? 0 : features.hashCode());
		result = prime * result + ((parents == null) ? 0 : parents.hashCode());
		result = prime * result + ((pids == null) ? 0 : pids.hashCode());
		result = prime * result + ((profileConfig == null) ? 0 : profileConfig.hashCode());
		result = prime * result + ((profileName == null) ? 0 : profileName.hashCode());
		result = prime * result + ((profileVersion == null) ? 0 : profileVersion.hashCode());
		result = prime * result + ((repositories == null) ? 0 : repositories.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProfileDetails other = (ProfileDetails) obj;
		if (bundles == null) {
			if (other.bundles != null)
				return false;
		} else if (!bundles.equals(other.bundles))
			return false;
		if (configurations == null) {
			if (other.configurations != null)
				return false;
		} else if (!configurations.equals(other.configurations))
			return false;
		if (fabs == null) {
			if (other.fabs != null)
				return false;
		} else if (!fabs.equals(other.fabs))
			return false;
		if (features == null) {
			if (other.features != null)
				return false;
		} else if (!features.equals(other.features))
			return false;
		if (parents == null) {
			if (other.parents != null)
				return false;
		} else if (!parents.equals(other.parents))
			return false;
		if (pids == null) {
			if (other.pids != null)
				return false;
		} else if (!pids.equals(other.pids))
			return false;
		if (profileConfig == null) {
			if (other.profileConfig != null)
				return false;
		} else if (!profileConfig.equals(other.profileConfig))
			return false;
		if (profileName == null) {
			if (other.profileName != null)
				return false;
		} else if (!profileName.equals(other.profileName))
			return false;
		if (profileVersion == null) {
			if (other.profileVersion != null)
				return false;
		} else if (!profileVersion.equals(other.profileVersion))
			return false;
		if (repositories == null) {
			if (other.repositories != null)
				return false;
		} else if (!repositories.equals(other.repositories))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "ProfileDetails [profileName=" + profileName + ", profileVersion=" + profileVersion + ", bundles="
				+ bundles + ", parents=" + parents + ", repositories=" + repositories + ", fabs=" + fabs + ", features="
				+ features + ", pids=" + pids + ", configurations=" + configurations + ", profileConfig="
				+ profileConfig + "]";
	}

}
