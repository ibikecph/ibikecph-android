package dk.kk.ibikecphlib.login;

public interface FBLoginListener {
	public void onFBLoginSuccess(String token);

	public void onFBLoginError();
}
