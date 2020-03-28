package org.redhat.fabric.commands.model;

import java.io.Serializable;

import com.google.gson.annotations.SerializedName;

public class Context implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3924161041891018839L;
	@SerializedName("CamelId")
	private String contextId;
	@SerializedName("TotalRoutes")
	private int totalRoutes;
	@SerializedName("StartedRoutes")
	private int startedRoutes;
	@SerializedName("State")
	private String contextState;
	public String getContextId() {
		return contextId;
	}
	public void setContextId(String contextId) {
		this.contextId = contextId;
	}
	public int getTotalRoutes() {
		return totalRoutes;
	}
	public void setTotalRoutes(int totalRoutes) {
		this.totalRoutes = totalRoutes;
	}
	public int getStartedRoutes() {
		return startedRoutes;
	}
	public void setStartedRoutes(int startedRoutes) {
		this.startedRoutes = startedRoutes;
	}
	public String getContextState() {
		return contextState;
	}
	public void setContextState(String contextState) {
		this.contextState = contextState;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((contextId == null) ? 0 : contextId.hashCode());
		result = prime * result + ((contextState == null) ? 0 : contextState.hashCode());
		result = prime * result + startedRoutes;
		result = prime * result + totalRoutes;
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
		Context other = (Context) obj;
		if (contextId == null) {
			if (other.contextId != null)
				return false;
		} else if (!contextId.equals(other.contextId))
			return false;
		if (contextState == null) {
			if (other.contextState != null)
				return false;
		} else if (!contextState.equals(other.contextState))
			return false;
		if (startedRoutes != other.startedRoutes)
			return false;
		if (totalRoutes != other.totalRoutes)
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "Context [contextId=" + contextId + ", totalRoutes=" + totalRoutes + ", startedRoutes=" + startedRoutes
				+ ", contextState=" + contextState + "]";
	}
	
	
}
