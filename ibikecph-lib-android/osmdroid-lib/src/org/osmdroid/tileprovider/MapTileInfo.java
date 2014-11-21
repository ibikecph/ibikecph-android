package org.osmdroid.tileprovider;

public class MapTileInfo {
	
	private String url;
	private String lastModified;
	private String eTag;
	private boolean expired;
	
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getLastModified() {
		return lastModified;
	}
	public void setLastModified(String lastModified) {
		this.lastModified = lastModified;
	}
	public String geteTag() {
		return eTag;
	}
	public void seteTag(String eTag) {
		this.eTag = eTag;
	}
	public boolean isExpired() {
		return expired;
	}
	public void setExpired(boolean expired) {
		this.expired = expired;
	}
	
	@Override
	public String toString() {
		return "MapTileInfo [url=" + url + ", lastModified=" + lastModified
				+ ", eTag=" + eTag + ", expired=" + expired + "]";
	}
	
	

}
