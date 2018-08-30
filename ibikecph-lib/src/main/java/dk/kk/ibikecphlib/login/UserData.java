// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package dk.kk.ibikecphlib.login;

public class UserData {
    private String name;
    private String email;
    private String password;
    private String passwordConfirmed;
    private String newPassword;
    private String auth_token;
    private int id;
    private String base64Image = "";
    private String imageName = "";

    public UserData(String authToken) {
        this.auth_token = authToken;
        this.email = "";
        this.password = authToken;
        this.passwordConfirmed = "";
        this.base64Image = "";
        this.imageName = "";
    }

    public UserData(String email, String password, String newPassword) {
        this.email = email;
        this.password = password;
        this.newPassword = newPassword;
    }

    public UserData(String name, String email, String password,
                    String passwordConfirmed, String base64Image, String imageName) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.passwordConfirmed = passwordConfirmed;
        this.base64Image = base64Image;
        this.imageName = imageName;
    }

    public UserData(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public UserData(String auth_token, int id) {
        this.auth_token = auth_token;
        this.id = id;
    }

    public UserData(String auth_token, int id, String password) {
        this.auth_token = auth_token;
        this.id = id;
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getPasswordConfirmed() {
        return passwordConfirmed;
    }

    public void setPasswordConfirmed(String passwordConfirmed) {
        this.passwordConfirmed = passwordConfirmed;
    }

    public String getAuth_token() {
        return auth_token;
    }

    public void setAuth_token(String auth_token) {
        this.auth_token = auth_token;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBase64Image() {
        return base64Image;
    }

    public void setBase64Image(String base64Image) {
        this.base64Image = base64Image;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

}
