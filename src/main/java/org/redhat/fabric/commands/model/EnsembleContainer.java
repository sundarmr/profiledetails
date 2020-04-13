package org.redhat.fabric.commands.model;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;

import org.apache.mina.util.ConcurrentHashSet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import jline.internal.Log;

public class EnsembleContainer implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String containerName;
	private ConcurrentHashSet<ProfileDetails> profiles;
	private HashSet<Context> contexts;
	private String version;
	private String parent;
	private String envDefaultVersion;

	public HashSet<Context> getContexts() {
		return contexts;
	}

	public void setContexts(HashSet<Context> contexts) {
		this.contexts = contexts;
	}

	public String getContainerName() {
		return containerName;
	}

	public void setContainerName(String containerName) {
		this.containerName = containerName;
	}

	public ConcurrentHashSet<ProfileDetails> getProfiles() {
		return profiles;
	}

	public void setProfiles(ConcurrentHashSet<ProfileDetails> profiles) {
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
		if (envDefaultVersion == null) {
			if (other.envDefaultVersion != null)
				return false;
		} else if (!envDefaultVersion.equals(other.envDefaultVersion))
			return false;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		if (profiles == null) {
			if (other.profiles != null)
				return false;
		} else if (!profiles.equals(other.profiles))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	@Override
	public String toString() {
		Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
		Type profileListType = new TypeToken<EnsembleContainer>() {
		}.getType();
		return gson.toJson(this,profileListType);
	}

}
