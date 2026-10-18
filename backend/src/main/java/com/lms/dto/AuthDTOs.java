package com.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDTOs {

    public static class LoginRequest {
        private String username;
        private String password;

        public LoginRequest() {}
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public void setUsername(String username) { this.username = username; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class LoginResponse {
        private String token;
        private String role;
        private String username;

        public LoginResponse() {}
        public LoginResponse(String token, String role, String username) {
            this.token = token; this.role = role; this.username = username;
        }
        public String getToken() { return token; }
        public String getRole() { return role; }
        public String getUsername() { return username; }
        public void setToken(String token) { this.token = token; }
        public void setRole(String role) { this.role = role; }
        public void setUsername(String username) { this.username = username; }
    }

    public static class ChangePasswordRequest {
        @NotBlank private String oldPassword;
        @NotBlank @Size(min = 8, max = 72, message = "must be between 8 and 72 characters") private String newPassword;

        public ChangePasswordRequest() {}
        public String getOldPassword() { return oldPassword; }
        public String getNewPassword() { return newPassword; }
        public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
    
    public static class getPasswordRequest {
    	private String password;

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}
    }
    
    public static class getPasswordResponse {
    	private String bCryptPassword;

		public String getbCryptPassword() {
			return bCryptPassword;
		}

		public void setbCryptPassword(String bCryptPassword) {
			this.bCryptPassword = bCryptPassword;
		}
    }
}
