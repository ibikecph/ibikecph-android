package com.spoiledmilk.ibikecph.login;

public interface FBLoginListener {
	public void onFBLoginSuccess(String token);

	public void onFBLoginError();
}
