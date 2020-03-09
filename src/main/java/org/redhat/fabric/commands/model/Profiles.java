package org.redhat.fabric.commands.model;

import java.io.Serializable;

import org.apache.mina.util.ConcurrentHashSet;

public class Profiles implements Serializable{
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8545269721774637676L;
	private ConcurrentHashSet<ProfileDetails> profileDetails;

	public ConcurrentHashSet<ProfileDetails> getProfileDetails() {
		return profileDetails;
	}

	public void setProfileDetails(ConcurrentHashSet<ProfileDetails> profileDetails) {
		this.profileDetails = profileDetails;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((profileDetails == null) ? 0 : profileDetails.hashCode());
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
		Profiles other = (Profiles) obj;
		if (profileDetails == null) {
			if (other.profileDetails != null)
				return false;
		} else if (!profileDetails.equals(other.profileDetails))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Profiles [profileDetails=" + profileDetails + "]";
	}
	
}
