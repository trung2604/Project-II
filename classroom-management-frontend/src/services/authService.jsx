import axiosInstance from '../utils/axiosConfig';

const authService = {
  register: async (formData) => {
    const response = await axiosInstance.post('/auth/register', formData);
    return response.data;
  },

  login: async (credentials) => {
    const response = await axiosInstance.post('/auth/login', credentials);
    const user = response.data;
    localStorage.setItem('currentUser', JSON.stringify(user));
    return user;
  },

  googleLogin: async (token) => {
    const response = await axiosInstance.post('/auth/google-login', { token });
    const user = response.data;
    localStorage.setItem('currentUser', JSON.stringify(user));
    return user;
  },

  verifyCode: async (email, code) => {
    const response = await axiosInstance.post('/auth/verify-code', { email, code });
    return response.data;
  },

  verifyEmail: async (token) => {
    const response = await axiosInstance.post('/auth/verify-email', { token });
    return response.data;
  },

  resendVerificationCode: async (email) => {
    const response = await axiosInstance.post('/auth/resend-code', { email });
    return response.data;
  },

  getCurrentUser: () => {
    const user = localStorage.getItem('currentUser');
    return user ? JSON.parse(user) : null;
  },

  logout: () => {
    localStorage.removeItem('currentUser');
  }
};

export default authService;
