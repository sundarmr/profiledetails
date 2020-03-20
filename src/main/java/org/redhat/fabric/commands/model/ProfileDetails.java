package org.redhat.fabric.commands.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProfileDetails implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5069214002075202593L;
	private String profileName;
	private String profileVersion;
	private List<String> bundles;
	private List<String> parents;
	private List<String> repositories;
	private List<String> fabs;
	private List<String> features;
	private Map<String, Map<String, String>> configurations;
	
	public ProfileDetails() {
		super();
	}
	


	public ProfileDetails(String profileName, String profileVersion, List<String> bundles, List<String> parents,
			List<String> repositories, List<String> fabs, List<String> features,
			Map<String, Map<String, String>> configurations) {
		super();
		this.profileName = profileName;
		this.profileVersion = profileVersion;
		this.bundles = bundles;
		this.parents = parents;
		this.repositories = repositories;
		this.fabs = fabs;
		this.features = features;
		this.configurations = configurations;
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



	public List<String> getBundles() {
		return bundles;
	}



	public void setBundles(List<String> bundles) {
		this.bundles = bundles;
	}



	public List<String> getParents() {
		return parents;
	}



	public void setParents(List<String> parents) {
		this.parents = parents;
	}



	public List<String> getRepositories() {
		return repositories;
	}



	public void setRepositories(List<String> repositories) {
		this.repositories = repositories;
	}



	public List<String> getFabs() {
		return fabs;
	}



	public void setFabs(List<String> fabs) {
		this.fabs = fabs;
	}



	public List<String> getFeatures() {
		return features;
	}



	public void setFeatures(List<String> features) {
		this.features = features;
	}



	public Map<String, Map<String, String>> getConfigurations() {
		return configurations;
	}



	public void setConfigurations(Map<String, Map<String, String>> configurations) {
		this.configurations = configurations;
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
		result = prime * result + ((profileName == null) ? 0 : profileName.hashCode());
		result = prime * result + ((profileVersion == null) ? 0 : profileVersion.hashCode());
		result = prime * result + ((repositories == null) ? 0 : repositories.hashCode());
		return result;
	}



	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
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
				+ features + ", configurations=" + configurations + "]";
	}


	
}
