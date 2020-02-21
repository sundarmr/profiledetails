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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((containerName == null) ? 0 : containerName.hashCode());
		result = prime * result + ((contexts == null) ? 0 : contexts.hashCode());
		result = prime * result + ((profiles == null) ? 0 : profiles.hashCode());
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
		EnsembleContainer other = (EnsembleContainer) obj;
		if (containerName == null) {
			if (other.containerName != null)
				return false;
		} else if (!containerName.equals(other.containerName))
			return false;
		if (contexts == null) {
			if (other.contexts != null)
				return false;
		} else if (!contexts.equals(other.contexts))
			return false;
		if (profiles == null) {
			if (other.profiles != null)
				return false;
		} else if (!profiles.equals(other.profiles))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "EnsembleContainer [containerName=" + containerName + ", profiles=" + profiles + ", contexts=" + contexts
				+ "]";
	}

}
