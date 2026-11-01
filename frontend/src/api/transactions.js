
import api from './axiosInstance';

export const getTransactions = async () => {
    const response = await api.get('/api/sms/imports');
    return response.data;
};

export const getTransactionById = async (id) => {
    const response = await api.get(`/api/sms/imports/${id}`);
    return response.data;
};

export const getSubscriptionSuggestions = async () => {
    const response = await api.get('/api/sms/subscription-suggestions');
    return response.data;
};
