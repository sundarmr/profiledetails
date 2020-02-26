package org.redhat.fabric.commands.model;

import java.io.Serializable;
import java.util.List;

public class EnsembleContainer implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String containerName;
	private List<ProfileDetails> profiles;
	private List<Context> contexts;
	private String version;
	private String parent;
	private String envDefaultVersion;

	public List<Context> getContexts() {
		return contexts;
	}

	public void setContexts(List<Context> contexts) {
		this.contexts = contexts;
	}

	public String getContainerName() {
		return containerName;
	}

	public void setContainerName(String containerName) {
		this.containerName = containerName;
	}

	public List<ProfileDetails> getProfiles() {
		return profiles;
	}

	public void setProfiles(List<ProfileDetails> profiles) {
		this.profiles = profiles;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public String getEnvDefaultVersion() {
		return envDefaultVersion;
	}

	public void setEnvDefaultVersion(String envDefaultVersion) {
		this.envDefaultVersion = envDefaultVersion;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((containerName == null) ? 0 : containerName.hashCode());
		result = prime * result + ((contexts == null) ? 0 : contexts.hashCode());
		result = prime * result + ((envDefaultVersion == null) ? 0 : envDefaultVersion.hashCode());
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		result = prime * result + ((profiles == null) ? 0 : profiles.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	// Intentional overide of equals to so as to compare only
	// containernames and not other objects
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;

		EnsembleContainer newContainer = (EnsembleContainer) obj;
		if (!this.containerName.equalsIgnoreCase(newContainer.getContainerName()))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "EnsembleContainer [containerName=" + containerName + ", profiles=" + profiles + ", contexts=" + contexts
				+ ", version=" + version + ", parent=" + parent + ", envDefaultVersion=" + envDefaultVersion + "]";
	}

}
